import numpy as np

# ---------- circular stats ----------
def circ_r(angles):
    if len(angles) == 0:
        return np.nan
    C = np.sum(np.cos(angles))
    S = np.sum(np.sin(angles))
    return np.sqrt(C * C + S * S) / len(angles)

def circ_mean(angles):
    if len(angles) == 0:
        return np.nan
    C = np.sum(np.cos(angles))
    S = np.sum(np.sin(angles))
    return np.arctan2(S, C)

def matlab_prctile_exact(a, q):
    """
    MATLAB-like prctile for 1D arrays (keeps +/-Inf, removes only NaNs).
    q can be scalar or list/tuple/np.ndarray of percentiles.
    """
    a = np.asarray(a)
    a = a[~np.isnan(a)]
    if a.size == 0:
        if np.isscalar(q):
            return np.nan
        else:
            return np.array([np.nan] * len(q))
    a_sorted = np.sort(a)  # numpy sorts +/-Inf to ends
    n = a_sorted.size

    scalar = np.isscalar(q)
    qs = [q] if scalar else list(q)
    out = []
    for p in qs:
        p = float(p)
        if p <= 0:
            out.append(a_sorted[0]); continue
        if p >= 100:
            out.append(a_sorted[-1]); continue
        alpha = p * (n + 1.0) / 100.0
        if alpha <= 1.0:
            out.append(a_sorted[0])
        elif alpha >= n:
            out.append(a_sorted[-1])
        else:
            k = int(np.floor(alpha))  # 1-based index in MATLAB
            d = alpha - k
            xk = a_sorted[k - 1]
            xk1 = a_sorted[k]
            out.append(xk + d * (xk1 - xk))
    return out[0] if scalar else np.array(out)

# ---------- extractor for single stroke ----------
def extract_from_stroke(raw_stroke,
                        dpi_x=400, dpi_y=400,
                        phone_orientation=0,
                        phone_id=1,
                        user_id=0,
                        doc_id=0):
    """
    raw_stroke: list of dicts with keys:
        time_ms, action, x, y, pressure, area, finger_orientation
      OR a numpy array with columns in order:
        phoneID,userID,docID,time_ms,action,phone_orientation,x,y,pressure,area,finger_orientation
        (this second form will be adapted but function expects stroke-level inputs)
    dpi_x/dpi_y: used only as fallback if phone_id not in known mapping
    phone_orientation, phone_id, user_id, doc_id: scalars to fill into each point (will be incremented by +1
        to match the original MATLAB pipeline behavior)
    Returns: numpy array of length 34 (float)
    """

    # make stroke_rows an Nx11 numpy array with columns matching the batch script:
    # [phoneID,userID,docID,time_s,action,phone_orientation,x,y,pressure,area,finger_orient]
    # We'll accept both dict-list and already-formed numpy rows.
    if isinstance(raw_stroke, np.ndarray):
        stroke_rows = np.array(raw_stroke, dtype=float)
        # assume raw array is already in suitable columns; time in ms expected -> convert below
    else:
        # raw_stroke is a list of dicts
        rows = []
        for p in raw_stroke:
            tms = float(p.get("time_ms", p.get("time", 0)))
            action = int(p.get("action", 2))
            x = float(p.get("x", 0.0))
            y = float(p.get("y", 0.0))
            press = float(p.get("pressure", p.get("press", 0.0)))
            area = float(p.get("area", 0.0))
            forient = float(p.get("finger_orientation", p.get("finger_orient", 0.0)))
            # fill phoneID/userID/docID with provided values (will +1 later to mimic MATLAB)
            rows.append([float(phone_id), float(user_id), float(doc_id),
                         float(tms), float(action), float(phone_orientation),
                         float(x), float(y), float(press), float(area), float(forient)])
        stroke_rows = np.array(rows, dtype=float)

    if stroke_rows.size == 0:
        return np.full(34, np.nan, dtype=float)

    # column indices (0-based) matching batch extractor
    col_phoneID = 0
    col_user = 1
    col_doc = 2
    col_time = 3
    col_act = 4
    col_orient = 5
    col_x = 6
    col_y = 7
    col_press = 8
    col_area = 9
    col_Forient = 10

    # work on a copy
    t = stroke_rows.copy().astype(float)

    # convert time ms -> seconds
    t[:, col_time] = t[:, col_time] / 1000.0

    # flip y to match MATLAB's coordinate flip
    t[:, col_y] = -t[:, col_y]

    # increment ids to mimic MATLAB preprocessing (they did +1 on entire file)
    t[:, col_phoneID] = t[:, col_phoneID] + 1
    t[:, col_user] = t[:, col_user] + 1
    t[:, col_doc] = t[:, col_doc] + 1

    # config constants (kept same as batch)
    indivPrctlVals = [20, 50, 80]
    pixTommFac = np.array([1/252., 1/233., 1/252., 1/233., 1/252.]) * 25.4

    # Now compute features (single-stroke)
    npoints = t.shape[0]
    feat = np.full(34, np.nan, dtype=float)

    # inter-stroke time: for single stroke we cannot know next stroke; keep NaN to match batch when not available
    # This will be set by the server when processing multiple strokes
    inter_stroke_time = np.nan

    # ids
    feat[0] = t[0, col_user]
    feat[1] = t[0, col_doc]
    feat[2] = inter_stroke_time  # Set inter-stroke time (will be NaN for single stroke)
    feat[12] = t[0, col_phoneID]

    # pixel->mm conversion using phone id if in range, else fallback to dpi_x
    phone_idx = int(feat[12]) - 1
    if 0 <= phone_idx < len(pixTommFac):
        fac = pixTommFac[phone_idx]
    else:
        fac = 25.4 / float(dpi_x)  # mm per px fallback

    stroke = t.copy()
    stroke[:, [col_x, col_y]] = stroke[:, [col_x, col_y]] * fac

    # stroke duration
    feat[3] = stroke[-1, col_time] - stroke[0, col_time]

    # start/stop coords
    feat[4] = stroke[0, col_x]
    feat[5] = stroke[0, col_y]
    feat[6] = stroke[-1, col_x]
    feat[7] = stroke[-1, col_y]

    # direct end-to-end distance
    feat[8] = np.sqrt((feat[7] - feat[5])**2 + (feat[6] - feat[4])**2)

    # pairwise displacements / tdelta
    if npoints >= 2:
        xdispl = np.diff(stroke[:, col_x], axis=0)
        ydispl = np.diff(stroke[:, col_y], axis=0)
        tdelta = np.diff(stroke[:, col_time], axis=0)  # keep zeros so infs may appear (MATLAB behavior)
        angl = np.arctan2(ydispl, xdispl)
        feat[9] = circ_r(angl) if angl.size > 0 else np.nan
        pairwDist = np.sqrt(xdispl**2 + ydispl**2)
        # velocities (may contain inf)
        with np.errstate(divide='ignore', invalid='ignore'):
            v = pairwDist / tdelta if pairwDist.size > 0 else np.array([])
        if v.size > 0:
            feat[13:16] = matlab_prctile_exact(v, indivPrctlVals)
        else:
            feat[13:16] = np.array([np.nan, np.nan, np.nan])
        # acceleration (MATLAB exact logic)
        if v.size > 1:
            a_filtered = np.concatenate([[v[0]], np.diff(v)])  # [v[0], v[1]-v[0], ...]
            with np.errstate(divide='ignore', invalid='ignore'):
                a_divided = a_filtered / tdelta  # divide by tdelta
            a = a_divided[1:]  # remove first element (MATLAB: a(1)=[])
        else:
            a = np.array([])
        if a.size > 0:
            feat[16:19] = matlab_prctile_exact(a, indivPrctlVals)
        else:
            feat[16:19] = np.array([np.nan, np.nan, np.nan])
        # median velocity last 3
        if v.size > 0:
            last3start = max(v.size - 3, 0)
            feat[19] = np.median(v[last3start:]) if v[last3start:].size > 0 else np.nan
        else:
            feat[19] = np.nan
    else:
        # not enough points for pairwise stats
        v = np.array([])
        pairwDist = np.array([])
        a = np.array([])

    # deviation from end-to-end line
    if npoints >= 1:
        xvek = stroke[:, col_x] - stroke[0, col_x]
        yvek = stroke[:, col_y] - stroke[0, col_y]
        x_end = xvek[-1]; y_end = yvek[-1]
        perVek2d = np.array([y_end, -x_end], dtype=float)
        norm = np.sqrt(perVek2d[0]**2 + perVek2d[1]**2)
        if norm == 0 or np.isnan(norm):
            perUnit = np.array([0.0, 0.0])
        else:
            perUnit = perVek2d / norm
        projectOnPerpStraight = xvek * perUnit[0] + yvek * perUnit[1]
        absProj = np.abs(projectOnPerpStraight)
        if absProj.size > 0:
            maxind = int(np.argmax(absProj))
            feat[20] = projectOnPerpStraight[maxind]
            feat[21:24] = matlab_prctile_exact(projectOnPerpStraight, indivPrctlVals)
        else:
            feat[20] = np.nan
            feat[21:24] = np.array([np.nan, np.nan, np.nan])
    else:
        feat[20] = np.nan
        feat[21:24] = np.array([np.nan, np.nan, np.nan])

    # circular mean of pairwise angles
    feat[24] = circ_mean(angl) if 'angl' in locals() and angl.size > 0 else np.nan

    # end-to-end angle
    feat[11] = np.arctan2(feat[7] - feat[5], feat[6] - feat[4]) if not np.isnan(feat[7]) else np.nan

    # direction flag mapping (MATLAB logic)
    if not np.isnan(feat[11]):
        tmpangle = feat[11] + np.pi
        tmpangle = float(tmpangle % (2 * np.pi))
        if tmpangle <= np.pi / 4:
            feat[10] = 4
        elif tmpangle > np.pi / 4 and tmpangle <= 5 * np.pi / 4:
            if tmpangle < 3 * np.pi / 4:
                feat[10] = 1
            else:
                feat[10] = 2
        else:
            if tmpangle < 7 * np.pi / 4:
                feat[10] = 3
            else:
                feat[10] = 4
    else:
        feat[10] = np.nan

    # trajectory length and ratio
    feat[25] = np.sum(pairwDist) if pairwDist.size > 0 else 0.0
    feat[26] = feat[8] / feat[25] if feat[25] != 0 else np.nan
    feat[27] = feat[25] / feat[3] if feat[3] != 0 else np.nan

    # median acceleration first 5
    if 'a' in locals() and a.size > 0:
        first_n = min(5, a.size)
        feat[28] = np.median(a[:first_n])
    else:
        feat[28] = np.nan

    # mid-stroke medians: follow MATLAB indexing floor(n/2):ceil(n/2)
    mid_start = max(0, int(np.floor(npoints / 2.0)) - 1)
    mid_end = min(npoints - 1, int(np.ceil(npoints / 2.0)) - 1)
    if mid_start <= mid_end and mid_start < npoints:
        feat[29] = np.median(stroke[mid_start:mid_end + 1, col_press]) if stroke[mid_start:mid_end + 1, col_press].size > 0 else np.nan
        feat[30] = np.median(stroke[mid_start:mid_end + 1, col_area]) if stroke[mid_start:mid_end + 1, col_area].size > 0 else np.nan
        feat[31] = np.median(stroke[mid_start:mid_end + 1, col_Forient]) if stroke[mid_start:mid_end + 1, col_Forient].size > 0 else np.nan
    else:
        feat[29] = np.nan
        feat[30] = np.nan
        feat[31] = np.nan

    # change finger orientation, phone orientation
    feat[32] = stroke[-1, col_Forient] - stroke[0, col_Forient] if npoints > 0 else np.nan
    feat[33] = stroke[0, col_orient] if npoints > 0 else np.nan

    return feat
