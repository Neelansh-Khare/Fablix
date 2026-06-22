# Windows Development Environment Setup Guide

This guide describes how to set up FabFlix locally on Windows. The project uses **PostgreSQL** exclusively — MySQL is not supported.

## Prerequisites

- Docker Desktop (recommended — handles everything automatically)
- Or: Java 17+, Maven, PostgreSQL 15+

---

## Option A: Docker (Recommended)

```bash
cp .env.example .env          # add your TMDB_API_KEY and RECAPTCHA_SECRET
docker-compose up -d
```

That's it. The DB initialises automatically on first run. On subsequent runs the volume persists — no re-initialisation.

**Test credentials:**

| Email | Password | Role |
|-------|----------|------|
| `jbrown@ics185.edu` | `keyboard` | customer |
| `admin@fabflix.com` | `admin123` | admin |

---

## Option B: Local PostgreSQL

### 1. Install PostgreSQL 15
Download from [postgresql.org](https://www.postgresql.org/download/windows/). Note the superuser password you set.

### 2. Create database and user

```sql
CREATE DATABASE fabflix;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE fabflix TO postgres;
```

### 3. Load schema and data

Run these scripts in order from the `sql/` directory:

```bash
psql -U postgres -d fabflix -f sql/createtable.sql
psql -U postgres -d fabflix -f sql/movie-data.sql
psql -U postgres -d fabflix -f sql/reset_sequences.sql
psql -U postgres -d fabflix -f sql/add_movie_procedure.sql
psql -U postgres -d fabflix -f sql/add_star_procedure.sql
psql -U postgres -d fabflix -f sql/count_movies_filtered.sql
psql -U postgres -d fabflix -f sql/get_movie_details.sql
psql -U postgres -d fabflix -f sql/get_recommendations.sql
psql -U postgres -d fabflix -f sql/search_movies_optimized.sql
psql -U postgres -d fabflix -f sql/update_schema_auth.sql
psql -U postgres -d fabflix -f sql/update_schema_fts.sql
psql -U postgres -d fabflix -f sql/update_schema_orders.sql
psql -U postgres -d fabflix -f sql/update_schema_ratings.sql
```

### 4. Configure `src/main/resources/db.properties`

```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/fabflix
db.username=postgres
db.password=password
```

### 5. Build and run

```bash
mvn package -DskipTests
# Deploy target/fabflix.war to Tomcat 9
```

---

## Troubleshooting

**Login fails:** Passwords in `movie-data.sql` are plaintext. The app handles this automatically and upgrades to BCrypt on first login.

**`search_movies_optimized` does not exist:** Run `sql/search_movies_optimized.sql` and then `sql/update_schema_ratings.sql`.

**Role/salt columns missing:** Run `sql/update_schema_auth.sql`.
