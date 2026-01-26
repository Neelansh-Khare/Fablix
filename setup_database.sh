#!/bin/bash
# FabFlix Database Setup Script
# This script sets up the database with proper sequences and hashed passwords

set -e

DB_NAME="${DB_NAME:-fabflix}"
DB_USER="${DB_USER:-$USER}"

echo "=== FabFlix Database Setup ==="
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo ""

# Step 1: Create tables
echo "Step 1: Creating tables..."
psql -d "$DB_NAME" -f createtable.sql
echo "Tables created."
echo ""

# Step 2: Load movie data
echo "Step 2: Loading movie data..."
psql -d "$DB_NAME" -f movie-data.sql
echo "Movie data loaded."
echo ""

# Step 3: Reset sequences
echo "Step 3: Resetting sequences..."
psql -d "$DB_NAME" -f reset_sequences.sql
echo "Sequences reset."
echo ""

# Step 4: Migrate passwords to BCrypt
echo "Step 4: Migrating passwords to BCrypt..."
if [ -d "target/fabflix/WEB-INF/lib" ]; then
    java -cp "target/fabflix/WEB-INF/lib/*:target/classes" \
         com.neelanshkhare.fabflix.util.PasswordMigration
else
    echo "Warning: Application not built. Run 'mvn package' first, then run:"
    echo "  java -cp 'target/fabflix/WEB-INF/lib/*:target/classes' \\"
    echo "       com.neelanshkhare.fabflix.util.PasswordMigration"
fi
echo ""

echo "=== Setup Complete ==="
echo ""
echo "Test login credentials:"
echo "  Email: jbrown@ics185.edu"
echo "  Password: keyboard"
echo ""
echo "Note: If you skipped password migration, original plaintext passwords"
echo "will not work with the BCrypt-based authentication system."
