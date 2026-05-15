# ABHED

ABHED is an Android-based banking demo application that explores touch dynamics and behavior-based authentication workflows for secure digital banking. The project combines a modern Android frontend with a Flask-powered backend to simulate intelligent fraud detection and user verification mechanisms.

## Repository Structure

- `app/`: Android application (Kotlin, Jetpack Compose).
- `server/`: Flask API server and ML-related backend logic.

## Prerequisites

- Android Studio (latest stable) with Android SDK installed.
- JDK 17 or newer.
- Python 3.10+.
- `pip` for Python package installation.

## Backend Setup (Flask Server)

1. Open a terminal in the repository root.
2. Create and activate a virtual environment:
  - Linux/macOS:
    - `python3 -m venv .venv`
    - `source .venv/bin/activate`
3. Install dependencies:
  - `pip install -r server/requirements.txt`
4. Start the server:
  - `python server/app.py`

The server runs on `http://0.0.0.0:3343` by default.

## Android App Setup

1. Open the project root in Android Studio.
2. Let Gradle sync complete.
3. Update server host if needed in:
  - `app/src/main/java/com/example/bankingapp/network/ABHEDServerClient.kt`
4. Run the app on an emulator or physical device.

## Twilio (Optional)

Twilio credentials are not stored in source code. Configure them at runtime from the app's credentials screen if real SMS delivery is needed.

## Notes
- This project is a prototype intended for demo and experimentation.

### Authors/Team 
Built for the FinShield Hackathon by  
[Vibhaas N Srivastava](https://github.com/vibhaas) and [Laavanya Rajan](https://github.com/Laav10)
- Intellectual Property owned by Bank of India and IIT Hyderabad
