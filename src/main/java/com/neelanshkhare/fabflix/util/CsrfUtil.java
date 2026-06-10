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
