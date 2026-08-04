#!/bin/bash
set -e

for db in iot_auth iot_ota; do
  echo "Checking database $db..."
  if psql -U "$POSTGRES_USER" -lqt | cut -d \| -f 1 | grep -qw "$db"; then
    echo "Database $db already exists, skipping."
  else
    echo "Creating database $db..."
    psql -U "$POSTGRES_USER" -c "CREATE DATABASE $db;"
  fi
done
