#!/bin/bash
# FabFlix MySQL Database Setup Script

set -e

# Default values
DB_NAME="${DB_NAME:-moviedb}" # Changed default to moviedb to match user's db.properties
DB_USER_DEFAULT="${DB_USER_DEFAULT:-root}"    # Default to root for MySQL, user can override

echo "=== FabFlix MySQL Database Setup ==="
echo "Database: $DB_NAME"
echo ""

# Prompt for MySQL User and Password
read -p "Enter MySQL username (default: $DB_USER_DEFAULT): " INPUT_DB_USER
DB_USER="${INPUT_DB_USER:-$DB_USER_DEFAULT}"
read -s -p "Enter MySQL password for $DB_USER: " DB_PASSWORD
echo ""

# Step 1: Create database if it doesn't exist
echo "Step 1: Creating database '$DB_NAME' if it doesn't exist..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
echo "Database created (if not existing)."
echo ""

# Step 2: Create tables
echo "Step 2: Creating tables..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < createtable.sql
echo "Tables created."
echo ""

# Step 3: Load movie data
echo "Step 3: Loading movie data..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < movie-data.sql
echo "Movie data loaded."
echo ""

# Step 4: Load sample data (optional)
# This step is optional, only run if you want sample data
# echo "Step 4: Loading sample data..."
# mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < sample_data.sql
# echo "Sample data loaded."
# echo ""

# Step 5: Migrate passwords to BCrypt
echo "Step 5: Migrating passwords to BCrypt..."
if [ -d "target/fabflix/WEB-INF/lib" ]; then
    java -cp "target/fabflix/WEB-INF/lib/*:target/classes"
         com.neelanshkhare.fabflix.util.PasswordMigration
else
    echo "Warning: Application not built. Run 'mvn package' first, then run:"
    echo "  java -cp 'target/fabflix/WEB-INF/lib/*:target/classes' "
    echo "       com.neelanshkhare.fabflix.util.PasswordMigration"
fi
echo ""

echo "=== Setup Complete ==="
echo ""
echo "Note: The 'reset_sequences.sql' script is PostgreSQL-specific and has been omitted."
echo "If you encounter auto-incrementing primary key issues in MySQL after manual ID inserts,"
echo "you may need to manually reset them using 'ALTER TABLE table_name AUTO_INCREMENT = N;'."
echo ""
echo "Test login credentials:"
echo "  Email: jbrown@ics185.edu"
echo "  Password: keyboard"
echo ""
echo "Remember to run 'mvn clean install' and re-deploy the application."
