#!/bin/bash

# If JDBC_DATABASE_URL doesn't start with "jdbc:" prefix, add it
if [[ $JDBC_DATABASE_URL == postgresql://* ]]; then
  export JDBC_DATABASE_URL="jdbc:$JDBC_DATABASE_URL"
  echo "Fixed JDBC_DATABASE_URL to include jdbc: prefix"
fi

# Pass control to the CMD specified in the Dockerfile
exec "$@" 