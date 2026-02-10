# FabFlix - Movie E-Commerce Application

A full-stack movie browsing and purchasing web application built with Java Servlets, JSP, MySQL/PostgreSQL, and jQuery.

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
| Database | PostgreSQL or MySQL with HikariCP connection pooling |
| Caching | Redis (optional) |
| Frontend | jQuery, HTML5, CSS3 |
| Security | BCrypt, Google reCAPTCHA |
| Logging | SLF4J + Logback |
| External APIs | TMDB (The Movie Database) |

## Quick Start

### Prerequisites

- Java 11+
- Maven
- PostgreSQL (Mac/Linux) or MySQL (Windows)
- Tomcat 9+

> **📖 For detailed setup instructions, see [`docs/NEW_MACHINE_SETUP.md`](docs/NEW_MACHINE_SETUP.md)**

### Quick Setup

**PostgreSQL (Mac/Linux):**
```bash
cd sql/
./setup_database.sh
```

**MySQL (Windows):**
```bash
cd sql/
./setup_database_mysql.sh
```

See [`sql/README.md`](sql/README.md) for all SQL scripts and manual setup instructions.

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
├── docs/                   # 📚 All documentation
│   ├── README.md
│   ├── NEW_MACHINE_SETUP.md   # ⭐ Start here for setup
│   ├── WINDOWS_DB_SETUP.md
│   ├── REDIS_SETUP.md
│   └── ...
├── sql/                    # 🗄️ All database scripts
│   ├── README.md
│   ├── schema_mysql.sql       # MySQL schema
│   ├── createtable.sql        # PostgreSQL schema
│   ├── movie-data.sql         # Dataset (~12MB)
│   ├── setup_database.sh      # PostgreSQL setup script
│   ├── setup_database_mysql.sh # MySQL setup script
│   └── ...
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
├── docker-compose.yml     # Docker configuration
└── pom.xml                # Maven dependencies
```

## Database Scripts

See [`sql/README.md`](sql/README.md) for complete documentation. Quick reference:

| Script | Purpose | Database |
|--------|---------|----------|
| `sql/schema_mysql.sql` | **Complete MySQL schema** (recommended) | MySQL |
| `sql/createtable.sql` | Schema definition | PostgreSQL |
| `sql/movie-data.sql` | Full movie dataset (~10,000+ movies) | Both |
| `sql/setup_database.sh` | Automated PostgreSQL setup | PostgreSQL |
| `sql/setup_database_mysql.sh` | Automated MySQL setup | MySQL |

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

See [`docs/README.md`](docs/README.md) for all documentation. Quick links:

- **[🚀 New Machine Setup](docs/NEW_MACHINE_SETUP.md)** - Complete setup guide for Mac/Windows
- [Windows DB Setup](docs/WINDOWS_DB_SETUP.md) - Windows-specific MySQL setup
- [Redis Setup](docs/REDIS_SETUP.md) - Redis caching configuration
- [Database Schema](docs/database.md) - Database design and architecture
- [Development Roadmap](docs/nextStepsV2.md) - Future improvements and features

## License

MIT
