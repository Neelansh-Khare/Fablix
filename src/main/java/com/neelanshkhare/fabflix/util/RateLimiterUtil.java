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

    // In-memory fallback used when Redis is unavailable
    private static final Map<String, LoginAttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private static volatile long lastCleanupTime = System.currentTimeMillis();

    public static boolean allowRequest(String ipAddress) {
        if (RedisUtil.isRedisAvailable()) {
            return allowRequestRedis(ipAddress);
        }
        cleanupIfNeeded();
        return allowRequestInMemory(ipAddress);
    }

    private static boolean allowRequestRedis(String ipAddress) {
        String lockKey = "lockout:" + ipAddress;
        String countKey = "ratelimit:" + ipAddress;

        // Check lockout first
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
            logger.warn("IP {} locked out after {} failed login attempts", ipAddress, MAX_ATTEMPTS);
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

    /** Evict expired in-memory entries to prevent unbounded map growth. */
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
        void setTimestamp(long t) { this.timestamp = t; }
        AtomicInteger getAttemptCount() { return attemptCount; }
    }
}
