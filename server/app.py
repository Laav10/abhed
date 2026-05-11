#!/usr/bin/env python3
# app.py - Flask server for ABHED demo (single-file)

import os
import sqlite3
import json
import uuid
import datetime
import threading
from collections import deque
from concurrent.futures import ThreadPoolExecutor

from flask import Flask, request, jsonify, g
from flask_cors import CORS
import numpy as np
from joblib import dump, load
from sklearn.svm import OneClassSVM
from sklearn.preprocessing import StandardScaler

# ---------- CONFIG ----------
DATABASE = "abhed_demo.db"
MODELS_DIR = "models"
os.makedirs(MODELS_DIR, exist_ok=True)

# sliding window & training thresholds
SLIDING_WINDOW = 500
MIN_TRAIN_STROKES = 100
RETRAIN_ON_NEW = 50
MAX_TRAIN_FETCH = 2000

# thread pool for background training
executor = ThreadPoolExecutor(max_workers=2)

app = Flask(__name__)
CORS(app)

# ---------- in-memory state ----------
sessions = {}                       # token -> user_id
user_buffers = {}                   # user_id -> deque of feature vectors
user_new_since_train = {}           # user_id -> int
user_train_lock = {}                # user_id -> threading.Lock()
user_last_stroke_start = {}         # user_id -> last stroke START time in ms

# ---------- DB helpers ----------
def get_db():
    db = getattr(g, "_database", None)
    if db is None:
        db = sqlite3.connect(DATABASE, check_same_thread=False)
        db.row_factory = sqlite3.Row
        g._database = db
    return db

def init_db():
    db = sqlite3.connect(DATABASE)
    cur = db.cursor()
    cur.execute("""
    CREATE TABLE IF NOT EXISTS users (
        user_id TEXT PRIMARY KEY,
        debit_card TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    """)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS strokes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        stroke_time REAL,
        features TEXT,
        raw_json TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(user_id) REFERENCES users(user_id)
    );
    """)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS models (
        user_id TEXT PRIMARY KEY,
        model_path TEXT,
        scaler_path TEXT,
        last_trained TIMESTAMP
    );
    """)
    db.commit()
    db.close()

@app.teardown_appcontext
def close_connection(exception):
    db = getattr(g, "_database", None)
    if db is not None:
        db.close()

# ---------- ML Prediction Functions ----------
def get_ml_prediction_for_user(user_id, features):
    """
    Get ML prediction for a user based on their stroke features
    Returns confidence score and prediction label
    """
    try:
        # Check if user has a trained model
        db = get_db()
        cur = db.cursor()
        cur.execute("SELECT model_path, scaler_path FROM models WHERE user_id = ?", (user_id,))
        model_row = cur.fetchone()
        
        if not model_row:
            # No model trained yet, return default prediction
            return {
                "confidence": 75.0,  # Default confidence for new users
                "prediction": "genuine_user",
                "model_version": "default_v1.0",
                "model_status": "no_model_trained"
            }
        
        model_path = model_row['model_path']
        scaler_path = model_row['scaler_path']
        
        # Load model and scaler
        if os.path.exists(model_path) and os.path.exists(scaler_path):
            model = load(model_path)
            scaler = load(scaler_path)
            
            # Scale features
            features_scaled = scaler.transform(features.reshape(1, -1))
            
            # Get prediction and confidence
            prediction = model.predict(features_scaled)[0]
            confidence = model.score_samples(features_scaled)[0]
            
            # Convert confidence to percentage (0-100)
            confidence_percent = max(0.0, min(100.0, (confidence + 5) * 10))
            
            # Map prediction to human-readable label
            prediction_label = "genuine_user" if prediction == 1 else "suspicious_activity"
            
            return {
                "confidence": round(confidence_percent, 2),
                "prediction": prediction_label,
                "model_version": f"trained_v{datetime.datetime.now().strftime('%Y%m%d')}",
                "model_status": "active_model"
            }
        else:
            # Model files missing
            return {
                "confidence": 50.0,
                "prediction": "model_unavailable",
                "model_version": "missing_files",
                "model_status": "model_files_missing"
            }
            
    except Exception as e:
        app.logger.exception("ML prediction failed for user %s: %s", user_id, e)
        return {
            "confidence": 0.0,
            "prediction": "prediction_error",
            "model_version": "error",
            "model_status": f"error: {str(e)}"
        }

# ---------- feature extractor integration ----------
# The server expects feature_extractor.py with function:
#   extract_from_stroke(raw_stroke_list, dpi_x, dpi_y, phone_orientation, phone_id) -> 1D array length 34
try:
    from feature_extractor import extract_from_stroke
except Exception:
    # fallback: trivial extractor for quick debug (not used in production)
    def extract_from_stroke(raw_stroke, dpi_x=400, dpi_y=400, phone_orientation=0, phone_id=1):
        vec = np.zeros(34, dtype=float)
        if isinstance(raw_stroke, list) and len(raw_stroke) > 0:
            vec[3] = (raw_stroke[-1].get("time_ms", 0) - raw_stroke[0].get("time_ms", 0)) / 1000.0
            vec[4] = raw_stroke[0].get("x", 0)
            vec[5] = raw_stroke[0].get("y", 0)
            vec[6] = raw_stroke[-1].get("x", 0)
            vec[7] = raw_stroke[-1].get("y", 0)
            vec[8] = np.sqrt((vec[6]-vec[4])**2 + (vec[7]-vec[5])**2)
            vec[33] = phone_orientation
        return vec

# ---------- confidence normalization ----------
def normalize_confidence_sigmoid(score, center=0.0, steepness=8):
    """
    Normalize confidence scores to [0,1] range using sigmoid function.
    
    This function maps raw One-Class SVM decision function scores to a more
    interpretable confidence scale where:
    - 0.0 = Very low confidence (likely impostor)
    - 0.5 = Neutral confidence
    - 1.0 = Very high confidence (likely legitimate user)
    
    Parameters:
    -----------
    score : float
        Raw decision function score from One-Class SVM
        Typical range: [-1.0, 1.0] but can vary based on data
        
    center : float, default=0.0
        The score value that maps to 0.5 confidence
        Based on actual One-Class SVM decision function scores:
        - Legitimate users: ~0.0 to 0.1
        - Impostors: ~-0.1 to 0.0
        - Center at 0.0 provides good separation
        
    steepness : float, default=8
        Controls how sharp the transition is around the center
        - Higher values = sharper transition (more binary-like)
        - Lower values = smoother transition (more gradual)
        - Range: 5-25 recommended
        - Reduced to 8 for better sensitivity with actual score ranges
        
    Returns:
    --------
    float
        Normalized confidence score in range [0.0, 1.0]
        
    Examples:
    ---------
    >>> normalize_confidence_sigmoid(-0.1)   # Typical impostor score
    0.31  # Low confidence
    
    >>> normalize_confidence_sigmoid(0.1)    # Typical legitimate score
    0.69  # High confidence
    
    >>> normalize_confidence_sigmoid(0.0)    # Center point
    0.50  # Neutral confidence
    
    Tuning Guidelines:
    ------------------
    1. ADJUST CENTER:
       - If legitimate users score too low: increase center (e.g., 0.1)
       - If impostors score too high: decrease center (e.g., -0.1)
       - Monitor: legitimate users should score >0.6, impostors <0.4
    
    2. ADJUST STEEPNESS:
       - For more binary behavior: increase steepness (e.g., 20)
       - For smoother transitions: decrease steepness (e.g., 10)
       - Higher steepness = more sensitive to small changes
    
    3. VALIDATION:
       - Test with known legitimate/impostor data
       - Aim for separation >0.5 between legitimate and impostor
       - Adjust parameters based on your specific data distribution
    """
    import numpy as np
    
    # Apply sigmoid transformation
    # Formula: 1 / (1 + exp(-steepness * (score - center)))
    # This creates an S-shaped curve centered at 'center'
    normalized = 1.0 / (1.0 + np.exp(-steepness * (score - center)))
    
    # Ensure output is in [0, 1] range (should be automatic with sigmoid)
    return max(0.0, min(1.0, normalized))

# ---------- model helpers ----------
def model_paths_for_user(user_id):
    model_path = os.path.join(MODELS_DIR, f"{user_id}_model.joblib")
    scaler_path = os.path.join(MODELS_DIR, f"{user_id}_scaler.joblib")
    return model_path, scaler_path

def save_model_for_user(user_id, model, scaler, db_connection=None):
    model_path, scaler_path = model_paths_for_user(user_id)
    dump(model, model_path)
    dump(scaler, scaler_path)
    if db_connection is None:
        db = get_db()
    else:
        db = db_connection
    cur = db.cursor()
    cur.execute("INSERT OR REPLACE INTO models(user_id, model_path, scaler_path, last_trained) VALUES (?, ?, ?, ?)",
                (user_id, model_path, scaler_path, datetime.datetime.utcnow()))
    db.commit()

def load_model_for_user(user_id):
    model_path, scaler_path = model_paths_for_user(user_id)
    if os.path.exists(model_path) and os.path.exists(scaler_path):
        try:
            model = load(model_path)
            scaler = load(scaler_path)
            return model, scaler
        except Exception:
            app.logger.exception("Failed to load model/scaler for %s", user_id)
    return None, None

def get_user_strokes_from_db(user_id, limit=MAX_TRAIN_FETCH, db_connection=None):
    if db_connection is None:
        db = get_db()
    else:
        db = db_connection
    cur = db.cursor()
    cur.execute("SELECT features FROM strokes WHERE user_id = ? ORDER BY id DESC LIMIT ?", (user_id, limit))
    rows = cur.fetchall()
    feats = []
    for r in rows:
        try:
            v = json.loads(r["features"])
            feats.append(v)
        except Exception:
            continue
    feats.reverse()  # oldest-first
    if db_connection is None:
        return np.array(feats, dtype=float) if len(feats) > 0 else np.empty((0,34))
    else:
        return np.array(feats, dtype=float) if len(feats) > 0 else np.empty((0,34))

# ---------- training ----------
def train_ocsvm_for_user(user_id):
    lock = user_train_lock.setdefault(user_id, threading.Lock())
    if not lock.acquire(blocking=False):
        app.logger.info("Training already running for %s", user_id)
        return
    try:
        app.logger.info("Training for %s started", user_id)
        # Create our own database connection for background training
        db = sqlite3.connect(DATABASE)
        db.row_factory = sqlite3.Row
        X = get_user_strokes_from_db(user_id, db_connection=db)
        
        if X.shape[0] < MIN_TRAIN_STROKES:
            app.logger.info("Not enough samples to train for %s (%d)", user_id, X.shape[0])
            return
        
        # Handle NaN values by replacing them with 0 or median
        from sklearn.impute import SimpleImputer
        imputer = SimpleImputer(strategy='median')
        X_imputed = imputer.fit_transform(X)
        
        scaler = StandardScaler().fit(X_imputed)
        Xs = scaler.transform(X_imputed)
        model = OneClassSVM(kernel="rbf", gamma="scale", nu=0.05)
        model.fit(Xs)
        save_model_for_user(user_id, model, scaler, db_connection=db)
        user_new_since_train[user_id] = 0
        app.logger.info("Training for %s finished (n=%d)", user_id, X.shape[0])
    except Exception:
        app.logger.exception("Training failed for %s", user_id)
    finally:
        if 'db' in locals():
            db.close()
        lock.release()

# ---------- auth helper ----------
def require_token(func):
    def wrapper(*args, **kwargs):
        auth = request.headers.get("Authorization", "")
        token = None
        if auth.startswith("Bearer "):
            token = auth.split(" ", 1)[1]
        else:
            token = request.args.get("token") or (request.get_json(silent=True) or {}).get("token")
        if not token or token not in sessions:
            return jsonify({"error": "missing or invalid token"}), 401
        request.user_id = sessions[token]
        return func(*args, **kwargs)
    wrapper.__name__ = func.__name__
    return wrapper

# ---------- endpoints ----------
@app.route("/userlogin", methods=["POST"])
def userlogin():
    data = request.get_json(force=True)
    if not data or "user_id" not in data:
        return jsonify({"error": "user_id required"}), 400
    user_id = str(data["user_id"])
    debit_card = data.get("debit_card")
    db = get_db()
    cur = db.cursor()
    cur.execute("INSERT OR IGNORE INTO users(user_id, debit_card) VALUES (?, ?)", (user_id, debit_card))
    if debit_card:
        cur.execute("UPDATE users SET debit_card = ? WHERE user_id = ?", (debit_card, user_id))
    db.commit()
    token = str(uuid.uuid4())
    sessions[token] = user_id
    user_buffers.setdefault(user_id, deque(maxlen=SLIDING_WINDOW))
    user_new_since_train.setdefault(user_id, 0)
    user_train_lock.setdefault(user_id, threading.Lock())
    # preload last stroke start time from DB if available
    try:
        cur.execute("SELECT stroke_time FROM strokes WHERE user_id = ? ORDER BY id DESC LIMIT 1", (user_id,))
        row = cur.fetchone()
        if row and row["stroke_time"] is not None:
            user_last_stroke_start[user_id] = float(row["stroke_time"])
    except Exception:
        app.logger.exception("Failed to preload last stroke start for user %s", user_id)
    return jsonify({"status": "ok", "token": token})

@app.route("/userlogout", methods=["POST"])
def userlogout():
    data = request.get_json(silent=True) or {}
    token = None
    auth = request.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        token = auth.split(" ", 1)[1]
    else:
        token = data.get("token")
    if token and token in sessions:
        sessions.pop(token, None)
    return jsonify({"status": "ok"})

@app.route("/raw_stroke", methods=["POST"])
@require_token
def raw_stroke():
    data = request.get_json(force=True)
    user_id = request.user_id

    stroke = data.get("stroke")
    if not stroke or not isinstance(stroke, list):
        return jsonify({"error": "stroke must be list of points"}), 400

    dpi_x = data.get("dpi_x", 400)
    dpi_y = data.get("dpi_y", 400)
    phone_orientation = data.get("phone_orientation", 0)
    phone_id = data.get("phone_id", 1)

    # normalize point keys
    def normalize_point(p):
        mapped = {}
        mapped['time_ms'] = p.get('time_ms') if 'time_ms' in p else p.get('time') if 'time' in p else None
        mapped['action'] = p.get('action')
        mapped['x'] = p.get('x') if 'x' in p else p.get('x_px') if 'x_px' in p else None
        mapped['y'] = p.get('y') if 'y' in p else p.get('y_px') if 'y_px' in p else None
        mapped['pressure'] = p.get('pressure') if 'pressure' in p else p.get('press') if 'press' in p else 0.0
        mapped['area'] = p.get('area') if 'area' in p else p.get('size') if 'size' in p else 0.0
        if 'finger_orientation' in p:
            mapped['finger_orientation'] = p.get('finger_orientation')
        elif 'finger_orient' in p:
            mapped['finger_orientation'] = p.get('finger_orient')
        else:
            mapped['finger_orientation'] = p.get('orientation', p.get('orient', 0.0))
        return mapped

    try:
        stroke_norm = [normalize_point(pt) for pt in stroke]
    except Exception as e:
        app.logger.exception("Stroke normalization failed: %s", e)
        return jsonify({"error": "stroke normalization failed", "detail": str(e)}), 400

    # validation
    if len(stroke_norm) < 2:
        return jsonify({"error": "stroke must contain at least 2 points"}), 400
    if stroke_norm[0].get('action') != 0:
        return jsonify({"error": "stroke must start with action == 0 (ACTION_DOWN)"}), 400
    if stroke_norm[-1].get('action') != 1:
        return jsonify({"error": "stroke must end with action == 1 (ACTION_UP)"}), 400

    # compute inter-stroke (start-to-start) using last start from memory
    try:
        curr_start = float(stroke_norm[0]['time_ms'])
    except Exception as e:
        return jsonify({"error": "invalid time_ms in stroke points", "detail": str(e)}), 400

    last_start = user_last_stroke_start.get(user_id)
    inter_stroke_time = (curr_start - last_start) / 1000.0 if last_start is not None else float("nan")
    # update last start for next stroke
    user_last_stroke_start[user_id] = curr_start

    # call extractor
    try:
        feat = extract_from_stroke(stroke_norm, dpi_x=dpi_x, dpi_y=dpi_y,
                                   phone_orientation=phone_orientation, phone_id=phone_id)
    except Exception as e:
        app.logger.exception("Feature extraction failed: %s", e)
        return jsonify({"error": "feature extraction failed", "detail": str(e)}), 500

    feat = np.asarray(feat, dtype=float).flatten()
    if feat.size != 34:
        return jsonify({"error": f"extractor must return 34 features, got {feat.size}"}), 500

    # set inter-stroke time (index 2) and ensure user_id/phone_id fields
    feat[2] = float(inter_stroke_time)
    try:
        feat[0] = float(user_id) if str(user_id).isdigit() else feat[0]
    except Exception:
        pass
    feat[12] = float(phone_id)

    # append to in-memory buffer
    buf = user_buffers.setdefault(user_id, deque(maxlen=SLIDING_WINDOW))
    buf.append(feat.tolist())
    user_new_since_train[user_id] = user_new_since_train.get(user_id, 0) + 1

    # persist to DB
    db = get_db()
    cur = db.cursor()
    try:
        cur.execute("INSERT INTO strokes(user_id, stroke_time, features, raw_json) VALUES (?, ?, ?, ?)",
                    (user_id, curr_start, json.dumps(feat.tolist()), json.dumps(stroke_norm)))
        db.commit()
    except Exception as e:
        app.logger.exception("DB write failed: %s", e)
        return jsonify({"error": "db write failed", "detail": str(e)}), 500

    # trigger retrain in background if needed
    if user_new_since_train[user_id] >= RETRAIN_ON_NEW:
        user_new_since_train[user_id] = 0
        executor.submit(train_ocsvm_for_user, user_id)

    # Get ML prediction for this stroke
    try:
        prediction_result = get_ml_prediction_for_user(user_id, feat)
        return jsonify({
            "status": "ok", 
            "stored": True, 
            "inter_stroke_time": None if np.isnan(feat[2]) else float(feat[2]),
            "ml_prediction": prediction_result
        })
    except Exception as e:
        app.logger.exception("ML prediction failed: %s", e)
        return jsonify({
            "status": "ok", 
            "stored": True, 
            "inter_stroke_time": None if np.isnan(feat[2]) else float(feat[2]),
            "ml_prediction": {
                "confidence": 0.0,
                "prediction": "prediction_failed",
                "model_version": "unknown",
                "error": str(e)
            }
        })

@app.route("/confidence", methods=["GET"])
@require_token
def confidence():
    user_id = request.user_id
    n_recent = int(request.args.get("n_recent", 10))
    buf = user_buffers.get(user_id, deque())

    if len(buf) < n_recent:
        # try to repopulate buffer from DB and set last_start
        features = get_user_strokes_from_db(user_id, limit=SLIDING_WINDOW)
        if features.shape[0] > 0:
            user_buffers[user_id] = deque(features.tolist(), maxlen=SLIDING_WINDOW)
            buf = user_buffers[user_id]
            try:
                db = get_db()
                cur = db.cursor()
                cur.execute("SELECT stroke_time FROM strokes WHERE user_id = ? ORDER BY id DESC LIMIT 1", (user_id,))
                row = cur.fetchone()
                if row and row["stroke_time"] is not None:
                    user_last_stroke_start[user_id] = float(row["stroke_time"])
            except Exception:
                app.logger.exception("Failed to set last_stroke_start from DB for %s", user_id)

    if len(buf) < MIN_TRAIN_STROKES:
        return jsonify({"status": "collecting_data", "have": len(buf), "need": MIN_TRAIN_STROKES})

    model, scaler = load_model_for_user(user_id)
    if model is None or scaler is None:
        return jsonify({"status": "collecting_data", "reason": "no_model", "have": len(buf)})

    recent = np.array(list(buf)[-n_recent:], dtype=float)
    try:
        # Handle NaN values in recent strokes (same as training)
        from sklearn.impute import SimpleImputer
        imputer = SimpleImputer(strategy='median')
        recent_imputed = imputer.fit_transform(recent)
        Xs = scaler.transform(recent_imputed)
    except Exception:
        app.logger.exception("Scaler transform failed for user %s", user_id)
        return jsonify({"status": "error", "detail": "scaler transform failed"}), 500

    df = model.decision_function(Xs)  # higher = more normal
    mean_df = float(np.mean(df))
    
    # Convert decision function to normalized confidence score (0-1 range)
    # One-Class SVM decision function can be negative (anomaly) or positive (normal)
    # We use sigmoid-based normalization for better interpretability
    confidence_score = normalize_confidence_sigmoid(mean_df)
    
    return jsonify({"status": "ok", "confidence": float(confidence_score), "raw_score": mean_df})

@app.route("/debug/list_users", methods=["GET"])
def debug_list_users():
    db = get_db()
    cur = db.cursor()
    cur.execute("SELECT user_id, debit_card FROM users")
    rows = cur.fetchall()
    return jsonify([dict(r) for r in rows])

@app.route("/debug/last_features", methods=["GET"])
def debug_last_features():
    user_id = request.args.get("user_id")
    if not user_id:
        return jsonify({"error": "user_id param required"}), 400
    buf = user_buffers.get(user_id, deque())
    return jsonify({"in_memory_count": len(buf), "last": list(buf)[-5:] if len(buf) > 0 else []})

@app.route("/hello", methods=["GET"])
def hello():
    return "<h3>hello</h3>"

# ---------- NEW ML MODEL ENDPOINTS FOR ANDROID APP ----------

@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint for Android app"""
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.datetime.now().isoformat(),
        "version": "1.0.0"
    })

@app.route("/predict_stroke", methods=["POST"])
def predict_stroke():
    """Single stroke prediction endpoint for real-time Android app usage"""
    try:
        data = request.get_json(force=True)
        
        # Extract stroke data from Android app format
        stroke_data = data.get("stroke_data", {})
        model_type = data.get("model_type", "touch_behavior")
        
        # Map Android app data to existing stroke format
        user_id = stroke_data.get("user_id", "android_user")
        stroke_id = stroke_data.get("stroke_id", "unknown")
        
        # Convert Android stroke format to existing format
        android_stroke = []
        
        # Handle single point (tap) or multi-point (swipe) strokes
        if stroke_data.get("start_x") == stroke_data.get("end_x") and stroke_data.get("start_y") == stroke_data.get("end_y"):
            # Single point - create ACTION_DOWN and ACTION_UP
            android_stroke = [
                {
                    "action": 0,  # ACTION_DOWN
                    "time_ms": stroke_data.get("start_time", 0),
                    "x": stroke_data.get("start_x", 0),
                    "y": stroke_data.get("start_y", 0),
                    "pressure": stroke_data.get("pressure", 0.5),
                    "area": 0.0,
                    "orientation": 0.0
                },
                {
                    "action": 1,  # ACTION_UP
                    "time_ms": stroke_data.get("end_time", stroke_data.get("start_time", 0) + 100),
                    "x": stroke_data.get("end_x", stroke_data.get("start_x", 0)),
                    "y": stroke_data.get("end_y", stroke_data.get("start_y", 0)),
                    "pressure": stroke_data.get("pressure", 0.5),
                    "area": 0.0,
                    "orientation": 0.0
                }
            ]
        else:
            # Multi-point stroke - create intermediate points
            start_time = stroke_data.get("start_time", 0)
            end_time = stroke_data.get("end_time", start_time + 100)
            duration = end_time - start_time
            
            android_stroke = [
                {
                    "action": 0,  # ACTION_DOWN
                    "time_ms": start_time,
                    "x": stroke_data.get("start_x", 0),
                    "y": stroke_data.get("start_y", 0),
                    "pressure": stroke_data.get("pressure", 0.5),
                    "area": 0.0,
                    "orientation": 0.0
                }
            ]
            
            # Add intermediate point if duration > 50ms
            if duration > 50:
                mid_time = start_time + (duration // 2)
                android_stroke.append({
                    "action": 2,  # ACTION_MOVE
                    "time_ms": mid_time,
                    "x": (stroke_data.get("start_x", 0) + stroke_data.get("end_x", 0)) / 2,
                    "y": (stroke_data.get("start_y", 0) + stroke_data.get("end_y", 0)) / 2,
                    "pressure": stroke_data.get("pressure", 0.5),
                    "area": 0.0,
                    "orientation": 0.0
                })
            
            android_stroke.append({
                "action": 1,  # ACTION_UP
                "time_ms": end_time,
                "x": stroke_data.get("end_x", 0),
                "y": stroke_data.get("end_y", 0),
                "pressure": stroke_data.get("pressure", 0.5),
                "area": 0.0,
                "orientation": 0.0
            })
        
        # Use existing stroke processing logic
        dpi_x = 400  # Default DPI
        dpi_y = 400
        phone_orientation = 0
        phone_id = 1
        
        # Extract features using existing logic
        try:
            feat = extract_from_stroke(android_stroke, dpi_x=dpi_x, dpi_y=dpi_y,
                                       phone_orientation=phone_orientation, phone_id=phone_id)
        except Exception as e:
            app.logger.exception("Feature extraction failed for Android stroke: %s", e)
            return jsonify({
                "error": "feature_extraction_failed",
                "detail": str(e)
            }), 500
        
        feat = np.asarray(feat, dtype=float).flatten()
        if feat.size != 34:
            return jsonify({
                "error": f"extractor returned {feat.size} features, expected 34"
            }), 500
        
        # Get confidence score using existing logic
        # For Android app, we'll use a simple confidence calculation
        # In production, you'd want to use the trained model
        
        # Simple confidence based on stroke characteristics
        pressure = stroke_data.get("pressure", 0.5)
        duration = stroke_data.get("duration", 0)
        velocity = stroke_data.get("velocity", 0)
        
        # Basic confidence calculation (replace with actual ML model)
        base_confidence = 75.0  # Base confidence for Android app
        pressure_factor = min(pressure * 20, 10)  # Pressure contribution
        duration_factor = min(max(duration / 1000.0 * 10, -5), 5)  # Duration contribution
        velocity_factor = min(max(velocity / 1000.0 * 5, -5), 5)  # Velocity contribution
        
        confidence = base_confidence + pressure_factor + duration_factor + velocity_factor
        confidence = max(0.0, min(100.0, confidence))  # Clamp to 0-100
        
        # Return prediction in Android app format
        return jsonify({
            "confidence": round(confidence, 1),
            "prediction": "genuine_user" if confidence > 60 else "suspicious_behavior",
            "model_version": "v1.0_android",
            "processing_time": 25,  # Simulated processing time
            "stroke_id": stroke_id,
            "features_extracted": len(feat),
            "stroke_type": "tap" if len(android_stroke) == 2 else "swipe"
        })
        
    except Exception as e:
        app.logger.exception("Android stroke prediction failed: %s", e)
        return jsonify({
            "error": "prediction_failed",
            "detail": str(e)
        }), 500

@app.route("/predict", methods=["POST"])
def predict_batch():
    """Batch prediction endpoint for multiple strokes"""
    try:
        data = request.get_json(force=True)
        strokes = data.get("strokes", [])
        batch_size = len(strokes)
        
        if batch_size == 0:
            return jsonify({"error": "no_strokes_provided"}), 400
        
        predictions = []
        
        for stroke_data in strokes:
            # Process each stroke individually
            stroke_id = stroke_data.get("stroke_id", "unknown")
            
            # Simple confidence calculation for batch processing
            pressure = stroke_data.get("pressure", 0.5)
            duration = stroke_data.get("duration", 0)
            
            confidence = 70.0 + (pressure * 20) + min(duration / 1000.0 * 10, 10)
            confidence = max(0.0, min(100.0, confidence))
            
            predictions.append({
                "stroke_id": stroke_id,
                "confidence": round(confidence, 1),
                "prediction": "genuine_user" if confidence > 60 else "suspicious_behavior",
                "model_version": "v1.0_android_batch"
            })
        
        return jsonify({
            "predictions": predictions,
            "batch_size": batch_size,
            "processed": len(predictions)
        })
        
    except Exception as e:
        app.logger.exception("Batch prediction failed: %s", e)
        return jsonify({
            "error": "batch_prediction_failed",
            "detail": str(e)
        }), 500

@app.route("/model_status", methods=["GET"])
def model_status():
    """Model status and health information"""
    try:
        # Count total users and models
        db = get_db()
        cur = db.cursor()
        
        cur.execute("SELECT COUNT(*) as user_count FROM users")
        user_count = cur.fetchone()["user_count"]
        
        cur.execute("SELECT COUNT(*) as model_count FROM models")
        model_count = cur.fetchone()["model_count"]
        
        cur.execute("SELECT COUNT(*) as stroke_count FROM strokes")
        stroke_count = cur.fetchone()["stroke_count"]
        
        # Get latest model training info
        cur.execute("SELECT MAX(last_trained) as last_training FROM models")
        last_training_row = cur.fetchone()
        last_training = last_training_row["last_training"] if last_training_row["last_training"] else None
        
        return jsonify({
            "is_healthy": True,
            "model_version": "v1.0_android",
            "last_training": last_training,
            "accuracy": 85.5,  # Placeholder accuracy
            "total_predictions": stroke_count,
            "total_users": user_count,
            "trained_models": model_count,
            "server_status": "running",
            "timestamp": datetime.datetime.now().isoformat()
        })
        
    except Exception as e:
        app.logger.exception("Model status check failed: %s", e)
        return jsonify({
            "is_healthy": False,
            "error": str(e),
            "timestamp": datetime.datetime.now().isoformat()
        }), 500

# ---------- bootstrap ----------
if __name__ == "__main__":
    init_db()
    print("Starting server on http://0.0.0.0:3343")
    app.run(debug=True, host="0.0.0.0", port=3343)
