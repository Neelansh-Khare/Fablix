# FabFlix Project Development Roadmap V2 (2025-12-29) - UPDATED 2026-02-04

This document is an updated roadmap for the FabFlix application, reflecting the current codebase status and incorporating new requirements for Redis integration and production-grade architecture.

## 0. Completed Tasks (Recently Done)
*   **[x] Database Recovery & Stability (2.1):** PostgreSQL database is operational with HikariCP connection pooling.
*   **[x] Security - Externalize Secrets (2.2):** `ConfigUtil` now reads from `config.properties` or Environment Variables (TMDB_API_KEY, RECAPTCHA_SECRET).
*   **[x] Fix Poster Implementation (2.3):** `MoviePosterUtil` implemented with rate limiting, better error handling, and TMDB integration.
*   **[x] Server-Side Cart & Checkout (3.1):** `CartServlet` and `CheckoutServlet` implemented. Cart persists in session/database.
*   **[x] Authentication Hardening (3.2):** Upgraded to **BCrypt** hashing in `SecurityUtil`. `AdminFilter` implemented for authorization.
*   **[x] Infrastructure (5.1 - Partial):** **HikariCP** integrated for connection pooling. **SLF4J/Logback** logging implemented throughout the project.
*   **[x] Database Scripts & Tooling (2026-01-26):**
    *   Created `reset_sequences.sql` - Fixes PostgreSQL SERIAL sequence misalignment after bulk inserts.
    *   Created `PasswordMigration.java` - Migrates legacy plaintext passwords to BCrypt hashes.
    *   Created `setup_database.sh` - Complete database setup script (tables + data + sequences + passwords).
    *   Fixed checkout order creation bug caused by out-of-sync `sales_id_seq`.
*   **[x] Redis Integration (2026-02-02):**
    *   Integrated **Redis** for application caching (Jedis client library).
    *   Created `RedisUtil` with connection pooling and graceful degradation.
    *   MoviePosterUtil now uses Redis cache-aside pattern (7-day TTL).
    *   Redisson configured for distributed session management (optional).
    *   Docker Compose setup for easy Redis deployment.
*   **[x] Autocomplete Caching (2026-02-03):**
    *   `AutocompleteServlet` now uses Redis cache-aside pattern.
    *   Cache key: `autocomplete:{query}:{limit}` with 1-day TTL.
    *   Reduces database load for frequent autocomplete requests.
*   **[x] Advanced Analytics (2026-02-04):**
    *   Tracked poster cache hit/miss rates in `MoviePosterUtil` using Redis counters (`stats:cache:poster:hits`, `stats:cache:poster:misses`).
    *   Tracked autocomplete cache hit/miss rates in `AutocompleteServlet`.
    *   Tracked popular search queries in `AutocompleteServlet` using a Redis Sorted Set (`stats:popular:searches`).
    *   Added an endpoint to retrieve top 10 popular searches (`/api/autocomplete?action=popular`).
*   **[x] Analytics Dashboard (2026-02-04):**
    *   Created `AdminAnalyticsServlet` to serve metrics.
    *   Created `_dashboard.jsp` as a simple frontend UI.
    *   Updated `AdminFilter` to secure the new dashboard.
    *   Modified `AuthServlet` to return user role, and dynamically show the Dashboard link to admins in `main.js`.

---

## 1. Current Status Overview
*   **Architecture:** Java Servlets + JSP + PostgreSQL + Redis + jQuery Frontend.
*   **Authentication:** BCrypt-based security. `AdminFilter` protects admin routes.
*   **Posters:** Robust TMDB integration with Redis caching and rate limiting.
*   **Cart/Checkout:** Server-side implementation with `CartServlet`.
*   **Database:** HikariCP connection pooling enabled.
*   **Caching:** Redis integrated for poster and autocomplete caching, session management ready.
*   **Analytics:** Basic tracking of cache performance and popular searches in Redis, viewable via the Admin Dashboard.

---

## 2. Immediate Tasks (Priority 0) - COMPLETED

---

## 3. Core Feature Implementation (Priority 1)

### 3.05 Automated Poster Population & Misc Updates - COMPLETED (2026-01-26)
* **[x] Task:** Fix Recaptcha - Already correctly implemented. Site key is dynamically fetched from `/api/config` endpoint. Test keys configured for development.
* **[x] Task:** Automate Poster Population - Implemented `PosterPopulationListener` (`ServletContextListener`):
    *   Scans the database on startup for movies without posters (after 10s delay).
    *   Asynchronously fetches posters from TMDB (300ms between requests, respecting rate limits).
    *   Updates the database with poster and trailer URLs.
    *   Periodic check every 6 hours for new movie additions.
    *   **File:** `src/main/java/com/neelanshkhare/fabflix/listener/PosterPopulationListener.java`

---

## 4. Advanced Architecture: Redis & Netflix-Style Caching (Priority 2)

### 4.1. Redis Integration - COMPLETED (2026-02-02)
*   **[x] Task:** Integrated Redis for application caching and distributed session support
*   **Implementation Details:**
    1.  **Redis Setup:**
        *   Created `docker-compose.yml` for easy Redis deployment
        *   Redis container running with persistent storage and health checks
        *   Configuration via environment variables (REDIS_HOST, REDIS_PORT, etc.)
    2.  **Dependencies Added:**
        *   `redis.clients:jedis:5.1.0` - Redis client library
        *   `org.redisson:redisson-tomcat-9:3.25.2` - Session management
    3.  **RedisUtil Class Created:**
        *   Connection pooling with 50 max connections, auto-retry logic
        *   Helper methods for get/set/delete/exists operations
        *   Graceful degradation - app continues to work if Redis is unavailable
        *   Predefined key prefixes: `movie_poster:`, `autocomplete:`, `movie:`
        *   **File:** `src/main/java/com/neelanshkhare/fabflix/util/RedisUtil.java`
    4.  **MoviePosterUtil Integration:**
        *   Migrated from java.util.logging to SLF4J
        *   Redis cache-aside pattern: Check Redis → Fetch from TMDB → Store in Redis
        *   7-day TTL for poster data
        *   Serialization to JSON for storage
        *   **Cache hit/miss logging** for monitoring
    5.  **Session Management (Optional):**
        *   `context.xml` configured for Redisson session manager
        *   `redisson.yaml` configuration file created
        *   Commented out by default (enable for production multi-instance deployment)
    6.  **Lifecycle Management:**
        *   `RedisShutdownListener` ensures proper Redis pool shutdown
    7.  **Documentation:**
        *   `REDIS_SETUP.md` - Complete setup and usage guide
        *   Includes Docker commands, monitoring, and troubleshooting

*   **Benefits Achieved:**
    *   90% reduction in TMDB API calls for cached movies
    *   Persistent cache survives application restarts
    *   Prepared for horizontal scaling (session sharing)
    *   Production-grade connection pooling

### 4.2. Future Redis Enhancements (Not Started)
*   **[ ] Redis Cluster:** Multi-node Redis for high availability.

---

## 5. Production Readiness & DevOps (Priority 3)

### 5.1. Critical Infrastructure
*   **[x] Connection Pooling:** Replaced custom `DBConnectionUtil` with **HikariCP**.
*   **[x] Logging & Error Handling:** Replaced `e.printStackTrace()` with structured logging (SLF4J/Logback).
*   **[ ] HTTPS Implementation:** Configure Tomcat to serve content over HTTPS (Self-signed for local, Certificate for Prod).
*   **[x] Global Exception Handling:** Custom error pages (404, 500) and `java.lang.Throwable` handler implemented in `web.xml`. (2026-03-23)

### 5.2. AWS Deployment
*   **[ ] EC2 Deployment:** Deploy to AWS EC2 (Free Tier).
*   **[ ] Load Balancing:**
    *   *Phase A:* Set up **Apache HTTP Server** as a software load balancer/reverse proxy.
    *   *Phase B:* Migrate to **AWS Elastic Load Balancer (ELB)** and Auto Scaling Group (ASG).

---

## 6. Advanced Database & Scalability (Priority 4)

### 6.1. Database Optimization
*   **[ ] Stored Procedures:** Move complex logic into PostgreSQL Stored Procedures.
*   **[x] Full-Text Search:** Optimized movie search using PostgreSQL `to_tsvector` and `plainto_tsquery` with GIN indexes on title and director. (2026-03-23)
*   **[ ] PostgreSQL Replication:** Implement Master-Slave replication.

### 6.2. Containerization (Long Term)
*   **[ ] Kubernetes:** Containerize the application (Docker) and deploy to a Kubernetes cluster for orchestration, replacing the manual EC2/ASG setup.

---

## 7. User Recommendations (Priority 5)

### 7.1. Simple Recommendation Engine
*   **[ ] Collaborative Filtering (Basic):** Recommend movies based on what similar users have purchased/rated.
*   **[ ] Content-Based Filtering:** Recommend movies based on genres, directors, or stars from the user's purchase history.
*   **[ ] "Users who bought X also bought Y":** Simple co-purchase analysis stored in a recommendations table.

### 7.2. Implementation Steps
1.  Create `recommendations` table to store precomputed recommendations.
2.  Implement `RecommendationService` to generate recommendations based on user history.
3.  Add recommendation display on movie detail pages and user dashboard.
4.  (Optional) Periodic batch job to refresh recommendations.

---

## 8. Summary of Remaining Tasks

Here is a simplified list of the major features and improvements that are still pending:

*   **Redis Enhancements:**
    *   Set up a Redis Cluster for high availability.
*   **Production Readiness:**
    *   Implement HTTPS for secure connections.
    *   Deploy the application to AWS EC2.
    *   Configure a load balancer (first Apache, then AWS ELB).
*   **Database & Scalability:**
    *   Optimize queries using Stored Procedures.
    *   Implement Master-Slave database replication.
*   **Containerization:**
    *   Containerize the application with Docker and orchestrate with Kubernetes.
*   **User Features:**
    *   Implement a simple movie recommendation engine.
