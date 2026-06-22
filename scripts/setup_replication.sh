#!/bin/bash
# Script to help set up PostgreSQL replication

echo "=== FabFlix Database Replication Setup ==="
echo ""
echo "This script provides instructions and partial automation for setting up"
echo "PostgreSQL Master-Slave streaming replication."
echo ""

if [ "$1" == "--master" ]; then
    echo "Setting up Master Node..."
    DB_NAME="${DB_NAME:-fabflix}"
    psql -d "$DB_NAME" -f setup_replication.sql
    
    echo ""
    echo "IMPORTANT NEXT STEPS FOR MASTER:"
    echo "1. Edit postgresql.conf:"
    echo "   listen_addresses = '*'"
    echo "   wal_level = replica"
    echo "   max_wal_senders = 10"
    echo "   max_replication_slots = 10"
    echo ""
    echo "2. Edit pg_hba.conf to allow the replicator user:"
    echo "   host    replication     replicator      [SLAVE_IP]/32     md5"
    echo ""
    echo "3. Restart PostgreSQL service."

elif [ "$1" == "--slave" ]; then
    echo "Setting up Slave Node..."
    echo ""
    echo "IMPORTANT STEPS FOR SLAVE:"
    echo "1. Stop PostgreSQL service."
    echo "2. Clear out the existing data directory:"
    echo "   rm -rf /var/lib/postgresql/data/*"
    echo ""
    echo "3. Run pg_basebackup from the Master:"
    echo "   pg_basebackup -h [MASTER_IP] -U replicator -p 5432 -D /var/lib/postgresql/data/ -Fp -Xs -R"
    echo "   (You will be prompted for the replica_password)"
    echo ""
    echo "4. Ensure postgresql.conf on Slave has:"
    echo "   hot_standby = on"
    echo ""
    echo "5. Restart PostgreSQL service on the Slave."
else
    echo "Usage: ./setup_replication.sh [--master | --slave]"
    echo ""
    echo "Run with --master on your primary database server."
    echo "Run with --slave on your replica database server."
fi
