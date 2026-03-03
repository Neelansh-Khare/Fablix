# Redis Integration Setup Guide

## Quick Start

### Option 1: Docker (Recommended)

1. **Start Docker Desktop** on your Windows machine

2. **Start Redis using Docker Compose:**
   ```bash
   docker-compose up -d redis
   ```

3. **Verify Redis is running:**
   ```bash
   docker ps | findstr redis
   ```

4. **Test Redis connection:**
   ```bash
   docker exec -it fabflix-redis redis-cli ping
   ```
   You should see `PONG`

### Option 2: Local Redis Installation

If you prefer not to use Docker:

1. Download Redis for Windows from: https://github.com/microsoftarchive/redis/releases
2. Install and start Redis service
3. Redis will run on `localhost:6379` by default

## Configuration

Redis connection settings can be configured via environment variables:

- `REDIS_HOST` - Redis server host (default: `localhost`)
- `REDIS_PORT` - Redis server port (default: `6379`)
- `REDIS_TIMEOUT` - Connection timeout in ms (default: `2000`)
- `REDIS_PASSWORD` - Redis password (optional, leave empty for no password)

## What's Cached in Redis?

### 1. Movie Poster Data
- **Key Pattern:** `movie_poster:{movieTitle}:{year}`
- **TTL:** 7 days
- **Purpose:** Cache TMDB API responses to reduce API calls and improve performance

### 2. Autocomplete Results
- **Key Pattern:** `autocomplete:{query}:{limit}`
- **TTL:** 1 day
- **Purpose:** Cache search autocomplete results to reduce database load

### 3. Session Management (Optional)
- **Purpose:** Distributed session storage for horizontal scaling (configured via Redisson)

## Monitoring Redis

### Check Redis Stats
```bash
docker exec -it fabflix-redis redis-cli INFO stats
```

### View All Keys
```bash
docker exec -it fabflix-redis redis-cli KEYS "*"
```

### Get a Specific Value
```bash
docker exec -it fabflix-redis redis-cli GET "movie_poster:The Matrix:1999"
```

### Clear All Cache (Development Only)
```bash
docker exec -it fabflix-redis redis-cli FLUSHDB
```

## Application Behavior

- **Graceful Degradation:** If Redis is unavailable, the application will continue to work normally, but without caching
- **Automatic Retry:** Redis connections are pooled and managed by HikariCP-style connection pooling
- **Logging:** Check application logs for Redis connection status and cache hit/miss statistics

## Stopping Redis

```bash
docker-compose down
```

## Performance Benefits

With Redis caching enabled:
- **TMDB API calls reduced by ~90%** for repeat requests
- **Faster page loads** for movie details and search results
- **Reduced external API costs** and rate limiting issues
- **Scalability:** Shared cache across multiple application instances (when deployed)

## Troubleshooting

### Redis connection errors
- Ensure Docker Desktop is running
- Check if Redis container is up: `docker ps`
- Restart Redis: `docker-compose restart redis`

### Cache not working
- Check application logs for Redis connection errors
- Verify environment variables if using custom Redis settings
- Test Redis connection: `docker exec -it fabflix-redis redis-cli ping`

## Next Steps

After Redis is running:
1. Start your Tomcat server
2. Access any movie page - first load will cache poster data
3. Refresh the page - subsequent loads will use cached data (check logs for "Cache HIT")
4. Monitor Redis keys: `docker exec -it fabflix-redis redis-cli KEYS "*"`
