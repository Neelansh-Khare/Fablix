#!/bin/bash
set -e

# Wait for master to be ready
until pg_isready -h db-primary -p 5432 -U postgres; do
  echo "Waiting for master database..."
  sleep 2
done

# Check if data directory is empty
if [ -z "$(ls -A /var/lib/postgresql/data)" ]; then
    echo "Initializing replica from master..."
    # Perform base backup
    # The replicator password is set in setup_replication.sql (replica_password)
    PGPASSWORD=replica_password pg_basebackup \
        -h db-primary \
        -D /var/lib/postgresql/data \
        -U replicator \
        -vP -R --slot=replica_slot
    
    # Ensure correct permissions
    chmod 700 /var/lib/postgresql/data
    
    echo "Replica initialized successfully."
else
    echo "Data directory not empty, skipping initialization."
fi

# Start PostgreSQL
exec postgres -c hot_standby=on
