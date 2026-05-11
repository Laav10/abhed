#!/bin/bash

# ABHED Server Startup Script

echo "Starting ABHED Authentication Server..."

# Activate virtual environment
source ../venv/bin/activate

# Check if database exists, if not initialize it
if [ ! -f "abhed_demo.db" ]; then
    echo "Initializing database..."
    python -c "from app import init_db; init_db()"
fi

# Start the server
echo "Starting Flask server on http://0.0.0.0:3343"
python app.py 