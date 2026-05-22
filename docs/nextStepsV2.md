# FabFlix Project Development Roadmap V2 (2025-12-29) - UPDATED 2026-05-17

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
*   **[x] Recommendation Engine Phase 1 (2026-04-06):**
    *   Created `sql/get_recommendations.sql` with stored functions for similar movies and co-purchase recommendations.
    *   Integrated recommendations into `MovieDAO`, `MovieService`, and `MovieServlet`.
*   **[x] Rating Integration (2026-05-21):**
    *   Synchronized and stored movie ratings and vote counts from TMDB into the local database.
    *   Updated `Movie` model and DAOs to support rating persistence.
    *   Optimized stored functions (`get_movie_details`, `search_movies_optimized`) to include rating data and support sorting by rating.
    *   Enhanced UI with rating badges in movie lists and star ratings on detail pages.
*   **[x] UI & DevOps Enhancements (2026-04-13):**
    *   **Recommendation UI:** Added "Similar Movies" and "Users also bought" sections to the movie details page in `main.js`.
    *   **Containerization:** Created `Dockerfile` and `docker-compose.yml` for full application stack (App + DB + Redis).
    *   **Environment Configuration:** Updated `DBConnectionUtil` to support configuration via Environment Variables.
    *   **HTTPS Support:** Configured Tomcat with self-signed certificate and HTTPS connector in `server.xml`.
    *   **Database Optimization:** Moved movie detail retrieval logic into `get_movie_details` PostgreSQL stored function.
*   **[x] Advanced Architecture & Scalability (2026-05-17):**
    *   **Redis Cluster:** Multi-node Redis for high availability. Implemented with a 6-node cluster and JedisCluster.
    *   **Automated DB Replication:** Finalized Master-Slave streaming replication with automated initialization in Docker.
    *   **Production K8s Manifests:** Created production-grade StatefulSet manifests for PostgreSQL and Redis Cluster.

---

## 1. Current Status Overview
*   **Architecture:** Java Servlets + JSP + PostgreSQL (Primary/Replica) + Redis Cluster + jQuery Frontend.
*   **Authentication:** BCrypt-based security. `AdminFilter` protects admin routes.
*   **Posters:** Robust TMDB integration with Redis caching and rate limiting.
*   **Cart/Checkout:** Server-side implementation with `CartServlet`.
*   **Database:** HikariCP connection pooling with Read/Write splitting.
*   **Caching:** Redis Cluster integrated for poster and autocomplete caching.
*   **Analytics:** Cache performance and popular searches tracking in Redis.
*   **Deployment:** Docker-ready with automated replication and production-grade K8s manifests.

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
    1.  **Redis Setup:** Created `docker-compose.yml` for easy Redis deployment.
    2.  **Dependencies Added:** `jedis:5.1.0`, `redisson-tomcat-9:3.25.2`.
    3.  **RedisUtil Class Created:** Connection pooling, JedisCluster support, graceful degradation.
    4.  **MoviePosterUtil Integration:** Cache-aside pattern with 7-day TTL.
    5.  **Session Management:** Redisson configured for distributed sessions.
    6.  **Lifecycle Management:** `RedisShutdownListener` for graceful shutdown.

### 4.2. Redis Enhancements - COMPLETED (2026-05-17)
*   **[x] Redis Cluster:** Multi-node Redis for high availability. Implemented with a 6-node cluster (3 masters, 3 replicas) and JedisCluster integration.

---

## 5. Production Readiness & DevOps (Priority 3)

### 5.1. Critical Infrastructure
*   **[x] Connection Pooling:** Replaced custom `DBConnectionUtil` with **HikariCP**.
*   **[x] Logging & Error Handling:** Replaced `e.printStackTrace()` with structured logging (SLF4J/Logback).
*   **[x] HTTPS Implementation:** Configured Tomcat to serve content over HTTPS.
*   **[x] Global Exception Handling:** Custom error pages and global exception handler.

### 5.2. AWS Deployment
*   **[ ] EC2 Deployment:** Deploy to AWS EC2 (Free Tier).
*   **[ ] Load Balancing:**
    *   *Phase A:* Set up **Apache HTTP Server** as a software load balancer. (COMPLETED 2026-04-27)
    *   *Phase B:* Migrate to **AWS Elastic Load Balancer (ELB)**.

---

## 6. Advanced Database & Scalability (Priority 4)

### 6.1. Database Optimization
*   **[x] Database Optimization:**
    *   Moved movie detail retrieval logic into `get_movie_details` PostgreSQL stored function.
    *   Optimized search and browsing queries using stored functions.
    *   Updated `search_movies_optimized` to support `star_id`.
*   **[x] Load Balancing:** Apache HTTP Server as software load balancer in Docker Compose.
*   **[x] Distributed Sessions:** Enabled Redisson session management.
*   **[x] Read/Write Splitting:** Supported separate write (primary) and read (replica) database pools.
*   **[x] Kubernetes Manifests:** Created production-grade StatefulSet manifests for K8s orchestration. (COMPLETED 2026-05-17)
*   **[x] PostgreSQL Replication:** Finalized Master-Slave streaming replication with automated setup. (COMPLETED 2026-05-17)


### 6.2. Containerization (Long Term)
*   **[x] Docker:** Containerized the application stack using Docker Compose.
*   **[ ] Kubernetes:** Deploy to a managed Kubernetes cluster (EKS/GKE).

---

## 7. User Recommendations (Priority 5)

### 7.1. Simple Recommendation Engine - PHASE 1 COMPLETED (2026-04-06)
*   **[x] Content-Based Filtering:** Recommend movies based on genres.
*   **[x] Collaborative Filtering (Basic):** "Users who bought this also bought".
*   **[x] Integration:** Movie details API returns recommendations.

---

## 8. Summary of Remaining Tasks

*   **Production Readiness:**
    *   Deploy the application stack to AWS EC2 using Docker Compose.
    *   Configure AWS Elastic Load Balancer (ELB).
*   **Containerization:**
    *   Deploy to a managed Kubernetes cluster.
