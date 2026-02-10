# SQL Scripts Directory

This directory contains all database-related SQL scripts and setup utilities for the FabFlix application.

## Database Support

The project supports both **PostgreSQL** (primary) and **MySQL** (adapted). Use the appropriate scripts for your database.

## Quick Start

### PostgreSQL (Mac/Linux)
```bash
./setup_database.sh
```

### MySQL (Windows)
```bash
./setup_database_mysql.sh
```

Or manually follow the steps in [`../docs/NEW_MACHINE_SETUP.md`](../docs/NEW_MACHINE_SETUP.md).

---

## Files Overview

### Core Schema Files

| File | Database | Description |
|------|----------|-------------|
| `schema.sql` | PostgreSQL | Simple schema definition (PostgreSQL syntax) |
| `schema_mysql.sql` | MySQL | **Complete MySQL schema** (recommended for fresh MySQL setups) |
| `createtable.sql` | Both | Detailed table creation script (PostgreSQL syntax, mostly compatible) |

**Recommendation:**
- **PostgreSQL**: Use `createtable.sql` + schema updates
- **MySQL**: Use `schema_mysql.sql` (includes all tables and columns)

---

### Data Files

| File | Size | Description |
|------|------|-------------|
| `movie-data.sql` | ~12MB | Complete movie database with ~10,000+ movies, stars, genres |
| `sample_data.sql` | ~2KB | Sample customer and credit card data for testing |

**Note:** These are database-agnostic and work with both PostgreSQL and MySQL.

---

### Schema Update Scripts

#### PostgreSQL
| File | Purpose |
|------|---------|
| `update_schema_auth.sql` | Adds `role` column to customers table |
| `update_schema_orders.sql` | Creates `orders` and `order_items` tables |
| `reset_sequences.sql` | Resets auto-increment sequences after data import |

#### MySQL
| File | Purpose |
|------|---------|
| `update_schema_auth_mysql.sql` | Adds `role` column (MySQL syntax) |
| `update_schema_orders_mysql.sql` | Creates `orders` and `order_items` tables (MySQL syntax) |

**Important:** Always run schema update scripts **after** creating base tables!

---

### Setup Scripts

| File | Database | Description |
|------|----------|-------------|
| `setup_database.sh` | PostgreSQL | Automated setup for PostgreSQL |
| `setup_database_mysql.sh` | MySQL | Automated setup for MySQL |

These scripts handle:
1. Database creation
2. Table creation
3. Data loading
4. Password hashing (BCrypt migration)

---

### Legacy/Deprecated Files

| File | Status | Reason |
|------|--------|--------|
| `hash_passwords.sql` | ⚠️ Deprecated | Replaced by `PasswordMigration.java` utility |
| `reset_sequences.sql` | ℹ️ PostgreSQL-only | Not needed for MySQL |

---

## Setup Order

### Option 1: Automated Setup

**PostgreSQL:**
```bash
./setup_database.sh
```

**MySQL:**
```bash
./setup_database_mysql.sh
```

### Option 2: Manual Setup

#### PostgreSQL
```bash
psql -d moviedb -f createtable.sql
psql -d moviedb -f ../src/main/resources/update_schema_auth.sql
psql -d moviedb -f ../src/main/resources/update_schema_orders.sql
psql -d moviedb -f movie-data.sql
psql -d moviedb -f reset_sequences.sql
```

Then run password migration:
```bash
mvn clean package
java -cp "target/fabflix/WEB-INF/lib/*:target/classes" \
     com.neelanshkhare.fabflix.util.PasswordMigration
```

#### MySQL
```bash
# Option A: Use complete schema (recommended)
mysql -u root -p moviedb < schema_mysql.sql
mysql -u root -p moviedb < movie-data.sql

# Option B: Use base schema + updates
mysql -u root -p moviedb < createtable.sql
mysql -u root -p moviedb < update_schema_auth_mysql.sql
mysql -u root -p moviedb < update_schema_orders_mysql.sql
mysql -u root -p moviedb < movie-data.sql
```

Then run password migration:
```bash
mvn clean package
java -cp "target/fabflix/WEB-INF/lib/*;target/classes" ^
     com.neelanshkhare.fabflix.util.PasswordMigration
```

---

## Schema Update Files Location

Some schema update files are located in `src/main/resources/` for deployment:
- `src/main/resources/update_schema_auth.sql` (PostgreSQL)
- `src/main/resources/update_schema_auth_mysql.sql` (MySQL)
- `src/main/resources/update_schema_orders.sql` (PostgreSQL)
- `src/main/resources/update_schema_orders_mysql.sql` (MySQL)

These are the same as the ones referenced above, just stored in the resources folder for runtime access if needed.

---

## Common Issues

### MySQL: "Duplicate column name 'role'"
This means the column already exists. Safe to ignore.

### PostgreSQL: "Sequence not found"
Run `reset_sequences.sql` after importing `movie-data.sql`.

### Login fails after setup
1. Verify `role` column exists: `SHOW COLUMNS FROM customers;` (MySQL) or `\d customers` (PostgreSQL)
2. Check password is BCrypt hashed (starts with `$2a$`)
3. Run password migration utility

---

## Test Credentials

After setup, test login with:
- **Email:** `jbrown@ics185.edu`
- **Password:** `keyboard`

---

## Additional Documentation

For complete setup instructions, troubleshooting, and database switching, see:
- [`../docs/NEW_MACHINE_SETUP.md`](../docs/NEW_MACHINE_SETUP.md) - Comprehensive setup guide
- [`../docs/WINDOWS_DB_SETUP.md`](../docs/WINDOWS_DB_SETUP.md) - Windows-specific instructions
- [`../docs/database.md`](../docs/database.md) - Database schema documentation

---

**Last Updated:** 2026-02-09