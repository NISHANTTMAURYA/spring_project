#!/bin/bash

# Extract connection details from JDBC_DATABASE_URL
if [[ $JDBC_DATABASE_URL == jdbc:postgresql* ]]; then
  # Remove the jdbc: prefix
  DB_URL=${JDBC_DATABASE_URL#jdbc:}
  
  echo "Initializing database schema..."
  
  # Create a simple SQL script to create the schema
  cat > /tmp/init-schema.sql << EOF
CREATE SCHEMA IF NOT EXISTS todoapp;
EOF
  
  # Use PGPASSWORD environment variable to avoid password prompt
  export PGPASSWORD=$DB_PASSWORD
  
  # Execute the SQL script using psql
  # Parse the connection URL to get host, port, database
  if [[ $DB_URL =~ postgresql://([^:]+):([^@]+)@([^:]+):([^/]+)/(.+) ]]; then
    DB_HOST=${BASH_REMATCH[3]}
    DB_PORT=${BASH_REMATCH[4]}
    DB_NAME=${BASH_REMATCH[5]}
    
    echo "Connecting to PostgreSQL database at $DB_HOST:$DB_PORT/$DB_NAME"
    
    # Install psql if not available
    if ! command -v psql &> /dev/null; then
      echo "Installing PostgreSQL client..."
      apt-get update && apt-get install -y postgresql-client
    fi
    
    # Run the schema creation script
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USERNAME -d $DB_NAME -f /tmp/init-schema.sql
    
    echo "Schema initialization completed."
  else
    echo "Error: Could not parse database URL"
  fi
fi

# Execute the next command in the chain
exec "$@" 