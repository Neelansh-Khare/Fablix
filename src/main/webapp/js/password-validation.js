// password-validation.js
function validatePassword(password) {
    // Check length
    if (password.length < 8) {
        return { valid: false, message: "Password must be at least 8 characters long" };
    }

    // Check character classes
    let criteriaCount = 0;
    if (/[a-z]/.test(password)) criteriaCount++;
    if (/[A-Z]/.test(password)) criteriaCount++;
    if (/\d/.test(password)) criteriaCount++;
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) criteriaCount++;

    if (criteriaCount < 3) {
        return {
            valid: false,
            message: "Password must contain at least 3 of the following: lowercase letters, uppercase letters, digits, and special characters"
        };
    }

    return { valid: true };
}

// Add password strength indicator
function updatePasswordStrength(password) {
    let strength = 0;

    // Length contribution
    if (password.length >= 8) strength += 1;
    if (password.length >= 12) strength += 1;

    // Character class contribution
    if (/[a-z]/.test(password)) strength += 1;
    if (/[A-Z]/.test(password)) strength += 1;
    if (/\d/.test(password)) strength += 1;
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) strength += 1;

    // Map strength score to a label
    let strengthLabel, strengthClass;
    if (strength < 3) {
        strengthLabel = "Weak";
        strengthClass = "weak";
    } else if (strength < 5) {
        strengthLabel = "Medium";
        strengthClass = "medium";
    } else {
        strengthLabel = "Strong";
        strengthClass = "strong";
    }

    return {
        strengthLabel: strengthLabel,
        strengthClass: strengthClass
    };
}