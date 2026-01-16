# FabFlix Project Development Roadmap - Next Steps (2025-11-12)

This document outlines the next steps for developing the FabFlix application, incorporating a deeper analysis of existing code, addressing identified bugs, and planning for future features and deployment as per the provided project description.

## 1. Re-evaluation of Existing Issues (from ANALYSIS-11-12-2025.md)

The initial analysis highlighted several critical areas for improvement:

*   **Hardcoded API Keys:** `TMDB_API_KEY` in `MoviePosterUtil.java` and `SECRET_KEY`/`SITE_KEY` in `RecaptchaUtil.java` are hardcoded. This is a major security vulnerability.
*   **Inadequate Connection Pooling:** The custom `DBConnectionUtil` is not robust. The `pom.xml` already includes `HikariCP`, which should be utilized.
*   **Missing Admin Authorization:** The `PosterUpdateServlet` lacks proper access control, allowing any logged-in user to trigger admin-level operations.
*   **Password Hashing Algorithm:** SHA-256 is used, which is acceptable but could be improved with more modern, computationally intensive algorithms like Argon2 or scrypt for enhanced security.
*   **Error Handling:** `e.printStackTrace()` is used extensively, which swallows exceptions and makes debugging and error reporting difficult in a production environment.
*   **Frontend Dependencies:** The reliance on jQuery is functional but dated. While not an immediate bug, it's a consideration for long-term maintainability and modernization.
*   **Client-side Cart:** The shopping cart is currently managed in `localStorage`, which does not persist across devices or sessions and is not robust for a production e-commerce feature.

## 2. New Integration Bugs & Missing Features

A deeper dive into the project's integration points and comparison with the `.webp` project description reveals further areas needing attention:

*   **Database Broken (Critical):** You mentioned, "I also believe i broke the Database and it doesn’t exist so how would i get it back online with some basic data." This is the most immediate and critical issue, as the application cannot function without a database.
*   **Poster Implementation Bug (High Priority):** You explicitly requested fixing this. While `MovieService` attempts asynchronous fetching, there might be issues with:
    *   The `TMDB_API_KEY` being invalid or misssing.
    *   The `MoviePosterUtil`'s parsing or URL construction.
    *   The `MovieDAOImpl`'s update logic for `banner_url`/`trailer_url`.
    *   Frontend display logic in `main.js` (e.g., `onerror` handling, placeholder logic).
*   **Autocomplete Bug (High Priority):** You explicitly requested fixing this. Potential issues could be in:
    *   `AutocompleteServlet`: Incorrect query construction, inefficient database calls, or improper JSON response formatting.
    *   `autocomplete.js`: Issues with debouncing, suggestion rendering, keyboard navigation, or selection logic.
    *   The `.webp` mentions "auto-complete backed by a cache" - this caching aspect needs to be verified or implemented.
*   **Server-Side Checkout & Order Processing (Missing Core Feature):** The `.webp` describes "cart checkout backed by sessions". The current `main.js` simulates payment. A robust server-side implementation is required, including:
    *   Storing orders and order items in the database.
    *   Handling payment processing (even if simulated for now).
    *   Associating orders with customer sessions.
*   **HTTPS (Missing Security Feature):** The `.webp` lists HTTPS as a technology used. This is crucial for securing user data (login, payment).
*   **Stored Procedures (Missing Optimization):** The `.webp` lists Stored Procedures as a technology. Identifying and implementing suitable database operations as stored procedures can improve performance and security.
*   **Full-Text Search (Verification/Improvement):** The `.webp` mentions "full-text search". `MovieDAOImpl.searchMovies` uses `MATCH AGAINST`. This needs to be verified for effectiveness and potentially optimized.
*   **Caching for Search/Autocomplete (Verification/Improvement):** The `.webp` mentions "auto-complete backed by a cache". This needs to be explicitly implemented or verified if it's implicitly handled (e.g., by the connection pool or database query cache).
*   **MySQL Replication (Missing Scalability Feature):** The `.webp` mentions "MySQL replication" for performance optimization.
*   **Apache Load Balancing (Missing Scalability Feature):** The `.webp` mentions "Apache load balancing" for performance optimization.
*   **Kubernetes Deployment (Long-term Scalability):** The `.webp` mentions "Deployed Containerized version of the project on a Kubernetes cluster". This is a significant undertaking and a long-term goal.
*   **Android App Development (Out of Scope for Web App):** The `.webp` mentions "Android app development". This is outside the scope of the current web application project.

## 3. Recommendations and Plan Forward (Ranked Steps)

Here is a step-by-step plan, prioritizing immediate fixes and core features, then moving to advanced features and AWS deployment. I will provide detailed instructions for each step when you prompt me to continue.

---

**Phase 1: Immediate Stability & Core Functionality**

**Step 1: Database Recovery and Initial Setup**
*   **Issue:** Database is broken/non-existent.
*   **Recommendation:** Recreate the MySQL database and tables, and populate with basic sample data. This is foundational for all other work.
*   **Plan:** Provide SQL scripts for schema creation and initial data insertion.
*   **Your Action:** Prompt me to start with "Step 1: Database Recovery and Initial Setup".

**Step 2: Fix Poster Implementation Bug**
*   **Issue:** Movie posters are not reliably displaying or being fetched.
*   **Recommendation:** Debug `MoviePosterUtil`, `MovieService`, and `main.js` to ensure correct API key usage, robust API calls, proper data updates, and correct frontend rendering.
*   **Plan:**
    *   Verify `TMDB_API_KEY` configuration.
    *   Review `MoviePosterUtil` for API call errors, rate limiting, and response parsing.
    *   Review `MovieService` for asynchronous update logic and error handling.
    *   Review `main.js` for image loading and error handling.
*   **Your Action:** Prompt me to start with "Step 2: Fix Poster Implementation Bug".

**Step 3: Fix Autocomplete Bug & Implement Caching**
*   **Issue:** Autocomplete functionality may be buggy or inefficient. The `.webp` mentions caching.
*   **Recommendation:** Refine the autocomplete logic and implement a server-side cache for autocomplete suggestions to improve performance.
*   **Plan:**
    *   Review `AutocompleteServlet` and `autocomplete.js` for functional issues.
    *   Introduce a caching mechanism (e.g., using a `ConcurrentHashMap` or a dedicated caching library) in `MovieService` or `AutocompleteServlet` for frequently searched terms.
*   **Your Action:** Prompt me to start with "Step 3: Fix Autocomplete Bug & Implement Caching".

**Step 4: Externalize API Keys and Sensitive Configuration**
*   **Issue:** API keys and reCAPTCHA secrets are hardcoded.
*   **Recommendation:** Move all sensitive keys and configurations to external properties files or environment variables, ensuring they are not committed to version control.
*   **Plan:**
    *   Modify `MoviePosterUtil` and `RecaptchaUtil` to read keys from `db.properties` or environment variables.
    *   Update `db.properties` to include these keys (with placeholders).
*   **Your Action:** Prompt me to start with "Step 4: Externalize API Keys and Sensitive Configuration".

---

**Phase 2: Core Features & Security Enhancements**

**Step 5: Implement Server-Side Checkout and Order Processing**
*   **Issue:** Checkout is client-side simulated; orders are not persisted.
*   **Recommendation:** Develop a robust server-side order management system, including new DAOs, Services, and Servlets to handle order creation, storage, and association with customer sessions.
*   **Plan:**
    *   Create `OrderDAO`, `OrderItemDAO` and their implementations.
    *   Create `OrderService`.
    *   Develop a new `CheckoutServlet` to handle order submission, payment simulation, and database persistence.
    *   Update `main.js` to interact with the new `CheckoutServlet`.
*   **Your Action:** Prompt me to start with "Step 5: Implement Server-Side Checkout and Order Processing".

**Step 6: Replace Custom Connection Pool with HikariCP**
*   **Issue:** The custom connection pool is basic and potentially inefficient.
*   **Recommendation:** Integrate `HikariCP` for robust and high-performance database connection management.
*   **Plan:**
    *   Modify `DBConnectionUtil` to initialize and manage connections using `HikariCP`.
    *   Ensure all DAOs correctly acquire and release connections from the HikariCP pool.
*   **Your Action:** Prompt me to start with "Step 6: Replace Custom Connection Pool with HikariCP".

**Step 7: Implement Role-Based Access Control (RBAC) for Admin Endpoints**
*   **Issue:** Admin endpoints (e.g., `PosterUpdateServlet`) lack proper authorization.
*   **Recommendation:** Introduce user roles (e.g., 'admin', 'customer') and implement a Servlet Filter to restrict access to admin functionalities based on user roles.
*   **Plan:**
    *   Add a `role` column to the `customers` table.
    *   Create a `SecurityFilter` to intercept requests to admin servlets.
    *   Modify `AuthServlet` to store user roles in the session upon login.
*   **Your Action:** Prompt me to start with "Step 7: Implement Role-Based Access Control (RBAC) for Admin Endpoints".

**Step 8: Improve Error Handling and Logging**
*   **Issue:** Errors are swallowed (`e.printStackTrace()`), making debugging difficult.
*   **Recommendation:** Implement a consistent error handling strategy, propagating exceptions and utilizing the configured SLF4J/Logback for structured logging.
*   **Plan:**
    *   Replace `e.printStackTrace()` with `LOGGER.log(Level.SEVERE, "...", e)` in all DAOs and Servlets.
    *   Implement custom exceptions for specific error conditions (e.g., `DataAccessException`).
    *   Consider a global exception handler or error pages for user-friendly error messages.
*   **Your Action:** Prompt me to start with "Step 8: Improve Error Handling and Logging".

**Step 9: Implement HTTPS**
*   **Issue:** Communication is unencrypted.
*   **Recommendation:** Configure Tomcat to serve content over HTTPS, securing all data in transit.
*   **Plan:**
    *   Generate a self-signed SSL certificate (for local development) or obtain a certificate (for AWS).
    *   Configure Tomcat's `server.xml` to enable HTTPS connector.
    *   (For AWS) Configure EC2 security groups and potentially an Application Load Balancer for SSL termination.
*   **Your Action:** Prompt me to start with "Step 9: Implement HTTPS".

**Step 10: Implement Stored Procedures**
*   **Issue:** Stored procedures are mentioned in the `.webp` but not yet implemented.
*   **Recommendation:** Identify complex or frequently executed database operations and refactor them into stored procedures for potential performance gains and better encapsulation.
*   **Plan:**
    *   Analyze `MovieDAOImpl` and `StarDAOImpl` for suitable candidates (e.g., `findById` with joins, `insert` operations involving multiple tables).
    *   Write SQL stored procedures.
    *   Modify relevant DAO methods to call these stored procedures.
*   **Your Action:** Prompt me to start with "Step 10: Implement Stored Procedures".

---

**Phase 3: AWS Deployment & Scalability**

**Step 11: Local Development Environment Setup (Review/Confirm)**
*   **Issue:** You mentioned you have Tomcat, MySQL, backend, and some frontend setup locally. This step is to confirm and ensure a consistent local environment for development.
*   **Recommendation:** Document the exact steps for setting up the local development environment from scratch, including IDE, Maven, Tomcat, and MySQL.
*   **Plan:** Provide a checklist and basic instructions for setting up the local environment.
*   **Your Action:** Prompt me to start with "Step 11: Local Development Environment Setup".

**Step 12: Deploy to AWS EC2 (Free Tier)**
*   **Issue:** Project needs to be deployed to AWS.
*   **Recommendation:** Deploy the application to a single EC2 instance using the AWS Free Tier.
*   **Plan:**
    *   Detailed steps for launching an EC2 instance (Ubuntu/Amazon Linux).
    *   Installing Java, Tomcat, MySQL (or connecting to RDS Free Tier).
    *   Configuring security groups.
    *   Deploying the WAR file.
*   **Your Action:** Prompt me to start with "Step 12: Deploy to AWS EC2 (Free Tier)".

**Step 13: Implement MySQL Replication (Master/Slave)**
*   **Issue:** The `.webp` mentions MySQL replication for performance.
*   **Recommendation:** Set up a master-slave replication for the MySQL database on AWS (potentially using RDS Free Tier or two EC2 instances).
*   **Plan:**
    *   Configure MySQL master and slave instances.
    *   Update `DBConnectionUtil` to use the master for writes and slaves for reads (read/write splitting).
*   **Your Action:** Prompt me to start with "Step 13: Implement MySQL Replication (Master/Slave)".

**Step 14: Implement Apache Load Balancing**
*   **Issue:** The `.webp` mentions Apache load balancing.
*   **Recommendation:** Set up Apache HTTP Server as a reverse proxy and load balancer for multiple Tomcat instances (even if only one is active initially, the setup will be ready for scaling).
*   **Plan:**
    *   Install and configure Apache HTTP Server on a separate EC2 instance.
    *   Configure `mod_jk` or `mod_proxy_ajp` to forward requests to Tomcat.
    *   Set up basic load balancing configuration.
*   **Your Action:** Prompt me to start with "Step 14: Implement Apache Load Balancing".

**Step 15: Future Consideration: Kubernetes Deployment**
*   **Issue:** The `.webp` mentions Kubernetes deployment.
*   **Recommendation:** This is a significant architectural shift and likely beyond the free tier. It should be considered a long-term goal after the current project is stable and deployed.
*   **Plan:** Briefly outline the high-level steps involved in containerizing the application and deploying to Kubernetes.
*   **Your Action:** Prompt me to start with "Step 15: Future Consideration: Kubernetes Deployment".

---

I am ready to begin with **Step 1: Database Recovery and Initial Setup**. Please let me know when you are ready for me to proceed.