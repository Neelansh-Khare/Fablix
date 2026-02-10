# FabFlix Database Setup Guide (PostgreSQL)

This guide provides step-by-step instructions to delete your old databases, set up a new PostgreSQL database, and populate it with the necessary data for the FabFlix project.

## 1. Prerequisites

Ensure you have **PostgreSQL** installed and running on your system.

*   **Check Status:** `pg_ctl status` or `brew services list` (if using Homebrew on macOS).
*   **Start PostgreSQL:** `brew services start postgresql` (macOS) or `sudo service postgresql start` (Linux).

## 2. Deleting Old Databases (PostgreSQL)

**WARNING:** This will permanently delete all data in the specified databases.

1.  Access the PostgreSQL command line tool:
    ```bash
    psql postgres
    ```

2.  List existing databases to confirm the name:
    ```sql
    \l
    ```

3.  Drop (delete) the old `fabflix` database (if it exists):
    ```sql
    DROP DATABASE IF EXISTS fabflix;
    ```
    *(If there are other active sessions connected to the database, this command might fail. In that case, you may need to terminate those connections first.)*

4.  Exit the `psql` shell:
    ```sql
    \q
    ```

## 3. Creating the New Database

1.  Create a new, empty database named `fabflix`:
    ```bash
    createdb fabflix
    ```

## 4. Populating the Database

You will use the SQL scripts provided in this repository. 

**Critical Note:** The `movie-data.sql` file in this repository currently contains **only** `INSERT` statements for movies. It does **not** contain the table definitions (CREATE TABLE) or data for Stars, Genres, or Customers.

**You must run the scripts in this specific order:**

### Step 4.1: Define the Schema (Create Tables)
Since there isn't a dedicated schema file visible in the root, you should create one or use the standard schema for this project. 
*   *Action:* Create a file named `createtable.sql` with your table definitions (Movies, Stars, Stars_in_Movies, Genres, Genres_in_Movies, Customers, Sales, CreditCards, Ratings).
*   *Run:*
    ```bash
    psql -d fabflix -f createtable.sql
    ```

### Step 4.2: Import Movie Data
Use `movie-data.sql` to populate the `movies` table.
*   *Run:*
    ```bash
    psql -d fabflix -f movie-data.sql
    ```

### Step 4.3: Import Other Data (Stars, Genres, etc.)
If you have other data files (e.g., for stars or genres) that were not in `movie-data.sql`, run them now.
*   *Run:*
    ```bash
    psql -d fabflix -f [other_data_file].sql
    ```

*(If you only have `movie-data.sql` and `sample_data.sql`, you might be missing data for Stars and Genres. `sample_data.sql` contains a small set of everything, which is good for testing but small for production.)*

## 5. Verifying the Setup

1.  Log into your new database:
    ```bash
    psql fabflix
    ```

2.  Check if tables exist:
    ```sql
    \dt
    ```

3.  Count the movies to ensure import was successful:
    ```sql
    SELECT COUNT(*) FROM movies;
    ```

## 6. Project Configuration

Since you are switching to PostgreSQL, you **must** update your project configuration to use the PostgreSQL JDBC driver instead of MySQL.

1.  **Update `pom.xml`:**
    Replace the `mysql-connector-j` dependency with the PostgreSQL driver:
    ```xml
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.2</version> <!-- Use the latest stable version -->
    </dependency>
    ```

2.  **Update `db.properties` (or `application.properties`):**
    Change the driver class and URL:
    ```properties
    db.driver=org.postgresql.Driver
    db.url=jdbc:postgresql://localhost:5432/fabflix
    db.username=[your_postgres_username]
    db.password=[your_postgres_password]
    ```
