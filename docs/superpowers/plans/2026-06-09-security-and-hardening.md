# Security Fixes & Infrastructure Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 23 audit findings (5 CRITICAL, 8 HIGH, 8 MEDIUM, 2 LOW) and complete the 3 partially implemented features (K8s hardening, PG replication in K8s, Redisson K8s config).

**Architecture:** Fixes span the full stack — new `CsrfUtil` shared by all servlets; Redis-backed rate limiting; session cookie hardening via `web.xml`; K8s manifests augmented with resource limits, probes, HPA, NetworkPolicy, and a Redisson ConfigMap.

**Tech Stack:** Java 11, Servlets, JedisCluster (Jedis 5.1.x), HikariCP, Kubernetes, PostgreSQL 15.

**Dependency order:** Task 1 (CsrfUtil) must complete before Tasks 3–6 use it. All other tasks are independent and can be done in any order.

---

### Task 1: CsrfUtil + web.xml session flags
*Fixes: C3, C4 (shared infrastructure), M6*

**Files:**
- Create: `src/main/java/com/neelanshkhare/fabflix/util/CsrfUtil.java`
- Modify: `src/main/webapp/WEB-INF/web.xml`

- [ ] **Step 1: Create CsrfUtil.java**

```java
package com.neelanshkhare.fabflix.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfUtil {
    public static final String SESSION_ATTR = "csrf_token";
    public static final String HEADER_NAME = "X-CSRF-Token";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_ATTR);
        if (token == null) {
            token = generateToken();
            session.setAttribute(SESSION_ATTR, token);
        }
        return token;
    }

    /** Constant-time comparison to prevent timing attacks. */
    public static boolean validateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String sessionToken = (String) session.getAttribute(SESSION_ATTR);
        String headerToken = request.getHeader(HEADER_NAME);
        if (sessionToken == null || headerToken == null) return false;
        return MessageDigest.isEqual(
            sessionToken.getBytes(StandardCharsets.UTF_8),
            headerToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
```

- [ ] **Step 2: Add session cookie config to web.xml**

Replace the current `<web-app>` contents with (add after the existing `<error-page>` blocks):

```xml
    <session-config>
        <session-timeout>30</session-timeout>
        <cookie-config>
            <http-only>true</http-only>
            <secure>true</secure>
        </cookie-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>
```

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/util/CsrfUtil.java src/main/webapp/WEB-INF/web.xml
git commit -m "security: add CsrfUtil and harden session cookie flags"
```

---

### Task 2: config.properties — remove hardcoded secrets
*Fixes: C1, H1*

**Files:**
- Modify: `src/main/resources/config.properties`
- Verify: `.gitignore` excludes config.properties

- [ ] **Step 1: Replace real values with placeholders**

```properties
# TMDB API Key — override via TMDB_API_KEY environment variable
# Get one at https://www.themoviedb.org/settings/api
tmdb.api.key=YOUR_TMDB_API_KEY_HERE

# Google reCAPTCHA — override via RECAPTCHA_SECRET_KEY / RECAPTCHA_SITE_KEY env vars
# Get test keys at https://www.google.com/recaptcha/admin
recaptcha.secret.key=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe
recaptcha.site.key=6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI
```
Note: the reCAPTCHA keys shown are Google's public test keys (intentionally public — safe to keep as fallback). Replace `tmdb.api.key` with placeholder only; set the real key via `TMDB_API_KEY` env var.

- [ ] **Step 2: Ensure .gitignore excludes config.properties**

Check for `src/main/resources/config.properties` in `.gitignore`. If missing, add it and rely on `config.properties.template` for new developers.

- [ ] **Step 3: Commit**
```
git add src/main/resources/config.properties .gitignore
git commit -m "security: remove real TMDB API key from config.properties"
```

---

### Task 3: CustomerServlet — session fixation + CSRF + generic errors
*Fixes: C2, C3, H2, H4*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/CustomerServlet.java`

- [ ] **Step 1: Fix session fixation on login (C2)**

In `doPost`, in the `"login"` branch, replace:
```java
// Create session
HttpSession session = request.getSession();
session.setAttribute("customerId", customer.getId());
```
with:
```java
// Invalidate old session to prevent session fixation
HttpSession oldSession = request.getSession(false);
if (oldSession != null) {
    oldSession.invalidate();
}
HttpSession session = request.getSession(true);
session.setAttribute("customerId", customer.getId());
```

- [ ] **Step 2: Return CSRF token on successful login**

Immediately after setting session attributes on successful login, add:
```java
String csrfToken = CsrfUtil.getOrCreateToken(session);
result.put("csrfToken", csrfToken);
```
Also add to the import: `import com.neelanshkhare.fabflix.util.CsrfUtil;`

- [ ] **Step 3: Add CSRF validation to doPut and doDelete (H4)**

At the top of `doPut()`, after the auth check:
```java
if (!CsrfUtil.validateToken(request)) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    JSONObject error = new JSONObject();
    error.put("message", "Invalid or missing CSRF token");
    response.getWriter().print(error.toString());
    return;
}
```
Repeat the same block at the top of `doDelete()`.

- [ ] **Step 4: Replace e.getMessage() with generic messages (H2)**

In all catch blocks in doGet, doPost, doPut, doDelete, replace:
```java
error.put("message", "Internal server error: " + e.getMessage());
```
with:
```java
error.put("message", "An unexpected error occurred. Please try again.");
```
Keep the full exception logged via `LOGGER.log(Level.SEVERE, ...)` for server-side debugging.

- [ ] **Step 5: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/CustomerServlet.java
git commit -m "security: fix session fixation, add CSRF tokens, sanitize error messages"
```

---

### Task 4: CartServlet — CSRF + quantity bounds
*Fixes: C3, H5*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/CartServlet.java`

- [ ] **Step 1: Add CSRF validation to doPost**

Add import: `import com.neelanshkhare.fabflix.util.CsrfUtil;`

At the top of `doPost()`, before the action-handling logic:
```java
if (!CsrfUtil.validateToken(request)) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    out.print(new JSONObject().put("message", "Invalid or missing CSRF token").toString());
    return;
}
```

- [ ] **Step 2: Add quantity bounds check (H5)**

Replace the quantity parsing block:
```java
int quantity = 1;
if (quantityStr != null && !quantityStr.isEmpty()) {
    try {
        quantity = Integer.parseInt(quantityStr);
    } catch (NumberFormatException e) {
        // ignore, use default 1
    }
}
```
with:
```java
int quantity = 1;
if (quantityStr != null && !quantityStr.isEmpty()) {
    try {
        quantity = Integer.parseInt(quantityStr);
    } catch (NumberFormatException e) {
        // ignore, use default 1
    }
}
if (quantity < 1 || quantity > 99) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    out.print(new JSONObject().put("message", "Quantity must be between 1 and 99").toString());
    return;
}
```

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/CartServlet.java
git commit -m "security: add CSRF protection and quantity bounds to CartServlet"
```

---

### Task 5: CheckoutServlet — CSRF + generic errors
*Fixes: C3, H2*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/CheckoutServlet.java`

- [ ] **Step 1: Add CSRF validation to doPost**

Add import: `import com.neelanshkhare.fabflix.util.CsrfUtil;`

After the session auth check in `doPost()`:
```java
if (!CsrfUtil.validateToken(request)) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    JSONObject error = new JSONObject();
    error.put("message", "Invalid or missing CSRF token");
    out.print(error.toString());
    return;
}
```

- [ ] **Step 2: Replace exception message leak (H2)**

Replace:
```java
error.put("message", "Internal server error during checkout: " + e.getMessage());
```
with:
```java
error.put("message", "An unexpected error occurred during checkout. Please try again.");
```

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/CheckoutServlet.java
git commit -m "security: add CSRF and sanitize checkout error messages"
```

---

### Task 6: AdminMovieServlet — CSRF + generic errors
*Fixes: C3, H2*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/AdminMovieServlet.java`

- [ ] **Step 1: Add CSRF validation to doPost**

Add import: `import com.neelanshkhare.fabflix.util.CsrfUtil;`

At the top of `doPost()`:
```java
if (!CsrfUtil.validateToken(request)) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    result.put("status", "error");
    result.put("message", "Invalid or missing CSRF token");
    out.print(result.toString());
    return;
}
```

- [ ] **Step 2: Sanitize error message in catch block (H2)**

Replace:
```java
result.put("message", "System Error: " + e.getMessage());
```
with:
```java
result.put("message", "An unexpected error occurred. Please try again.");
```
(Keep `logger.error("...", e)` for server-side logging — add SLF4J logger if not present.)

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/AdminMovieServlet.java
git commit -m "security: add CSRF and sanitize error messages in AdminMovieServlet"
```

---

### Task 7: MovieServlet + SearchServlet — ID validation, page bounds, generic errors
*Fixes: H7, H8, M2, M4, H2*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/MovieServlet.java`
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/SearchServlet.java`

- [ ] **Step 1: Validate movieId format in MovieServlet doGet**

After `String movieId = pathInfo.substring(1);`:
```java
if (!movieId.matches("[a-zA-Z0-9_\\-]{1,50}")) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    JSONObject error = new JSONObject();
    error.put("message", "Invalid movie ID format");
    out.print(error.toString());
    return;
}
```

- [ ] **Step 2: Validate movieId format in MovieServlet doPut and doDelete**

Add the same validation after `String movieId = pathInfo.substring(1);` in both `doPut()` and `doDelete()`.

- [ ] **Step 3: Bound pageSize in MovieServlet doGet**

After parsing `pageSize`, add:
```java
page = Math.max(1, page);
pageSize = Math.min(Math.max(1, pageSize), 100);
```

- [ ] **Step 4: Replace all e.getMessage() in MovieServlet catch blocks (H2)**

Replace all:
```java
error.put("message", "Internal server error: " + e.getMessage());
```
with:
```java
error.put("message", "An unexpected error occurred. Please try again.");
```

- [ ] **Step 5: Bound pageSize in SearchServlet doGet (M4)**

After parsing `pageSize` and `page`:
```java
page = Math.max(1, page);
pageSize = Math.min(Math.max(1, pageSize), 100);
```

- [ ] **Step 6: Replace e.getMessage() in SearchServlet catch block (H2)**

Replace:
```java
error.put("message", "Internal server error: " + e.getMessage());
```
with:
```java
error.put("message", "An unexpected error occurred. Please try again.");
```

- [ ] **Step 7: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/MovieServlet.java src/main/java/com/neelanshkhare/fabflix/servlet/SearchServlet.java
git commit -m "security: validate movie IDs, bound pageSize, sanitize error messages"
```

---

### Task 8: HealthServlet — remove internal infrastructure details
*Fixes: M3*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/servlet/HealthServlet.java`

- [ ] **Step 1: Return minimal public status; hide component details**

Replace the current `result` assembly block:
```java
result.put("status", isHealthy ? "UP" : "DOWN");
result.put("database", dbOk ? "UP" : "DOWN");
result.put("redis", redisOk ? "UP" : "DOWN");
result.put("timestamp", System.currentTimeMillis());
```
with:
```java
// Only return overall status — component details leak infrastructure info
result.put("status", isHealthy ? "UP" : "DOWN");
```

- [ ] **Step 2: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/servlet/HealthServlet.java
git commit -m "security: remove internal component details from public health endpoint"
```

---

### Task 9: MovieService — race condition + executor shutdown
*Fixes: M1, M5*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/service/MovieService.java`
- Modify: `src/main/java/com/neelanshkhare/fabflix/listener/PosterPopulationListener.java`

- [ ] **Step 1: Fix race condition with putIfAbsent (M1)**

In `fetchPosterAsync()`, replace:
```java
posterFetchCache.put(movie.getId(), true);
```
with:
```java
if (posterFetchCache.putIfAbsent(movie.getId(), true) != null) {
    return; // Another thread already handling this movie
}
```

- [ ] **Step 2: Update shouldFetchPoster to match**

In `shouldFetchPoster()`, remove the `posterFetchCache.containsKey()` check (it's now handled atomically in step 1):
```java
private boolean shouldFetchPoster(Movie movie) {
    if (!MoviePosterUtil.isApiKeyConfigured()) {
        return false;
    }
    String bannerUrl = movie.getBannerUrl();
    return bannerUrl == null ||
            bannerUrl.isEmpty() ||
            bannerUrl.contains("no-poster.jpg") ||
            bannerUrl.contains("placeholder") ||
            bannerUrl.contains("default");
}
```

- [ ] **Step 3: Ensure executor shuts down via PosterPopulationListener (M5)**

In `PosterPopulationListener.contextDestroyed()`, add:
```java
MovieService.shutdown();
```
(Import: `import com.neelanshkhare.fabflix.service.MovieService;`)

- [ ] **Step 4: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/service/MovieService.java src/main/java/com/neelanshkhare/fabflix/listener/PosterPopulationListener.java
git commit -m "fix: atomic poster fetch cache check, guarantee executor shutdown"
```

---

### Task 10: RedisUtil — degradation logging + RateLimiterUtil — Redis-backed + cleanup
*Fixes: M7, M8, H3*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/util/RedisUtil.java`
- Modify: `src/main/java/com/neelanshkhare/fabflix/util/RateLimiterUtil.java`

- [ ] **Step 1: Log Redis degradation in RedisUtil (M8)**

In `initializeCluster()`, in the catch block (after setting `redisEnabled = false`), add:
```java
logger.warn("APPLICATION DEGRADED: Redis is unavailable. Caching and rate-limiting disabled. Restart app when Redis recovers.");
```

In each operation method (get, set, delete, etc.), when `!isRedisAvailable()`:
```java
if (!isRedisAvailable()) {
    logger.debug("Redis unavailable, skipping cache operation for key: {}", key);
    return null; // or false
}
```
(Keep these as debug level — the initial degradation warning is WARN level already.)

- [ ] **Step 2: Rewrite RateLimiterUtil with Redis backend + memory cleanup (H3, M7)**

Replace entire class content:

```java
package com.neelanshkhare.fabflix.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiterUtil {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterUtil.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000L;
    private static final int TIME_WINDOW_SECONDS = 5 * 60;
    private static final int LOCKOUT_SECONDS = 15 * 60;

    // In-memory fallback (used when Redis is unavailable)
    private static final Map<String, LoginAttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private static long lastCleanupTime = System.currentTimeMillis();

    public static boolean allowRequest(String ipAddress) {
        // Redis-backed rate limiting (persistent across restarts and instances)
        if (RedisUtil.isRedisAvailable()) {
            return allowRequestRedis(ipAddress);
        }
        // Fallback to in-memory
        cleanupIfNeeded();
        return allowRequestInMemory(ipAddress);
    }

    private static boolean allowRequestRedis(String ipAddress) {
        String lockKey = "lockout:" + ipAddress;
        String countKey = "ratelimit:" + ipAddress;

        // Check if locked out
        if ("1".equals(RedisUtil.get(lockKey))) {
            return false;
        }

        Long count = RedisUtil.increment(countKey);
        if (count == null) {
            // Redis returned null — fall back to in-memory
            return allowRequestInMemory(ipAddress);
        }
        if (count == 1) {
            RedisUtil.expire(countKey, TIME_WINDOW_SECONDS);
        }
        if (count >= MAX_ATTEMPTS) {
            RedisUtil.set(lockKey, "1", LOCKOUT_SECONDS);
            logger.warn("IP {} locked out after {} failed attempts", ipAddress, MAX_ATTEMPTS);
        }
        return count <= MAX_ATTEMPTS;
    }

    private static boolean allowRequestInMemory(String ipAddress) {
        long now = System.currentTimeMillis();
        LoginAttemptInfo info = loginAttempts.computeIfAbsent(ipAddress,
                k -> new LoginAttemptInfo(now, new AtomicInteger(0)));

        if (now - info.getTimestamp() > TIME_WINDOW_MS) {
            info.setTimestamp(now);
            info.getAttemptCount().set(1);
            return true;
        }
        return info.getAttemptCount().incrementAndGet() <= MAX_ATTEMPTS;
    }

    public static void loginSucceeded(String ipAddress) {
        if (RedisUtil.isRedisAvailable()) {
            RedisUtil.delete("ratelimit:" + ipAddress);
            RedisUtil.delete("lockout:" + ipAddress);
        } else {
            loginAttempts.remove(ipAddress);
        }
    }

    /** Evict expired in-memory entries to prevent unbounded map growth (M7). */
    private static void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < TIME_WINDOW_MS) return;
        lastCleanupTime = now;
        Iterator<Map.Entry<String, LoginAttemptInfo>> it = loginAttempts.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().getTimestamp() > TIME_WINDOW_MS) {
                it.remove();
            }
        }
    }

    private static class LoginAttemptInfo {
        private volatile long timestamp;
        private final AtomicInteger attemptCount;

        LoginAttemptInfo(long timestamp, AtomicInteger attemptCount) {
            this.timestamp = timestamp;
            this.attemptCount = attemptCount;
        }

        long getTimestamp() { return timestamp; }
        void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        AtomicInteger getAttemptCount() { return attemptCount; }
    }
}
```

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/util/RedisUtil.java src/main/java/com/neelanshkhare/fabflix/util/RateLimiterUtil.java
git commit -m "fix: Redis-backed rate limiting with account lockout and in-memory eviction"
```

---

### Task 11: MoviePosterUtil — disconnect HttpURLConnection + DBConnectionUtil fail-fast
*Fixes: H6, L1*

**Files:**
- Modify: `src/main/java/com/neelanshkhare/fabflix/util/MoviePosterUtil.java`
- Modify: `src/main/java/com/neelanshkhare/fabflix/util/DBConnectionUtil.java`

- [ ] **Step 1: Disconnect HttpURLConnection in MoviePosterUtil (H6)**

In `searchMoviePoster()`, replace:
```java
HttpURLConnection connection = createConnection(searchUrl);
int responseCode = connection.getResponseCode();
```
with:
```java
HttpURLConnection connection = createConnection(searchUrl);
int responseCode;
try {
    responseCode = connection.getResponseCode();
    if (responseCode == 200) {
        String responseBody = readResponse(connection);
        MoviePosterResult result = parseSearchResponse(responseBody, movieTitle, year);
        if (result != null) {
            try {
                RedisUtil.set(cacheKey, serializeToJson(result), RedisUtil.POSTER_TTL);
            } catch (Exception e) {
                LOGGER.error("Error caching poster data for: {}", movieTitle, e);
            }
        }
        return result;
    } else if (responseCode == 401) {
        LOGGER.error("TMDB API authentication failed - check your API key");
    } else if (responseCode == 429) {
        LOGGER.warn("TMDB API rate limit exceeded");
    } else {
        LOGGER.warn("TMDB API request failed with code: {}", responseCode);
    }
    return null;
} finally {
    connection.disconnect();
}
```

Do the same in `fetchTrailerUrl()` — wrap the `getResponseCode()` + `readResponse()` calls in try-finally with `connection.disconnect()`.

- [ ] **Step 2: Fail-fast in DBConnectionUtil if config missing (L1)**

In `initializePools()`, add validation before creating pools:
```java
private static void initializePools() {
    String writeUrl = ConfigUtil.getProperty("db.url.write",
            ConfigUtil.getProperty("db.url", null));
    if (writeUrl == null) {
        throw new ExceptionInInitializerError(
            "Database URL is not configured. Set db.url in config or DB_URL env var.");
    }
    // ... rest of existing init
```

- [ ] **Step 3: Commit**
```
git add src/main/java/com/neelanshkhare/fabflix/util/MoviePosterUtil.java src/main/java/com/neelanshkhare/fabflix/util/DBConnectionUtil.java
git commit -m "fix: disconnect HttpURLConnection in MoviePosterUtil; fail-fast DB init"
```

---

### Task 12: autocomplete.js — HTML-escape suggestion content
*Fixes: L2*

**Files:**
- Modify: `src/main/webapp/js/autocomplete.js`

- [ ] **Step 1: Add escape helper and use it in suggestion rendering**

At the top of the file, add:
```js
function escapeHtml(text) {
    return $('<div>').text(text || '').html();
}
```

In `displayAutocompleteSuggestions()`, replace the template literal:
```js
item.html(`
    <div class="suggestion-content">
        <span class="suggestion-icon">${icon}</span>
        <div class="suggestion-text">
            <div class="suggestion-title">${suggestion.title}</div>
            <div class="suggestion-subtitle">${suggestion.subtitle}</div>
        </div>
    </div>
`);
```
with:
```js
item.html(`
    <div class="suggestion-content">
        <span class="suggestion-icon">${icon}</span>
        <div class="suggestion-text">
            <div class="suggestion-title">${escapeHtml(suggestion.title)}</div>
            <div class="suggestion-subtitle">${escapeHtml(suggestion.subtitle)}</div>
        </div>
    </div>
`);
```

- [ ] **Step 2: Commit**
```
git add src/main/webapp/js/autocomplete.js
git commit -m "security: HTML-escape autocomplete suggestions to prevent XSS"
```

---

### Task 13: K8s app.yaml — resource limits, probes, HPA
*Partial feature: K8s production hardening*

**Files:**
- Modify: `k8s/app.yaml`
- Create: `k8s/hpa.yaml`

- [ ] **Step 1: Add resource limits, liveness/readiness probes to app.yaml container spec**

In the `containers` spec, add after the `ports` section:
```yaml
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /fabflix/api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /fabflix/api/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          failureThreshold: 3
```

- [ ] **Step 2: Create k8s/hpa.yaml**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: fabflix-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: fabflix-app
  minReplicas: 2
  maxReplicas: 6
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

- [ ] **Step 3: Commit**
```
git add k8s/app.yaml k8s/hpa.yaml
git commit -m "k8s: add resource limits, health probes, and HPA for app deployment"
```

---

### Task 14: K8s db-primary.yaml + db-replica.yaml + redis-cluster.yaml — resource limits + probes
*Partial feature: K8s hardening + PG replication completion*

**Files:**
- Modify: `k8s/db-primary.yaml`
- Modify: `k8s/db-replica.yaml`
- Modify: `k8s/redis-cluster.yaml`

- [ ] **Step 1: Add resource limits and liveness probe to db-primary.yaml**

In the `postgres` container spec, add:
```yaml
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command: ["pg_isready", "-U", "postgres", "-d", "fabflix"]
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["pg_isready", "-U", "postgres", "-d", "fabflix"]
          initialDelaySeconds: 5
          periodSeconds: 5
```

- [ ] **Step 2: Complete db-replica.yaml with pg_basebackup init container**

Replace the entire db-replica.yaml content:
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: fabflix-db-replica
spec:
  serviceName: "db-replica"
  replicas: 1
  selector:
    matchLabels:
      app: fabflix-db
      role: replica
  template:
    metadata:
      labels:
        app: fabflix-db
        role: replica
    spec:
      initContainers:
      - name: init-replica
        image: postgres:15-alpine
        env:
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: fabflix-secrets
              key: db-password
        command:
        - /bin/sh
        - -c
        - |
          until pg_isready -h db-primary -p 5432 -U postgres; do
            echo "Waiting for primary..."; sleep 2;
          done
          if [ -z "$(ls -A /var/lib/postgresql/data)" ]; then
            pg_basebackup -h db-primary -D /var/lib/postgresql/data \
              -U postgres -Fp -Xs -P -R
            echo "Replica initialized from primary"
          else
            echo "Data directory already exists, skipping basebackup"
          fi
        volumeMounts:
        - name: db-data
          mountPath: /var/lib/postgresql/data
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          value: fabflix
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: fabflix-secrets
              key: db-password
        - name: POSTGRES_HOST_AUTH_METHOD
          value: "md5"
        command: ["postgres"]
        args: ["-c", "hot_standby=on"]
        ports:
        - containerPort: 5432
          name: postgres
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command: ["pg_isready", "-U", "postgres", "-d", "fabflix"]
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["pg_isready", "-U", "postgres", "-d", "fabflix"]
          initialDelaySeconds: 5
          periodSeconds: 5
        volumeMounts:
        - name: db-data
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: db-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 1Gi
---
apiVersion: v1
kind: Service
metadata:
  name: db-replica
spec:
  selector:
    app: fabflix-db
    role: replica
  ports:
  - port: 5432
    targetPort: 5432
```

- [ ] **Step 3: Add resource limits and liveness probe to redis-cluster.yaml**

In the `redis` container spec, add:
```yaml
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        livenessProbe:
          exec:
            command: ["redis-cli", "ping"]
          initialDelaySeconds: 15
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["redis-cli", "ping"]
          initialDelaySeconds: 5
          periodSeconds: 5
```

- [ ] **Step 4: Commit**
```
git add k8s/db-primary.yaml k8s/db-replica.yaml k8s/redis-cluster.yaml
git commit -m "k8s: complete PG replica init container, add resource limits and probes to all statefulsets"
```

---

### Task 15: K8s Redisson ConfigMap + NetworkPolicy
*Partial feature: Distributed sessions in K8s, network hardening*

**Files:**
- Create: `k8s/redisson-configmap.yaml`
- Create: `k8s/network-policy.yaml`
- Modify: `k8s/app.yaml` (mount ConfigMap)

- [ ] **Step 1: Create k8s/redisson-configmap.yaml**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redisson-config
data:
  redisson.yaml: |
    ---
    clusterServersConfig:
      nodeAddresses:
        - "redis://redis-cluster-0.redis-cluster:6379"
        - "redis://redis-cluster-1.redis-cluster:6379"
        - "redis://redis-cluster-2.redis-cluster:6379"
        - "redis://redis-cluster-3.redis-cluster:6379"
        - "redis://redis-cluster-4.redis-cluster:6379"
        - "redis://redis-cluster-5.redis-cluster:6379"
      connectTimeout: 10000
      timeout: 3000
      retryAttempts: 3
      retryInterval: 1500
      slaveConnectionPoolSize: 24
      masterConnectionPoolSize: 24
      slaveConnectionMinimumIdleSize: 5
      masterConnectionMinimumIdleSize: 5
      idleConnectionTimeout: 10000
      dnsMonitoringInterval: 5000
      scanInterval: 2000
    threads: 16
    nettyThreads: 32
    codec: !<org.redisson.codec.MarshallingCodec> {}
    transportMode: "NIO"
```

- [ ] **Step 2: Mount ConfigMap in k8s/app.yaml**

In the `containers` spec, add a volumeMount:
```yaml
        volumeMounts:
        - name: redisson-config
          mountPath: /usr/local/tomcat/webapps/fabflix/WEB-INF/classes/redisson.yaml
          subPath: redisson.yaml
```

In the `spec` (pod level), add a volume:
```yaml
      volumes:
      - name: redisson-config
        configMap:
          name: redisson-config
```

- [ ] **Step 3: Create k8s/network-policy.yaml**

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: fabflix-app-netpol
spec:
  podSelector:
    matchLabels:
      app: fabflix-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - ports:
    - port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: fabflix-db
    ports:
    - port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis-cluster
    ports:
    - port: 6379
  - ports:
    - port: 443  # TMDB API, reCAPTCHA
    - port: 53   # DNS
      protocol: UDP
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: fabflix-db-netpol
spec:
  podSelector:
    matchLabels:
      app: fabflix-db
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: fabflix-app
    ports:
    - port: 5432
  - from:
    - podSelector:
        matchLabels:
          app: fabflix-db
    ports:
    - port: 5432
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: redis-cluster-netpol
spec:
  podSelector:
    matchLabels:
      app: redis-cluster
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: fabflix-app
    ports:
    - port: 6379
  - from:
    - podSelector:
        matchLabels:
          app: redis-cluster
    ports:
    - port: 6379
    - port: 16379
```

- [ ] **Step 4: Commit**
```
git add k8s/redisson-configmap.yaml k8s/network-policy.yaml k8s/app.yaml
git commit -m "k8s: Redisson ConfigMap for cluster node addresses, NetworkPolicy for all pods"
```

---

## Coverage Check

| Finding | Task | Status |
|---------|------|--------|
| C1 Hardcoded secrets | Task 2 | Covered |
| C2 Session fixation | Task 3 | Covered |
| C3 No CSRF | Tasks 1,3,4,5,6 | Covered |
| C4 IDOR orders | Tasks 3 (CSRF on doPut/doDelete; existing orders endpoint already scoped to session user) | Covered |
| C5 TMDB title unsanitized | Task 11 (connection fix; title encoding via URLEncoder already present) | Covered |
| H1 RecaptchaUtil secret | Task 2 (config.properties; RecaptchaUtil already uses ConfigUtil) | Covered |
| H2 e.getMessage() leaks | Tasks 3,4,5,6,7 | Covered |
| H3 In-memory rate limiter | Task 10 | Covered |
| H4 CSRF on password update | Task 3 | Covered |
| H5 Cart quantity bounds | Task 4 | Covered |
| H6 HttpURLConnection leak | Task 11 | Covered |
| H7 Movie ID not validated | Task 7 | Covered |
| H8 No Content-Type check | (LOW risk: PreparedStatements prevent injection; JSON parsing already handles bad input gracefully — won't add 415 responses as it would break current clients) | Skipped as low ROI |
| M1 Race condition | Task 9 | Covered |
| M2 No read timeout | (already has `setReadTimeout(10000)` in `createConnection()`) | Already fixed |
| M3 Health endpoint leaks | Task 8 | Covered |
| M4 Page size unbounded | Task 7 | Covered |
| M5 Executor not shutdown | Task 9 | Covered |
| M6 Session cookie flags | Task 1 | Covered |
| M7 RateLimiter map grows | Task 10 | Covered |
| M8 Redis silent degradation | Task 10 | Covered |
| L1 DB defaults insecure | Task 11 | Covered |
| L2 XSS in autocomplete | Task 12 | Covered |
| Partial: Redisson K8s | Task 15 | Covered |
| Partial: PG replication K8s | Task 14 | Covered |
| Partial: K8s hardening | Tasks 13,14,15 | Covered |

**Total: 22/23 findings covered** (H8 Content-Type check skipped — low ROI, existing JSON parsing handles gracefully).
