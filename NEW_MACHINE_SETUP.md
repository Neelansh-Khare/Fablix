# FabFlix - New Machine Setup Guide

This guide helps you set up the FabFlix application on a new development machine. The project supports both PostgreSQL (typically on Mac) and MySQL (typically on Windows).

## Table of Contents
- [Prerequisites](#prerequisites)
- [Database Choice: PostgreSQL vs MySQL](#database-choice-postgresql-vs-mysql)
- [PostgreSQL Setup (Mac/Linux)](#postgresql-setup-maclinux)
- [MySQL Setup (Windows)](#mysql-setup-windows)
- [Configuration Files](#configuration-files)
- [Common Issues & Troubleshooting](#common-issues--troubleshooting)
- [SQL Scripts Reference](#sql-scripts-reference)

---

## Prerequisites

### All Platforms
- **Java**: JDK 11 or higher
- **Maven**: 3.6+
- **Tomcat**: 9.0+
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

### Platform-Specific
- **Mac/Linux**: PostgreSQL 12+ recommended
- **Windows**: MySQL 8.0+ or PostgreSQL

---

## Database Choice: PostgreSQL vs MySQL

The project was originally designed for PostgreSQL but has been adapted to work with MySQL.

### Current Status
| Component | PostgreSQL | MySQL |
|-----------|-----------|--------|
| Primary target | ✅ Yes | ⚠️ Adapted |
| pom.xml dependency | ❌ Not included | ✅ Included |
| SQL scripts | ✅ Native | ⚠️ Requires conversion |
| Schema updates | ✅ `update_schema_*.sql` | ✅ `update_schema_*_mysql.sql` |

### Key Differences

**PostgreSQL Syntax:**
```sql
-- Auto-increment
id SERIAL PRIMARY KEY

-- Conditional column add
ALTER TABLE customers ADD COLUMN IF NOT EXISTS role VARCHAR(20);

-- Sequences
SELECT setval('customers_id_seq', ...);
```

**MySQL Syntax:**
```sql
-- Auto-increment
id INT AUTO_INCREMENT PRIMARY KEY

-- Conditional column add (not supported, must catch error)
ALTER TABLE customers ADD COLUMN role VARCHAR(20);

-- Auto-increment reset
ALTER TABLE customers AUTO_INCREMENT = 100;
```

---

## PostgreSQL Setup (Mac/Linux)

### 1. Install PostgreSQL
```bash
# macOS (using Homebrew)
brew install postgresql@14
brew services start postgresql@14

# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### 2. Create Database
```bash
# Connect as postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE moviedb;
CREATE USER fabflix WITH PASSWORD 'fabflix123';
GRANT ALL PRIVILEGES ON DATABASE moviedb TO fabflix;
\q
```

### 3. Run Setup Script
```bash
./setup_database.sh
```

This script will:
1. Create all tables (using `createtable.sql`)
2. Load movie data (using `movie-data.sql`)
3. Reset sequences (using `reset_sequences.sql`)
4. Migrate passwords to BCrypt

### 4. Configure Application

**Edit `src/main/resources/db.properties`:**
```properties
# PostgreSQL Configuration
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/moviedb
db.username=fabflix
db.password=fabflix123
db.min_connections=5
db.max_connections=10
```

### 5. Update pom.xml

**Ensure PostgreSQL dependency is included:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
</dependency>
```

**Remove or comment out MySQL dependency:**
```xml
<!-- Remove this if using PostgreSQL -->
<!--
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
-->
```

---

## MySQL Setup (Windows)

### 1. Install MySQL
- Download MySQL 8.0+ from [MySQL Downloads](https://dev.mysql.com/downloads/installer/)
- During installation, set root password (remember this!)
- Install MySQL Workbench (recommended for GUI access)

### 2. Create Database
```bash
# Using MySQL command line
mysql -u root -p

# Create database
CREATE DATABASE moviedb;
USE moviedb;
exit;
```

### 3. Run Setup Script (Automated)
```bash
# Run the MySQL setup script
./setup_database_mysql.sh
```

Or follow manual steps below:

### 4. Manual MySQL Setup

**Step 1: Create tables**
```bash
mysql -u root -p moviedb < createtable.sql
```

**Step 2: Add missing columns** (IMPORTANT!)
```bash
# This adds the 'role' column which is missing in the base schema
mysql -u root -p moviedb < src/main/resources/update_schema_auth_mysql.sql

# This creates orders tables
mysql -u root -p moviedb < src/main/resources/update_schema_orders_mysql.sql
```

**Step 3: Load movie data**
```bash
mysql -u root -p moviedb < movie-data.sql
```

**Step 4: Hash passwords**

First, build the project:
```bash
mvn clean package
```

Then run the password migration utility:
```bash
java -cp "target/fabflix/WEB-INF/lib/*;target/classes" ^
     com.neelanshkhare.fabflix.util.PasswordMigration
```

### 5. Configure Application

**Edit `src/main/resources/db.properties`:**
```properties
# MySQL Configuration
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&useSSL=false
db.username=root
db.password=password
db.min_connections=5
db.max_connections=10
```

### 6. Update pom.xml

**Ensure MySQL dependency is included:**
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

---

## Configuration Files

### Files to Update When Switching Databases

| File | What to Change | Example |
|------|----------------|---------|
| `src/main/resources/db.properties` | Driver, URL, credentials | See sections above |
| `pom.xml` | Database dependency (MySQL vs PostgreSQL) | See sections above |
| `src/main/resources/config.properties` | API keys (TMDB, reCAPTCHA) | No database-specific changes |

### config.properties
```properties
# TMDB API Key (Get one at https://www.themoviedb.org/settings/api)
tmdb.api.key=YOUR_TMDB_API_KEY

# Google reCAPTCHA Secrets (Get them at https://www.google.com/recaptcha/admin)
recaptcha.secret.key=YOUR_SECRET_KEY
recaptcha.site.key=YOUR_SITE_KEY
```

---

## Common Issues & Troubleshooting

### Issue 1: NullPointerException - "Cannot invoke Customer.getId() because customer is null"

**Symptom:**
```
java.lang.NullPointerException: Cannot invoke "com.neelanshkhare.fabflix.model.Customer.getId()" because "customer" is null
    at CustomerServlet.doPost(CustomerServlet.java:215)
```

**Root Cause:** The `customers` table is missing the `role` column, causing the SQL query in `findByEmail()` to fail silently.

**Solution:**
```sql
-- For MySQL
ALTER TABLE customers ADD COLUMN role VARCHAR(20) DEFAULT 'customer';

-- For PostgreSQL
ALTER TABLE customers ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'customer';
```

**Verify the fix:**
```sql
-- Check if column exists
SHOW COLUMNS FROM customers LIKE 'role';  -- MySQL
\d customers                                -- PostgreSQL

-- Verify customer can be queried
SELECT id, email, first_name, last_name, role FROM customers WHERE email = 'jbrown@ics185.edu';
```

### Issue 2: Login Fails with Correct Credentials

**Possible Causes:**
1. Password not properly hashed
2. Email mismatch (check for typos like `ics185.uci.edu` vs `ics185.edu`)
3. Missing `role` column (see Issue 1)

**Solution:**

**Check the customer record:**
```sql
SELECT id, email, password, salt FROM customers WHERE email = 'jbrown@ics185.edu';
```

**BCrypt passwords should:**
- Start with `$2a$` or `$2b$`
- Be 60 characters long
- Have `salt` column set to `NULL`

**To reset a user's password to "keyboard":**

Option 1: Generate hash using the utility
```bash
# Compile the project first
mvn compile

# Run the hash generator (create this file if it doesn't exist)
mvn exec:java -Dexec.mainClass="com.neelanshkhare.fabflix.util.GenerateHash"
```

Option 2: Use a pre-generated BCrypt hash
```sql
-- This hash is for the password "admin"
UPDATE customers
SET password = '$2a$10$Z7VvINy6Q1Z7VvINy6Q1Z.XxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXx',
    salt = NULL
WHERE email = 'your@email.com';
```

### Issue 3: SQL Syntax Errors When Running PostgreSQL Scripts on MySQL

**Symptom:**
```
ERROR 1064 (42000): You have an error in your SQL syntax; check the manual...
```

**Solution:** Use the MySQL-specific versions of scripts:
- Use `schema_mysql.sql` instead of `schema.sql`
- Use `update_schema_auth_mysql.sql` instead of `update_schema_auth.sql`
- Use `update_schema_orders_mysql.sql` instead of `update_schema_orders.sql`

### Issue 4: "Table doesn't exist" Errors

**Solution:** Ensure tables are created in the correct order:
```bash
# For MySQL
mysql -u root -p moviedb < createtable.sql
mysql -u root -p moviedb < src/main/resources/update_schema_auth_mysql.sql
mysql -u root -p moviedb < src/main/resources/update_schema_orders_mysql.sql
mysql -u root -p moviedb < movie-data.sql

# For PostgreSQL
psql -d moviedb -f createtable.sql
psql -d moviedb -f src/main/resources/update_schema_auth.sql
psql -d moviedb -f src/main/resources/update_schema_orders.sql
psql -d moviedb -f movie-data.sql
```

### Issue 5: Connection Pool Errors

**Symptom:**
```
Could not get JDBC connection; nested exception is java.sql.SQLException: Cannot create PoolableConnectionFactory
```

**Solution:**
1. Verify database is running:
   ```bash
   # MySQL
   mysql -u root -p -e "SELECT 1"

   # PostgreSQL
   psql -c "SELECT 1"
   ```

2. Check `db.properties` has correct credentials

3. Verify database exists:
   ```bash
   # MySQL
   mysql -u root -p -e "SHOW DATABASES LIKE 'moviedb'"

   # PostgreSQL
   psql -l | grep moviedb
   ```

---

## SQL Scripts Reference

### Core Scripts (Database-Agnostic)

| Script | Purpose | When to Use | Dependencies |
|--------|---------|-------------|--------------|
| `movie-data.sql` | Movie/genre/star data | After creating tables | Requires tables to exist |
| `sample_data.sql` | Sample customer/creditcard data | Optional testing data | Requires tables to exist |
| `hash_passwords.sql` | Marks plaintext passwords for migration | Legacy, mostly unused | N/A |

### PostgreSQL Scripts

| Script | Purpose | MySQL Equivalent |
|--------|---------|-----------------|
| `createtable.sql` | Create all tables (PostgreSQL syntax) | `createtable.sql` (works for both) |
| `schema.sql` | Simplified schema definition | `schema_mysql.sql` |
| `reset_sequences.sql` | Reset auto-increment sequences | Not needed (MySQL auto-handles) |
| `setup_database.sh` | Automated PostgreSQL setup | `setup_database_mysql.sh` |
| `src/main/resources/update_schema_auth.sql` | Add `role` column | `update_schema_auth_mysql.sql` |
| `src/main/resources/update_schema_orders.sql` | Create orders tables | `update_schema_orders_mysql.sql` |

### MySQL Scripts

| Script | Purpose | Notes |
|--------|---------|-------|
| `schema_mysql.sql` | Complete MySQL-compatible schema | Includes `role` column and orders tables |
| `update_schema_auth_mysql.sql` | Add `role` column to customers | Run if using `createtable.sql` |
| `update_schema_orders_mysql.sql` | Create orders and order_items tables | Run if tables don't exist |
| `setup_database_mysql.sh` | Automated MySQL setup | Windows/Linux compatible |

### Redundant/Legacy Files

These files can be safely ignored or removed:

| File | Reason | Replacement |
|------|--------|-------------|
| `hash_passwords.sql` | Superseded by Java utility | `PasswordMigration.java` |
| `reset_sequences.sql` | PostgreSQL-only | Not needed for MySQL |

---

## Quick Reference Commands

### Build & Deploy

```bash
# Clean build
mvn clean package

# Run with Tomcat (via IDE or command line)
mvn tomcat7:run

# Deploy to external Tomcat
cp target/fabflix.war $TOMCAT_HOME/webapps/
```

### Database Verification

```bash
# MySQL - Check if setup is correct
mysql -u root -p moviedb -e "
  SELECT
    (SELECT COUNT(*) FROM movies) as movie_count,
    (SELECT COUNT(*) FROM customers) as customer_count,
    (SELECT COUNT(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_NAME='customers' AND COLUMN_NAME='role') as has_role_column;
"

# PostgreSQL - Check if setup is correct
psql -d moviedb -c "
  SELECT
    (SELECT COUNT(*) FROM movies) as movie_count,
    (SELECT COUNT(*) FROM customers) as customer_count,
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_name='customers' AND column_name='role') as has_role_column;
"
```

### Test Login Credentials

Default test credentials (after setup):
- **Email**: `jbrown@ics185.edu`
- **Password**: `keyboard`

---

## Switching Between Databases

If you need to switch between PostgreSQL and MySQL:

### 1. Update `pom.xml`
- Comment out the current database dependency
- Uncomment the target database dependency

### 2. Update `db.properties`
- Change `db.driver`
- Change `db.url`
- Update credentials

### 3. Clean and rebuild
```bash
mvn clean package
```

### 4. Restart Tomcat

---

## Best Practices

1. **Always use the MySQL-specific scripts on Windows** to avoid syntax errors
2. **Run schema update scripts** (`update_schema_*`) after initial table creation
3. **Verify the `role` column exists** before testing login functionality
4. **Use BCrypt hashes** for all passwords (never store plaintext)
5. **Keep `db.properties` out of version control** (add to `.gitignore`)

---

## Additional Resources

- [MySQL Documentation](https://dev.mysql.com/doc/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [BCrypt Info](https://en.wikipedia.org/wiki/Bcrypt)

---

## Getting Help

If you encounter issues not covered in this guide:

1. Check application logs in `$TOMCAT_HOME/logs/catalina.out`
2. Verify database connectivity using the commands in "Database Verification"
3. Ensure all schema update scripts have been run
4. Check that dependencies in `pom.xml` match your database choice

---

**Last Updated:** 2026-02-09