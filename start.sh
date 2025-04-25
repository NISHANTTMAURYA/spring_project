#!/bin/bash

# Default port
DEFAULT_PORT=8080

# Use PORT from environment variable if set, otherwise use default
if [ -z "$PORT" ]; then
  SPRING_PORT=$DEFAULT_PORT
  echo "PORT environment variable not set, using default: $DEFAULT_PORT"
else
  SPRING_PORT=$PORT
  echo "Using PORT environment variable: $PORT"
fi

# Print all environment variables for debugging
echo "Environment variables:"
echo "====================="
printenv | sort
echo "====================="

# Start the Spring Boot application with explicit port
echo "Starting Spring Boot application on port $SPRING_PORT"
java -Dspring.profiles.active=prod -Dserver.port=$SPRING_PORT -jar app.jar 