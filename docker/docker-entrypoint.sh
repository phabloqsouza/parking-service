#!/bin/bash
set -e

echo "Starting Parking Service..."

# Start Spring Boot application in the background
java -jar /app/app.jar &

APP_PID=$!
echo "Application started with PID: $APP_PID"

# Wait for the application to be healthy
echo "Waiting for application to be ready..."
MAX_WAIT=60
WAIT_COUNT=0
HEALTH_CHECK_URL="http://localhost:3003/actuator/health"

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if curl -sf $HEALTH_CHECK_URL > /dev/null 2>&1; then
        echo "Application is ready!"
        break
    fi
    echo "Waiting for application to start... (${WAIT_COUNT}/${MAX_WAIT}s)"
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    echo "ERROR: Application did not become healthy within ${MAX_WAIT} seconds"
    exit 1
fi

# Call initialization endpoint with retry logic
MAX_ATTEMPTS=5
ATTEMPT=1
SLEEP_SECONDS=2
INIT_URL="http://localhost:3003/internal/initialize"

echo "Initializing garage from simulator..."

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    echo "Attempt ${ATTEMPT}/${MAX_ATTEMPTS} to initialize garage..."
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $INIT_URL)
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "Garage initialized successfully."
        break
    else
        echo "Initialization failed with HTTP $HTTP_CODE. Retrying in $SLEEP_SECONDS seconds..."
        sleep $SLEEP_SECONDS
        SLEEP_SECONDS=$((SLEEP_SECONDS * 2))
    fi
    
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo "WARNING: Failed to initialize garage after $MAX_ATTEMPTS attempts. Continuing anyway..."
    echo "You can manually trigger initialization via POST /internal/initialize"
fi

# Keep the application running in the foreground
echo "Parking Service is ready and running..."
wait $APP_PID
