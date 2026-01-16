package com.neelanshkhare.fabflix.util;

import org.apache.commons.codec.binary.Hex;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecurityUtil {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);

    // Generate a random salt (Legacy, kept for compatibility if needed)
    public static String generateSalt() {
        return BCrypt.gensalt();
    }

    // Hash a password using BCrypt
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Check password using BCrypt
    public static boolean checkPassword(String candidate, String hashed) {
        try {
            return BCrypt.checkpw(candidate, hashed);
        } catch (IllegalArgumentException e) {
            // Might be a legacy SHA-256 hash, handled in DAO or here?
            // For this migration, let's assume we handle legacy in DAO or just fail.
            logger.warn("Invalid BCrypt hash, might be legacy", e);
            return false;
        }
    }

    // Legacy SHA-256 Hashing
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Hex.encodeHexString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password", e);
            return null;
        }
    }
}