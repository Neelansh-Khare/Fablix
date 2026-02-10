# Windows Development Environment Setup Guide

This guide describes how to set up the FabFlix database and environment on a Windows machine.

## 1. Prerequisites
*   **Java 11 or higher** (JDK) installed.
*   **Maven** installed and added to PATH.
*   **PostgreSQL** installed (The project uses PostgreSQL driver, not MySQL, based on `db.properties`).
*   **Git** (Git Bash or generic Command Prompt).

## 2. Database Setup

### A. Install PostgreSQL
If you haven't installed PostgreSQL, download it from [postgresql.org](https://www.postgresql.org/download/windows/).
*   Remember the **superuser password** (usually for user `postgres`) you set during installation.
*   The default port is `5432`.

### B. Create Database and User
Open `pgAdmin` or `psql` (SQL Shell) and run the following commands:

```sql
-- Create the database
CREATE DATABASE fabflix;

-- Create a user (matches db.properties)
-- You can change the password 'password' to something secure, but update db.properties if you do.
CREATE USER neelanshkhare WITH PASSWORD 'password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE fabflix TO neelanshkhare;

-- Grant privileges on all tables in public schema (run this AFTER connecting to 'fabflix' db)
-- \c fabflix
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO neelanshkhare;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO neelanshkhare;
```

### C. Import Schema & Data
You need to run the SQL scripts in a specific order. You can use the command line (`psql`) or a GUI tool like DBeaver/pgAdmin.

**Using Command Line (PowerShell/CMD):**
Assuming you are in the project root directory:

```powershell
# 1. Create Base Tables
psql -U neelanshkhare -d fabflix -f createtable.sql

# 2. Import Initial Data (Choose ONE)
# Option A: Full Data (recommended)
psql -U neelanshkhare -d fabflix -f movie-data.sql
# Option B: Sample Data (minimal test data)
# psql -U neelanshkhare -d fabflix -f sample_data.sql

# 3. Apply Schema Updates (Critical for features like Checkout/Auth)
psql -U neelanshkhare -d fabflix -f src/main/resources/update_schema_orders.sql
psql -U neelanshkhare -d fabflix -f src/main/resources/update_schema_auth.sql

# 4. IMPORTANT: Reset sequences after bulk insert
# This fixes "duplicate key" errors when creating orders or new users
psql -U neelanshkhare -d fabflix -f reset_sequences.sql

# 5. Migrate plaintext passwords to BCrypt (requires built project)
# First build the project:
mvn clean package
# Then run the migration:
java -cp "target/fabflix/WEB-INF/lib/*;target/classes" ^
     com.neelanshkhare.fabflix.util.PasswordMigration
```

**Note:** If `psql` asks for a password, enter `password` (or whatever you set in Step B).

**Note:** The password migration step is required because `movie-data.sql` contains plaintext passwords, but the application uses BCrypt for authentication.

## 3. Configuration

### Update `db.properties`
Navigate to `src/main/resources/db.properties`.
Ensure the settings match your Windows PostgreSQL installation:

```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/fabflix
db.username=neelanshkhare
db.password=password
db.min_connections=5
db.max_connections=10
```

### Environment Variables
Create a `.env` file in the root directory (or set System Environment Variables in Windows Search > "Edit the system environment variables"):

```properties
TMDB_API_KEY=your_tmdb_api_key_here
RECAPTCHA_SECRET_KEY=your_recaptcha_secret_here
RECAPTCHA_SITE_KEY=your_recaptcha_site_key_here
```

## 4. Building & Running

1.  Open Command Prompt / PowerShell in the project root.
2.  Build the project:
    ```powershell
    mvn clean package
    ```
3.  Deploy the generated `.war` file (in `target/`) to your Tomcat `webapps` folder, OR use the Tomcat Maven plugin if configured:
    ```powershell
    mvn tomcat7:run
    ```
    *(Note: Check `pom.xml` for the specific run command plugin version).*

## 5. Troubleshooting

### Connection Issues
*   **Connection Refused:** Ensure PostgreSQL service is running in Windows Services (`services.msc`).
*   **Role does not exist:** Did you create the `neelanshkhare` user?
*   **Relation does not exist:** Did you run the scripts against the `fabflix` database (and not `postgres` default db)?

### Authentication Issues
*   **Login fails with valid credentials:** Passwords in `movie-data.sql` are plaintext. Run the password migration utility (Step C.5) to convert them to BCrypt hashes.
*   **"Invalid BCrypt hash" in logs:** Same issue - run password migration.

### Order/Checkout Issues
*   **"duplicate key value violates unique constraint 'sales_pkey'":** The `sales_id_seq` sequence is out of sync. Run:
    ```powershell
    psql -U neelanshkhare -d fabflix -f reset_sequences.sql
    ```
*   **Same error for customers:** Run the same script - it resets all sequences.

### TMDB Poster Issues
*   **"TMDB API authentication failed":** Your API key is invalid. Get a new one from https://www.themoviedb.org/settings/api and update `config.properties`.
*   **Posters not loading:** Check that `tmdb.api.key` is set correctly in `src/main/resources/config.properties`.

## 6. Test Credentials

After running the password migration, you can log in with:

| Email | Password |
|-------|----------|
| jbrown@ics185.edu | keyboard |
| jblack@ics185.edu | paper |
| kwhite@ics185.edu | book |
| ksmith@ics185.edu | light |
| jharris@ics185.edu | honey |
