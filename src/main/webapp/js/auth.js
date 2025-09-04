// auth.js - Handle authentication with reCAPTCHA

let loginRecaptchaWidget;
let registerRecaptchaWidget;
let recaptchaSiteKey;

// Fetch configuration and initialize reCAPTCHA
function initRecaptcha() {
    // First fetch the site key from the server
    $.ajax({
        url: 'api/config',
        method: 'GET',
        success: function(config) {
            recaptchaSiteKey = config.recaptchaSiteKey;
            console.log('Loaded reCAPTCHA site key:', recaptchaSiteKey.substring(0, 10) + '...');

            // Initialize login reCAPTCHA
            if (document.getElementById('login-recaptcha')) {
                loginRecaptchaWidget = grecaptcha.render('login-recaptcha', {
                    'sitekey': recaptchaSiteKey,
                    'callback': function(response) {
                        console.log('Login reCAPTCHA completed:', response.substring(0, 20) + '...');
                    }
                });
            }

            // Initialize register reCAPTCHA
            if (document.getElementById('register-recaptcha')) {
                registerRecaptchaWidget = grecaptcha.render('register-recaptcha', {
                    'sitekey': recaptchaSiteKey,
                    'callback': function(response) {
                        console.log('Register reCAPTCHA completed:', response.substring(0, 20) + '...');
                    }
                });
            }
        },
        error: function() {
            console.error('Failed to load reCAPTCHA configuration');
        }
    });
}

// Function to get reCAPTCHA response
function getRecaptchaResponse(widgetId) {
    return grecaptcha.getResponse(widgetId);
}

// Function to reset reCAPTCHA
function resetRecaptcha(widgetId) {
    grecaptcha.reset(widgetId);
}

// Enhanced login function with proper reCAPTCHA handling
function performLogin(email, password) {
    // Get reCAPTCHA response
    const recaptchaResponse = getRecaptchaResponse(loginRecaptchaWidget);

    if (!recaptchaResponse) {
        showErrorMessage('Please complete the reCAPTCHA verification.');
        return;
    }

    const loginData = {
        action: 'login',
        email: email,
        password: password,
        recaptcha: recaptchaResponse
    };

    console.log('Sending login request with reCAPTCHA:', recaptchaResponse.substring(0, 20) + '...');

    $.ajax({
        url: 'api/customers',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(loginData),
        success: function(response) {
            console.log('Login successful:', response);
            $('#login-modal').css('display', 'none');
            checkLoginStatus();
            showSuccessMessage('Login successful! Welcome back.');
            resetRecaptcha(loginRecaptchaWidget);
        },
        error: function(xhr) {
            console.error('Login failed:', xhr.responseText);
            let errorMsg = 'Login failed. Please check your credentials.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            showErrorMessage(errorMsg);
            resetRecaptcha(loginRecaptchaWidget);
        }
    });
}

// Enhanced registration function with proper reCAPTCHA handling
function performRegistration(formData) {
    // Get reCAPTCHA response
    const recaptchaResponse = getRecaptchaResponse(registerRecaptchaWidget);

    if (!recaptchaResponse) {
        showErrorMessage('Please complete the reCAPTCHA verification.');
        return;
    }

    // Add reCAPTCHA response to form data
    formData.recaptcha = recaptchaResponse;
    formData.action = 'register';

    console.log('Sending registration request with reCAPTCHA:', recaptchaResponse.substring(0, 20) + '...');

    $.ajax({
        url: 'api/customers',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(formData),
        success: function(response) {
            console.log('Registration successful:', response);
            $('#register-modal').css('display', 'none');
            showSuccessMessage('Registration successful! You can now log in.');
            resetRecaptcha(registerRecaptchaWidget);
        },
        error: function(xhr) {
            console.error('Registration failed:', xhr.responseText);
            let errorMsg = 'Registration failed. Please try again.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            showErrorMessage(errorMsg);
            resetRecaptcha(registerRecaptchaWidget);
        }
    });
}

// Make sure this function is called when reCAPTCHA API loads
window.onRecaptchaLoad = function() {
    console.log('reCAPTCHA API loaded, initializing widgets...');
    initRecaptcha();
};