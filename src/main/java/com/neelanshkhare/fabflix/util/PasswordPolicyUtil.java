package com.neelanshkhare.fabflix.util;

import java.util.regex.Pattern;

public class PasswordPolicyUtil {

    // Minimum password length
    private static final int MIN_LENGTH = 8;

    // Regular expressions for different character classes
    private static final String LOWERCASE_REGEX = ".*[a-z].*";
    private static final String UPPERCASE_REGEX = ".*[A-Z].*";
    private static final String DIGIT_REGEX = ".*\\d.*";
    private static final String SPECIAL_CHAR_REGEX = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        // Check for at least one lowercase letter
        boolean hasLowercase = Pattern.matches(LOWERCASE_REGEX, password);

        // Check for at least one uppercase letter
        boolean hasUppercase = Pattern.matches(UPPERCASE_REGEX, password);

        // Check for at least one digit
        boolean hasDigit = Pattern.matches(DIGIT_REGEX, password);

        // Check for at least one special character
        boolean hasSpecialChar = Pattern.matches(SPECIAL_CHAR_REGEX, password);

        // Require at least 3 out of the 4 character classes
        int criteriaCount = 0;
        if (hasLowercase) criteriaCount++;
        if (hasUppercase) criteriaCount++;
        if (hasDigit) criteriaCount++;
        if (hasSpecialChar) criteriaCount++;

        return criteriaCount >= 3;
    }
}