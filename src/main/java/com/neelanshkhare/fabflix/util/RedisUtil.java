package com.neelanshkhare.fabflix.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Redis connection and caching utility for FabFlix
 * Implements connection pooling and provides helper methods for common Redis operations
 */
public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private static JedisPool jedisPool;
    private static boolean redisEnabled = true;

    // Redis configuration from environment or defaults
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    private static final int REDIS_TIMEOUT = Integer.parseInt(System.getenv().getOrDefault("REDIS_TIMEOUT", "2000"));
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD"); // Optional

    // Cache key prefixes for different data types
    public static final String POSTER_KEY_PREFIX = "movie_poster:";
    public static final String TRAILER_KEY_PREFIX = "movie_trailer:"; // Reserved for future use
    public static final String AUTOCOMPLETE_KEY_PREFIX = "autocomplete:";
    public static final String MOVIE_KEY_PREFIX = "movie:"; // Reserved for future use
    public static final String POPULAR_SEARCHES_KEY = "stats:popular:searches";


    // Default TTL values (in seconds)
    public static final int POSTER_TTL = 7 * 24 * 60 * 60; // 7 days
    public static final int AUTOCOMPLETE_TTL = 24 * 60 * 60; // 1 day
    public static final int MOVIE_TTL = 60 * 60; // 1 hour - Reserved for future use

    static {
        initializePool();
    }

    /**
     * Initialize the Jedis connection pool
     */
    private static void initializePool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(50);
            poolConfig.setMaxIdle(20);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60)); // Note: Method deprecated in newer versions
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);

            if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT, REDIS_PASSWORD);
            } else {
                jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT);
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                logger.info("Redis connection pool initialized successfully at {}:{}", REDIS_HOST, REDIS_PORT);
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize Redis connection pool. Redis caching disabled. Error: {}", e.getMessage());
            redisEnabled = false;
            if (jedisPool != null) {
                jedisPool.close();
                jedisPool = null;
            }
        }
    }

    /**
     * Get a Jedis instance from the pool
     */
    public static Jedis getJedis() {
        if (!redisEnabled || jedisPool == null) {
            return null;
        }
        try {
            return jedisPool.getResource();
        } catch (JedisException e) {
            logger.error("Failed to get Jedis resource from pool", e);
            return null;
        }
    }

    /**
     * Check if Redis is enabled and available
     */
    public static boolean isRedisAvailable() {
        return redisEnabled && jedisPool != null;
    }

    /**
     * Get a value from Redis cache
     */
    public static String get(String key) {
        if (!isRedisAvailable()) {
            return null;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return null;
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting key from Redis: {}", key, e);
            return null;
        }
    }

    /**
     * Set a value in Redis cache with TTL
     */
    public static boolean set(String key, String value, int ttlSeconds) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            jedis.setex(key, ttlSeconds, value);
            return true;
        } catch (Exception e) {
            logger.error("Error setting key in Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Set a value in Redis cache without expiration
     */
    public static boolean set(String key, String value) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Error setting key in Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Delete a key from Redis
     */
    public static boolean delete(String key) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            jedis.del(key);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting key from Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Check if a key exists in Redis
     */
    public static boolean exists(String key) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            return jedis.exists(key);
        } catch (Exception e) {
            logger.error("Error checking key existence in Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Increment a counter in Redis
     */
    public static Long increment(String key) {
        if (!isRedisAvailable()) {
            return null;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return null;
            return jedis.incr(key);
        } catch (Exception e) {
            logger.error("Error incrementing key in Redis: {}", key, e);
            return null;
        }
    }

    /**
     * Increment the score of a member in a sorted set
     */
    public static Double zincrby(String key, double score, String member) {
        if (!isRedisAvailable()) {
            return null;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return null;
            return jedis.zincrby(key, score, member);
        } catch (Exception e) {
            logger.error("Error incrementing sorted set member in Redis: {}", key, e);
            return null;
        }
    }

    /**
     * Get range of members from sorted set (reverse order - highest score first)
     */
    public static List<String> zrevrange(String key, long start, long stop) {
        if (!isRedisAvailable()) {
            return null;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return null;
            return jedis.zrevrange(key, start, stop);
        } catch (Exception e) {
            logger.error("Error getting zrevrange from Redis: {}", key, e);
            return null;
        }
    }

    /**
     * Set expiration time for a key
     */
    public static boolean expire(String key, int ttlSeconds) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            jedis.expire(key, ttlSeconds);
            return true;
        } catch (Exception e) {
            logger.error("Error setting expiration for key in Redis: {}", key, e);
            return false;
        }
    }

    /**
     * Close the Jedis pool (should be called on application shutdown)
     */
    public static void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection pool closed");
        }
    }

    /**
     * Clear all keys matching a pattern (use carefully!)
     */
    public static boolean clearPattern(String pattern) {
        if (!isRedisAvailable()) {
            return false;
        }
        try (Jedis jedis = getJedis()) {
            if (jedis == null) return false;
            var keys = jedis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                logger.info("Cleared {} keys matching pattern: {}", keys.size(), pattern);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error clearing pattern in Redis: {}", pattern, e);
            return false;
        }
    }

    /**
     * Get Redis pool statistics for monitoring
     */
    public static String getPoolStats() {
        if (jedisPool == null) {
            return "Redis pool not initialized";
        }
        return String.format("Active: %d, Idle: %d, Waiters: %d",
                jedisPool.getNumActive(),
                jedisPool.getNumIdle(),
                jedisPool.getNumWaiters());
    }
}
