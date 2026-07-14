# Documentation Directory

This directory contains all project documentation for the FabFlix application.

## Files Overview

### Setup & Installation

| File | Description | Target Platform |
|------|-------------|----------------|
| [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md) | **🌟 Primary setup guide** - Comprehensive instructions for setting up on new machines | All platforms |
| [`WINDOWS_DB_SETUP.md`](WINDOWS_DB_SETUP.md) | Windows-specific MySQL setup instructions | Windows |
| [`setup.md`](setup.md) | General setup documentation | All |
| [`database.md`](database.md) | Database schema and architecture documentation | All |
| [`REDIS_SETUP.md`](REDIS_SETUP.md) | Redis cache setup and configuration | All |

### Project Planning & Analysis

| File | Description |
|------|-------------|
| [`nextSteps.md`](nextSteps.md) | Project roadmap and future improvements |
| [`nextStepsV2.md`](nextStepsV2.md) | Updated project roadmap |
| [`ANALYSIS-11-12-2025.md`](ANALYSIS-11-12-2025.md) | Project analysis and audit report |
| [`SPRING_BOOT_VS_CURRENT_SETUP.md`](SPRING_BOOT_VS_CURRENT_SETUP.md) | Compares the current Maven + plain Servlet setup against a Spring Boot rewrite |

## Quick Start

**New to this project?** Start here:

1. **Read:** [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md) for complete setup instructions
2. **Database:** Choose PostgreSQL (Mac) or MySQL (Windows)
3. **Configure:** Update `src/main/resources/db.properties` and `config.properties`
4. **Run SQL Scripts:** Follow instructions in [`../sql/README.md`](../sql/README.md)
5. **Build:** `mvn clean package`
6. **Deploy:** Copy `target/fabflix.war` to Tomcat

## Platform-Specific Guides

### macOS / Linux
- Use PostgreSQL (recommended)
- Follow the PostgreSQL setup section in [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md)
- Run `sql/setup_database.sh`

### Windows
- Use MySQL (recommended)
- Follow the MySQL setup section in [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md) or [`WINDOWS_DB_SETUP.md`](WINDOWS_DB_SETUP.md)
- Run `sql/setup_database_mysql.sh`

## Common Tasks

### Troubleshooting Login Issues
See "Common Issues & Troubleshooting" section in [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md)

### Database Schema Reference
See [`database.md`](database.md)

### Setting up Redis Cache
See [`REDIS_SETUP.md`](REDIS_SETUP.md)

### Switching Between Databases
See "Switching Between Databases" section in [`NEW_MACHINE_SETUP.md`](NEW_MACHINE_SETUP.md)

## Document Hierarchy

```
docs/
├── README.md (this file)
│
├── Setup & Installation
│   ├── NEW_MACHINE_SETUP.md ⭐ (Start here!)
│   ├── WINDOWS_DB_SETUP.md
│   ├── setup.md
│   └── REDIS_SETUP.md
│
├── Database
│   └── database.md
│
└── Planning & Analysis
    ├── nextSteps.md
    ├── nextStepsV2.md
    └── ANALYSIS-11-12-2025.md
```

## Additional Resources

- **SQL Scripts:** See [`../sql/README.md`](../sql/README.md)
- **Main README:** See [`../README.md`](../README.md)
- **Source Code:** `../src/`

## Contributing

When adding new documentation:
1. Place in this `docs/` directory
2. Update this README with a link
3. Use clear, descriptive filenames
4. Include a "Last Updated" date at the bottom

---

**Last Updated:** 2026-02-09
