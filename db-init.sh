#!/bin/bash

echo "Setting up PostgreSQL database..."

# Make sure we have the postgres client
if ! command -v psql &> /dev/null; then
  echo "Installing PostgreSQL client..."
  apt-get update -y && apt-get install -y postgresql-client curl
fi

# Create a simple SQL script to set up database
cat > /tmp/init-db.sql << EOF
-- Create tables in public schema
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS todos (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    completed BOOLEAN DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert admin user if it doesn't exist
INSERT INTO users (username, password, email, full_name)
SELECT 'admin', '\$2a\$10\$rYlzymM7HxwB1IcrJZeW0.oAYnL8kDagGGtzFNK0CRnpvAVUu3jdu', 'admin@example.com', 'Admin User'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin') AND role = 'ROLE_ADMIN');

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin') AND role = 'ROLE_USER');
EOF

# Extract database connection details
if [[ $JDBC_DATABASE_URL =~ jdbc:postgresql://([^:]+):([^/]+)/(.+) ]]; then
  DB_HOST=${BASH_REMATCH[1]}
  DB_PORT=${BASH_REMATCH[2]}
  DB_NAME=${BASH_REMATCH[3]}
  
  echo "Executing SQL on database: $DB_NAME"
  
  # Run the SQL script
  PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USERNAME -d $DB_NAME -f /tmp/init-db.sql
  
  echo "Database initialization completed"
else
  echo "Could not parse JDBC_DATABASE_URL. Using application's built-in schema creation."
fi

# Execute the next command in the chain
exec "$@" 