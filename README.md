# FabFlix - Movie E-Commerce Application

A full-stack movie browsing and purchasing web application built with Java Servlets, JSP, PostgreSQL, and jQuery.

## Features

- **Movie Browsing**: Search, filter by genre/year/director, and browse movies
- **User Authentication**: BCrypt-hashed passwords with Google reCAPTCHA protection
- **Shopping Cart**: Server-side cart with checkout functionality
- **Movie Posters**: Automatic TMDB integration for movie posters and trailers
- **Admin Panel**: Protected admin routes for management

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java Servlets, JSP |
| Database | PostgreSQL with HikariCP connection pooling |
| Frontend | jQuery, HTML5, CSS3 |
| Security | BCrypt, Google reCAPTCHA |
| Logging | SLF4J + Logback |
| External APIs | TMDB (The Movie Database) |

## Quick Start

### Prerequisites

- Java 11+
- Maven
- PostgreSQL
- Tomcat 9+

### Database Setup

```bash
# 1. Create database
psql -c "CREATE DATABASE fabflix;"

# 2. Run setup script (creates tables, loads data, fixes sequences, hashes passwords)
./setup_database.sh

# Or manually:
psql -d fabflix -f createtable.sql
psql -d fabflix -f movie-data.sql
psql -d fabflix -f reset_sequences.sql
java -cp "target/fabflix/WEB-INF/lib/*:target/classes" \
     com.neelanshkhare.fabflix.util.PasswordMigration
```

### Configuration

Copy and edit `src/main/resources/config.properties`:

```properties
# TMDB API Key (https://www.themoviedb.org/settings/api)
tmdb.api.key=YOUR_TMDB_API_KEY

# Google reCAPTCHA (https://www.google.com/recaptcha/admin)
recaptcha.site.key=YOUR_RECAPTCHA_SITE_KEY
recaptcha.secret.key=YOUR_RECAPTCHA_SECRET_KEY
```

### Build & Run

```bash
mvn clean package
# Deploy target/fabflix.war to Tomcat, or:
mvn tomcat7:run
```

### Test Credentials

| Email | Password |
|-------|----------|
| jbrown@ics185.edu | keyboard |
| jblack@ics185.edu | paper |
| kwhite@ics185.edu | book |

## Project Structure

```
fabflix/
├── src/main/java/com/neelanshkhare/fabflix/
│   ├── dao/impl/          # Data Access Objects
│   ├── filter/            # Auth filters
│   ├── listener/          # ServletContextListeners
│   ├── model/             # Domain models
│   ├── service/           # Business logic
│   ├── servlet/           # HTTP endpoints
│   └── util/              # Utilities (Security, Config, etc.)
├── src/main/webapp/
│   ├── js/                # Frontend JavaScript
│   ├── WEB-INF/           # Web config
│   └── index.jsp          # Main entry point
├── createtable.sql        # Schema definition
├── movie-data.sql         # Full dataset
├── sample_data.sql        # Minimal test data
├── reset_sequences.sql    # Fix PostgreSQL sequences
└── setup_database.sh      # Complete setup script
```

## Database Scripts

| Script | Purpose |
|--------|---------|
| `createtable.sql` | Creates all tables with proper constraints |
| `movie-data.sql` | Full movie dataset (~160k lines) |
| `sample_data.sql` | Minimal sample data for testing |
| `reset_sequences.sql` | Resets SERIAL sequences after bulk inserts |
| `setup_database.sh` | Complete setup: tables + data + sequences + passwords |

## Key Components

### Automatic Poster Population

`PosterPopulationListener` runs on startup and periodically (every 6 hours) to:
- Find movies without posters
- Fetch from TMDB API (rate-limited)
- Update database with poster/trailer URLs

### Password Migration

`PasswordMigration.java` converts legacy plaintext passwords to BCrypt hashes:

```bash
java -cp "target/fabflix/WEB-INF/lib/*:target/classes" \
     com.neelanshkhare.fabflix.util.PasswordMigration
```

## Documentation

- [Windows Setup Guide](WINDOWS_DB_SETUP.md)
- [Development Roadmap](nextStepsV2.md)

## License

MIT
