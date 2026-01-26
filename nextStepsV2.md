# FabFlix Project Development Roadmap V2 (2025-12-29) - UPDATED 2026-01-26

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

---

## 1. Current Status Overview
*   **Architecture:** Java Servlets + JSP + PostgreSQL + jQuery Frontend.
*   **Authentication:** BCrypt-based security. `AdminFilter` protects admin routes.
*   **Posters:** Robust TMDB integration with caching and rate limiting.
*   **Cart/Checkout:** Server-side implementation with `CartServlet`.
*   **Database:** HikariCP connection pooling enabled.

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

### 4.1. Redis Integration Plan for FabFlix
We will integrate Redis to solve two specific problems, moving us closer to a "Netflix-lite" architecture:

1.  **Distributed Session Store:**
    *   *Why:* Prepare for horizontal scaling (multiple Tomcat instances).
    *   *Fix:* Use **Redis for Session Management**.
2.  **Application Caching (Replacing local caches):**
    *   *Fix:* Use **Jedis** or **Lettuce** to store:
        *   Poster URLs (`Key: movie_poster:{id} -> Value: url`)
        *   Autocomplete results (`Key: autocomplete:{query} -> Value: json_list`)
    *   *Benefit:* Persistent cache, faster restarts, shared state.

**Implementation Steps:**
1.  Run Redis (Docker or Local).
2.  Add `jedis` dependency to `pom.xml`.
3.  Create `RedisUtil` class.
4.  Refactor `MovieService` to check Redis before DB/API.

---

## 5. Production Readiness & DevOps (Priority 3)

### 5.1. Critical Infrastructure
*   **[x] Connection Pooling:** Replaced custom `DBConnectionUtil` with **HikariCP**.
*   **[x] Logging & Error Handling:** Replaced `e.printStackTrace()` with structured logging (SLF4J/Logback).
*   **HTTPS Implementation:** Configure Tomcat to serve content over HTTPS (Self-signed for local, Certificate for Prod).
*   **Global Exception Handling:** Implement a `Filter` or custom error pages in `web.xml` to handle 404/500 errors gracefully.

### 5.2. AWS Deployment
*   **EC2 Deployment:** Deploy to AWS EC2 (Free Tier).
*   **Load Balancing:**
    *   *Phase A:* Set up **Apache HTTP Server** as a software load balancer/reverse proxy.
    *   *Phase B:* Migrate to **AWS Elastic Load Balancer (ELB)** and Auto Scaling Group (ASG).

---

## 6. Advanced Database & Scalability (Priority 4)

### 6.1. Database Optimization
*   **Stored Procedures:** Move complex logic into PostgreSQL Stored Procedures.
*   **Full-Text Search:** Optimize `MATCH AGAINST` syntax for movie searching.
*   **PostgreSQL Replication:** Implement Master-Slave replication.

### 6.2. Containerization (Long Term)
*   **Kubernetes:** Containerize the application (Docker) and deploy to a Kubernetes cluster for orchestration, replacing the manual EC2/ASG setup.

---

## 7. User Recommendations (Priority 5)

### 7.1. Simple Recommendation Engine
*   **Collaborative Filtering (Basic):** Recommend movies based on what similar users have purchased/rated.
*   **Content-Based Filtering:** Recommend movies based on genres, directors, or stars from the user's purchase history.
*   **"Users who bought X also bought Y":** Simple co-purchase analysis stored in a recommendations table.

### 7.2. Implementation Steps
1.  Create `recommendations` table to store precomputed recommendations.
2.  Implement `RecommendationService` to generate recommendations based on user history.
3.  Add recommendation display on movie detail pages and user dashboard.
4.  (Optional) Periodic batch job to refresh recommendations.

---
