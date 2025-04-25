#!/bin/bash

# URL to ping
URL="https://todo-app-with-gemini.onrender.com"

# Log file
LOG_FILE="/app/ping-logs.txt"

echo "Keep-alive service started at $(date)" > $LOG_FILE

# Function to ping the URL
ping_server() {
  timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  echo "[$timestamp] Pinging $URL" >> $LOG_FILE
  
  # Get HTTP status code
  status=$(curl -s -o /dev/null -w "%{http_code}" $URL)
  
  # Log the status
  echo "[$timestamp] Status: $status" >> $LOG_FILE
}

# Start ping process in the background
while true; do
  ping_server
  sleep 300  # Sleep for 5 minutes (300 seconds)
done &

# Store the PID of the background process
echo $! > /app/keep-alive.pid

# Execute the main command
exec "$@" 