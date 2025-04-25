#!/bin/bash

# Check if the URL is in the format postgresql://username:password@hostname/database
if [[ $JDBC_DATABASE_URL == postgresql://* ]]; then
  # Extract parts from the URL
  AUTH_HOST_DB=${JDBC_DATABASE_URL#postgresql://}
  
  # Split into auth and host_db parts
  AUTH=${AUTH_HOST_DB%@*}
  HOST_DB=${AUTH_HOST_DB#*@}
  
  # Split host_db into host and db
  HOST=${HOST_DB%/*}
  DB=${HOST_DB#*/}
  
  # Construct proper JDBC URL with default port 5432
  export JDBC_DATABASE_URL="jdbc:postgresql://${HOST}:5432/${DB}"
  echo "Fixed JDBC_DATABASE_URL to: jdbc:postgresql://${HOST}:5432/${DB}"
  
  # Also properly set username and password environment variables if they're being used
  if [[ -n "$AUTH" && "$AUTH" == *":"* ]]; then
    export DB_USERNAME=${AUTH%:*}
    export DB_PASSWORD=${AUTH#*:}
    echo "Set DB_USERNAME and DB_PASSWORD from URL"
  fi
fi

# Pass control to the CMD specified in the Dockerfile
exec "$@" 