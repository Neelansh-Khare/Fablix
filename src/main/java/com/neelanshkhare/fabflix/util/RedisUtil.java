package com.neelanshkhare.fabflix.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis connection and caching utility for FabFlix
 * Implements JedisCluster for high availability and scalability
 */
public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private static JedisCluster jedisCluster;
    private static boolean redisEnabled = true;

    // Redis configuration from environment or defaults
    private static final String REDIS_CLUSTER_NODES = System.getenv().getOrDefault("REDIS_CLUSTER_NODES", "localhost:6379");
    private static final int REDIS_TIMEOUT = Integer.parseInt(System.getenv().getOrDefault("REDIS_TIMEOUT", "2000"));
    private static final int MAX_ATTEMPTS = 5;
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
        initializeCluster();
    }

    /**
     * Initialize the JedisCluster
     */
    private static void initializeCluster() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(50);
            poolConfig.setMaxIdle(20);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);

            Set<HostAndPort> clusterNodes = new HashSet<>();
            String[] nodes = REDIS_CLUSTER_NODES.split(",");
            for (String node : nodes) {
                String[] parts = node.split(":");
                clusterNodes.add(new HostAndPort(parts[0], Integer.parseInt(parts[1])));
            }

            if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
                jedisCluster = new JedisCluster(clusterNodes, REDIS_TIMEOUT, REDIS_TIMEOUT, MAX_ATTEMPTS, REDIS_PASSWORD, poolConfig);
            } else {
                jedisCluster = new JedisCluster(clusterNodes, REDIS_TIMEOUT, REDIS_TIMEOUT, MAX_ATTEMPTS, poolConfig);
            }

            // Test connection
            String testKey = "cluster_test_key";
            jedisCluster.set(testKey, "working");
            jedisCluster.del(testKey);
            logger.info("Redis Cluster initialized successfully with nodes: {}", REDIS_CLUSTER_NODES);
        } catch (Exception e) {
            logger.warn("Failed to initialize Redis Cluster. Redis caching disabled. Error: {}", e.getMessage());
            redisEnabled = false;
            if (jedisCluster != null) {
                try { jedisCluster.close(); } catch (Exception ignore) {}
                jedisCluster = null;
            }
        }
    }

    /**
     * Check if Redis is enabled and available
     */
    public static boolean isRedisAvailable() {
        return redisEnabled && jedisCluster != null;
    }

    /**
     * Get a value from Redis cache
     */
    public static String get(String key) {
        if (!isRedisAvailable()) {
            return null;
        }
        try {
            return jedisCluster.get(key);
        } catch (Exception e) {
            logger.error("Error getting key from Redis Cluster: {}", key, e);
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
        try {
            jedisCluster.setex(key, ttlSeconds, value);
            return true;
        } catch (Exception e) {
            logger.error("Error setting key in Redis Cluster: {}", key, e);
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
        try {
            jedisCluster.set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Error setting key in Redis Cluster: {}", key, e);
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
        try {
            jedisCluster.del(key);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting key from Redis Cluster: {}", key, e);
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
        try {
            return jedisCluster.exists(key);
        } catch (Exception e) {
            logger.error("Error checking key existence in Redis Cluster: {}", key, e);
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
        try {
            return jedisCluster.incr(key);
        } catch (Exception e) {
            logger.error("Error incrementing key in Redis Cluster: {}", key, e);
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
        try {
            return jedisCluster.zincrby(key, score, member);
        } catch (Exception e) {
            logger.error("Error incrementing sorted set member in Redis Cluster: {}", key, e);
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
        try {
            Set<String> result = jedisCluster.zrevrange(key, start, stop);
            return result != null ? result.stream().collect(Collectors.toList()) : null;
        } catch (Exception e) {
            logger.error("Error getting zrevrange from Redis Cluster: {}", key, e);
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
        try {
            jedisCluster.expire(key, ttlSeconds);
            return true;
        } catch (Exception e) {
            logger.error("Error setting expiration for key in Redis Cluster: {}", key, e);
            return false;
        }
    }

    /**
     * Close the JedisCluster (should be called on application shutdown)
     */
    public static void shutdown() {
        if (jedisCluster != null) {
            try {
                jedisCluster.close();
                logger.info("Redis Cluster connection closed");
            } catch (Exception e) {
                logger.error("Error closing Redis Cluster connection", e);
            }
        }
    }

    /**
     * Clear all keys matching a pattern (use carefully in Cluster!)
     */
    public static boolean clearPattern(String pattern) {
        if (!isRedisAvailable()) {
            return false;
        }
        try {
            // In a cluster, we need to iterate over all master nodes to find keys
            Set<String> allKeys = new HashSet<>();
            jedisCluster.getClusterNodes().values().forEach(pool -> {
                try (var jedis = pool.getResource()) {
                    allKeys.addAll(jedis.keys(pattern));
                } catch (Exception ignore) {}
            });

            if (!allKeys.isEmpty()) {
                jedisCluster.del(allKeys.toArray(new String[0]));
                logger.info("Cleared {} keys matching pattern: {} from Cluster", allKeys.size(), pattern);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error clearing pattern in Redis Cluster: {}", pattern, e);
            return false;
        }
    }

    /**
     * Get JedisCluster instance (for advanced operations)
     * Note: Use with caution as it's cluster-wide
     */
    public static JedisCluster getCluster() {
        return jedisCluster;
    }
}
