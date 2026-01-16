# FabFlix Project Development Roadmap V2 (2025-12-29)

This document is an updated roadmap for the FabFlix application, reflecting the current codebase status and incorporating new requirements for Redis integration and production-grade architecture.

## 1. Current Status Overview
*   **Architecture:** Java Servlets + JSP + MySQL + jQuery Frontend.
*   **Authentication:** Functional but has security flaws (hardcoded keys, basic hashing).
*   **Posters:** Asynchronous fetching implemented but relies on hardcoded keys and in-memory caching.
*   **Cart/Checkout:** Currently **Client-Side Only** (localStorage). This is a major limitation for a real-world app.
*   **Database:** `movie-data.sql` and `sample_data.sql` exist for recovery. Connection pooling is custom (should be HikariCP).

---

## 2. Immediate Critical Fixes (Priority 0)

### 2.1. Database Recovery & Stability
*   **Task:** Ensure the MySQL database is up and running.
*   **Action:** Run `sample_data.sql` or `movie-data.sql` to restore the schema and data.
*   **Verify:** Check `db.properties` configuration.

### 2.2. Security: Externalize Secrets
*   **Problem:** `TMDB_API_KEY` (MoviePosterUtil) and `RECAPTCHA_SECRET` (RecaptchaUtil) are hardcoded.
*   **Action:**
    1.  Create a `.env` file or use environment variables.
    2.  Update `MoviePosterUtil` and `RecaptchaUtil` to read from `System.getenv()` or a secure properties file not in git.

### 2.3. Fix Poster Implementation
*   **Problem:** User reported bugs. Potential causes: Invalid API key, rate limiting, or async thread pool issues.
*   **Action:**
    1.  Verify TMDB API Key is valid.
    2.  Add better error logging in `MovieService.fetchPosterAsync`.
    3.  Ensure `posterFetchCache` invalidates correctly if a fetch fails.

---

## 3. Core Feature Implementation (Priority 1)

### 3.05 Misc Updates
* **Task:** Fix Recaptcha, unable to login rightnow. site key must be updated
* **Action:** Login as admin and populate all movie posters
* **Task:** Then remove poster admin

### 3.1. Server-Side Cart & Checkout (Major Task)
*   **Current State:** Cart lives in browser `localStorage`.
*   **Goal:** Persist cart in Server Session (and eventually Database).
*   **Plan:**
    1.  **Session Cart:** Create `Cart` class and store it in `HttpSession`.
    2.  **API Endpoints:** Create `CartServlet` (`/api/cart`) to handle `add`, `remove`, `update`, `view` on the server.
    3.  **Frontend Update:** Modify `main.js` to call `/api/cart` instead of manipulating `localStorage`.
    4.  **Checkout:** Implement `CheckoutServlet` to:
        *   Validate User Session.
        *   Create `Order` in DB (requires `OrderDAO`).
        *   Save `OrderItems` in DB.
        *   Clear Session Cart.

### 3.2. Authentication Hardening

*   **Task:** Upgrade Password Security.
*   **Action:** Replace SHA-256 with **Argon2** or **BCrypt** (using libraries like Bouncy Castle or jBCrypt).
*   **Task:** Admin Authorization.
*   **Action:** Implement `AdminFilter` to protect `/_dashboard` or administrative servlets.

---

## 4. Advanced Architecture: Redis & Netflix-Style Caching (Priority 2)

You requested to explore Redis and understand how Netflix handles this.

### 4.1. Netflix Production Insight
Netflix uses a tiered caching strategy:
*   **EVCache (Memcached-based):** Primary distributed key-value store for caching request responses (user history, recommendations). It handles massive scale.
*   **Redis:** Used for more complex data structures, sorted sets (e.g., "Top 10" lists), and specific high-speed use cases where persistence/replication features of Redis are needed over Memcached.
*   **Pattern:** They use "Write-Behind" or "Write-Through" caching where data is written to the cache and DB asynchronously or synchronously.

### 4.2. Redis Integration Plan for FabFlix
We will integrate Redis to solve two specific problems, moving us closer to a "Netflix-lite" architecture:

1.  **Distributed Session Store:**
    *   *Why:* If we scale to 2 Tomcat servers, a user logged into Server A isn't logged into Server B.
    *   *Fix:* Use **Redis for Session Management**. Tomcat can be configured to store `HttpSession` data in Redis.
2.  **Application Caching (Replacing `ConcurrentHashMap`):**
    *   *Current:* `MovieService` uses a local Java `ConcurrentHashMap` for posters. This memory is lost on restart and not shared between servers.
    *   *Fix:* Use **Jedis** or **Lettuce** (Java Redis clients) to store:
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
*   **Connection Pooling:** Replace custom `DBConnectionUtil` with **HikariCP** (already in `pom.xml`).
*   **HTTPS Implementation:** Configure Tomcat to serve content over HTTPS (Self-signed for local, Certificate for Prod). Crucial for security.
*   **Logging & Error Handling:** Replace `e.printStackTrace()` with structured logging (SLF4J/Logback). Implement global error handling to stop swallowing exceptions.

### 5.2. AWS Deployment
*   **EC2 Deployment:** Deploy to AWS EC2 (Free Tier).
*   **Load Balancing:**
    *   *Phase A:* Set up **Apache HTTP Server** as a software load balancer/reverse proxy.
    *   *Phase B:* Migrate to **AWS Elastic Load Balancer (ELB)** and Auto Scaling Group (ASG).

---

## 6. Advanced Database & Scalability (Priority 4)

### 6.1. Database Optimization
*   **Stored Procedures:** Move complex logic (e.g., insertion of movies/stars/genres) into MySQL Stored Procedures for performance and encapsulation.
*   **Full-Text Search:** Verify and optimize the `MATCH AGAINST` syntax for movie searching. Ensure indices are correctly applied.
*   **MySQL Replication:** Implement Master-Slave replication. Configure the application (via HikariCP or custom logic) to direct Writes to Master and Reads to Slaves.

### 6.2. Containerization (Long Term)
*   **Kubernetes:** Containerize the application (Docker) and deploy to a Kubernetes cluster for orchestration, replacing the manual EC2/ASG setup.

---

## Summary of Next Steps for You (User)
1.  **Confirm Database:** Is your MySQL running and populated?
2.  **Secrets:** Do you have a valid TMDB API Key?
3.  **Choice:** Do you want to start with **fixing the existing Poster/Auth bugs** OR **implementing the Redis integration** immediately?
