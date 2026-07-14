# FabFlix: Current Setup vs. a Spring Boot Rewrite

This doc explains how FabFlix is actually built today, clears up a common point of
confusion ("isn't Maven a framework?"), and then walks through what the same
application would look like if it were rebuilt on Spring Boot. It ends with a
concrete recommendation.

## TL;DR

- **Maven is already in use** — it's the build tool (`pom.xml`, dependency
  management, WAR packaging). FabFlix is not "missing Maven"; it's missing a
  *framework* layered on top of it.
- **The framework layer is plain Jakarta/`javax.servlet`** — `HttpServlet`,
  `Filter`, `ServletContextListener`, JSP, manual JDBC via HikariCP. This is
  the Java EE / "just servlets" style that predates Spring's dominance.
- **Spring Boot would replace the framework layer, not the build tool.** You'd
  still very plausibly use Maven (or Gradle) underneath Spring Boot — Spring
  Boot ships a Maven plugin (`spring-boot-maven-plugin`) for exactly this. The
  comparison people usually mean is really **"raw Servlets vs. Spring Boot,"**
  with Maven present in both cases.

## 1. Maven's actual role (present in both scenarios)

Maven is a **build tool / dependency manager**, not an application framework.
Today it already does everything a build tool is responsible for in this repo:

```xml
<!-- pom.xml -->
<packaging>war</packaging>
<dependencies>
  <dependency><groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId></dependency>
  <dependency><groupId>com.zaxxer</groupId><artifactId>HikariCP</artifactId></dependency>
  <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
  <!-- ... -->
</dependencies>
<build>
  <plugins>
    <plugin><artifactId>maven-war-plugin</artifactId></plugin>
    <plugin><artifactId>tomcat7-maven-plugin</artifactId></plugin>
  </plugins>
</build>
```

`mvn clean package` compiles the code, resolves dependencies from Maven
Central, and produces `target/fabflix.war` — which `Dockerfile` then copies
into a Tomcat image:

```dockerfile
FROM maven:3.8.4-openjdk-17-slim AS build
RUN mvn package -DskipTests
FROM tomcat:9.0-jdk17-openjdk-slim
COPY --from=build /app/target/fabflix.war /usr/local/tomcat/webapps/fabflix.war
```

If FabFlix moved to Spring Boot, this part of the picture barely changes: you'd
still have a `pom.xml`, still run `mvn package`, and Maven would still resolve
dependencies. The differences below are entirely about what's *built on top of*
Maven.

## 2. What's actually different: Servlets vs. Spring Boot

| Concern | FabFlix today (Servlet API) | With Spring Boot |
|---|---|---|
| HTTP entry point | `@WebServlet` + `HttpServlet` subclass, one class per resource, manual `doGet`/`doPost`/`doPut`/`doDelete` | `@RestController` with `@GetMapping`/`@PostMapping` methods; routing declared once, dispatched by `DispatcherServlet` |
| Request/response body | Manually read `BufferedReader`, manually build `org.json.JSONObject`, manually `response.getWriter().print(...)` | Method params/return types auto-(de)serialized to/from JSON via Jackson |
| Dependency wiring | `new GenreService()` inside `init()`; manual `new` chains through DAO → service → servlet | Constructor injection via `@Autowired`/`@Service`/`@Repository`; Spring's IoC container wires the graph |
| Data access | Hand-written JDBC in `*DAOImpl` classes against a `HikariDataSource` obtained from a static `DBConnectionUtil` | Spring Data JPA/JDBC repositories (`interface MovieRepository extends JpaRepository<Movie, Integer>`) or `JdbcTemplate` — HikariCP is still the connection pool, just auto-configured |
| Connection pool setup | 100 lines in `DBConnectionUtil.java`: manual `HikariConfig`, manual read/write pool split, manual property lookups | `application.yml` properties (`spring.datasource.*`); Spring Boot auto-configures a `HikariDataSource` bean from them |
| Config | `ConfigUtil.getProperty(...)` reading a custom properties file/env var scheme | `application.yml`/`.properties` + `@ConfigurationProperties`, environment-specific `application-{profile}.yml`, `@Profile` |
| Auth/authorization | `AdminFilter implements Filter`, manually checks `HttpSession` attributes, manually returns 403/redirect | Spring Security: `SecurityFilterChain` bean, `@PreAuthorize`, session or JWT config declared once |
| App lifecycle hooks | `PosterPopulationListener`, `RedisShutdownListener` implementing `ServletContextListener` | `ApplicationRunner`/`CommandLineRunner` beans, `@EventListener(ContextClosedEvent.class)` |
| Error pages | `web.xml` `<error-page>` entries pointing at JSP | `@ControllerAdvice` + `@ExceptionHandler`, or `ErrorController` |
| Views | JSP (`index.jsp`, `_dashboard.jsp`) rendered by Tomcat's Jasper engine | Thymeleaf/JSP (still supported) — or, more commonly today, the JSPs become a separate SPA (React/Vue) hitting the REST API, since FabFlix's servlets are already API-shaped (`/api/genres/*`, JSON in/out) |
| Health checks | `HealthServlet` (hand-rolled) | Spring Boot Actuator (`/actuator/health`) out of the box |
| Server | External Tomcat 9, app deployed as a `.war` dropped into `webapps/` | Embedded Tomcat/Jetty/Undertow packaged **inside** an executable `.jar` (`java -jar fabflix.jar`); `.war` deployment to an external container is still optional but no longer necessary |
| Redis session clustering | `redisson-tomcat-9` hooked into Tomcat's session manager via `context.xml` + jars manually copied to `$CATALINA_HOME/lib` in the Dockerfile | Spring Session Data Redis (`spring-session-data-redis`) — a dependency + a couple of properties, no manual jar surgery |

## 3. Same endpoint, both ways

**Today — `GenreServlet.java`** (`src/main/java/.../servlet/GenreServlet.java`),
~250 lines to hand-roll GET-by-id, GET-all, POST, PUT, DELETE with manual JSON:

```java
@WebServlet("/api/genres/*")
public class GenreServlet extends HttpServlet {
    private GenreService genreService;

    @Override
    public void init() throws ServletException {
        genreService = new GenreService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Genre> genres = genreService.getAllGenres();
                JSONArray genresArray = new JSONArray();
                for (Genre genre : genres) {
                    JSONObject genreObj = new JSONObject();
                    genreObj.put("id", genre.getId());
                    genreObj.put("name", genre.getName());
                    genresArray.put(genreObj);
                }
                JSONObject result = new JSONObject();
                result.put("genres", genresArray);
                out.print(result.toString());
            } else {
                // ...parse id, look up, 404 if missing, catch NumberFormatException...
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // ...
        }
    }
    // doPost, doPut, doDelete repeat this same boilerplate shape
}
```

**Equivalent with Spring Boot** — routing, serialization, and error mapping
are declarative; the DTO shape is defined once as a `record`:

```java
@RestController
@RequestMapping("/api/genres")
public class GenreController {
    private final GenreService genreService;

    public GenreController(GenreService genreService) { // constructor injection
        this.genreService = genreService;
    }

    @GetMapping
    public Map<String, List<Genre>> getAllGenres() {
        return Map.of("genres", genreService.getAllGenres());
    }

    @GetMapping("/{id}")
    public Genre getGenre(@PathVariable int id) {
        return genreService.getGenre(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Genre createGenre(@RequestBody Genre genre) {
        return genreService.addGenre(genre);
    }
}
```

Jackson handles JSON (de)serialization automatically; `ResponseStatusException`
and a global `@ControllerAdvice` handle the error-response shape once instead
of in every `catch` block; the `NumberFormatException` on a bad path variable
is handled by Spring's argument binding instead of by hand.

## 4. Same idea, the auth filter

**Today — `AdminFilter.java`:**

```java
@WebFilter(filterName = "AdminFilter", urlPatterns = {"/api/admin/*", "/_dashboard"})
public class AdminFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpSession session = ((HttpServletRequest) request).getSession(false);
        boolean isLoggedIn = session != null && session.getAttribute("customerId") != null;
        String role = isLoggedIn ? (String) session.getAttribute("customerRole") : null;
        if (isLoggedIn && "admin".equalsIgnoreCase(role)) {
            chain.doFilter(request, response);
        } else {
            // manually decide JSON 403 vs. redirect based on the URI prefix
        }
    }
}
```

**With Spring Security:**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**", "/_dashboard").hasRole("ADMIN")
            .anyRequest().permitAll())
        .formLogin(Customizer.withDefaults())
        .build();
}
```

The role check, session handling, and the 403-for-API-vs-redirect-for-page
split are all standard Spring Security behavior instead of custom code that
someone has to maintain and re-test.

## 5. What genuinely gets better with Spring Boot

- **Less boilerplate.** The DAO → Service → Servlet chain in FabFlix
  (`MovieDAO`/`MovieDAOImpl`/`MovieService`/`MovieServlet`, repeated per
  resource) largely collapses into `@Repository`/`@Service`/`@RestController`
  with dependency injection doing the wiring `init()` does manually today.
- **Config is centralized and environment-aware.** `application-dev.yml` /
  `application-prod.yml` + `@Profile` replaces `ConfigUtil` + hand-rolled
  environment variable fallbacks scattered through `DBConnectionUtil`,
  `RedisUtil`, `RecaptchaUtil`, etc.
- **Security is a library, not hand-written filters.** CSRF (`CsrfUtil.java`
  today), password checks, and role-based access become configuration instead
  of code to audit for bugs.
- **Actuator gives health/metrics for free**, replacing the custom
  `HealthServlet`/`DebugServlet`.
- **Testability improves.** Constructor-injected services are trivial to
  mock in unit tests; static-method-heavy classes like `DBConnectionUtil` and
  service classes that `new` their own DAOs are harder to test in isolation.
- **Executable jar simplifies deployment** — no external Tomcat to manage,
  no manual jar-copying step like the Redisson workaround currently baked
  into the `Dockerfile` (lines 32–37).

## 6. What gets worse / what's a wash

- **Migration cost is real.** ~55 Java files, 5 DAOs, 14 servlets, 2 filters/
  listeners, and all the JSP views would need rewriting, not just relabeling —
  this isn't a drop-in dependency swap.
- **More "magic."** Annotation-driven DI and auto-configuration are less
  visible than the current explicit `new` chains — new contributors need to
  learn Spring's conventions, not just Java.
- **Heavier runtime footprint** — a Spring context boot adds startup time and
  memory versus a bare servlet container, though this rarely matters at
  FabFlix's scale.
- **Docker image changes shape** but isn't simpler on day one: you trade the
  Tomcat-base-image + WAR-copy pattern for a JDK-base-image +
  `java -jar` pattern, and you'd need to re-verify the HTTPS keystore and
  Redis session clustering setup (`server.xml`, `redisson-tomcat-9`) against
  Spring Session's Redis integration instead.
- **JSP is a dead end either way.** It works today and would keep working
  under Spring Boot, but neither path modernizes the frontend — that's a
  separate decision (e.g., extracting a React/Vue SPA against the existing
  `/api/*` endpoints, which are already JSON-shaped and wouldn't need much
  change to serve a SPA instead of JSPs).

## 7. Recommendation

FabFlix's servlets are already organized like a REST API (`/api/genres/*`,
`/api/movies/*`, JSON in and out) — the *shape* of a Spring Boot migration is
natural, it's mostly a mechanical port of existing DAO/Service logic into
`@Repository`/`@Service` beans and existing servlets into `@RestController`s.
The main things worth weighing:

- If the goal is **learning/demonstrating Spring** or this app has room to
  grow (more endpoints, more contributors, need for real auth/security
  hardening), migrating is worth it — the framework buys back a lot of the
  hand-written plumbing (`ConfigUtil`, `DBConnectionUtil`, `CsrfUtil`,
  `AdminFilter`) with tested, maintained equivalents.
- If FabFlix is feature-complete and stable, a full rewrite is hard to
  justify purely on elegance grounds — Maven + plain servlets is a perfectly
  valid, lighter-weight production setup, and the current code, while
  verbose, is not actually broken.
- A middle ground: keep Maven and the WAR/Tomcat deployment, but selectively
  adopt individual libraries the "Spring way" already uses under the hood —
  e.g., Spring Session Data Redis, or Jackson instead of `org.json`+manual
  string building — without pulling in the full framework.

---

**Last Updated:** 2026-07-13
