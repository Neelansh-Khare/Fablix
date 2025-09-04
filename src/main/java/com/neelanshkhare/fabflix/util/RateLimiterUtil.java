package com.neelanshkhare.fabflix.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiterUtil {
    // Map to store IP addresses and their login attempt counts
    private static final Map<String, LoginAttemptInfo> loginAttempts = new ConcurrentHashMap<>();

    // Maximum allowed attempts in the time window
    private static final int MAX_ATTEMPTS = 5;

    // Time window in milliseconds (5 minutes)
    private static final long TIME_WINDOW = 5 * 60 * 1000;

    public static boolean allowRequest(String ipAddress) {
        long currentTime = System.currentTimeMillis();

        // Get or create the login attempt info for this IP
        LoginAttemptInfo info = loginAttempts.computeIfAbsent(ipAddress,
                k -> new LoginAttemptInfo(currentTime, new AtomicInteger(0)));

        // If the time window has passed, reset the counter
        if (currentTime - info.getTimestamp() > TIME_WINDOW) {
            info.setTimestamp(currentTime);
            info.getAttemptCount().set(1);
            return true;
        }

        // Increment the counter
        int attemptCount = info.getAttemptCount().incrementAndGet();

        // Check if the attempt count exceeds the max allowed
        return attemptCount <= MAX_ATTEMPTS;
    }

    public static void loginSucceeded(String ipAddress) {
        // Reset the counter on successful login
        loginAttempts.remove(ipAddress);
    }

    // Inner class to store login attempt information
    private static class LoginAttemptInfo {
        private long timestamp;
        private AtomicInteger attemptCount;

        public LoginAttemptInfo(long timestamp, AtomicInteger attemptCount) {
            this.timestamp = timestamp;
            this.attemptCount = attemptCount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public AtomicInteger getAttemptCount() {
            return attemptCount;
        }
    }
}