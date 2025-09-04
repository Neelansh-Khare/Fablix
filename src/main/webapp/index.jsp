<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>FabFlix - Movie Database</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/main.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <!-- reCAPTCHA API with callback -->
    <script src="https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit" async defer></script>
    <script src="js/password-validation.js"></script>
    <script src="js/auth.js"></script>
    <script src="js/autocomplete.js"></script>
    <script src="js/poster-admin.js"></script>
    <script src="js/main.js"></script>
</head>
<body>
<div id="app">
    <!-- Header section -->
    <header id="main-header">
        <div class="logo-container">
            <h1><span class="highlight">Fab</span>Flix</h1>
        </div>
        <div class="search-container">
            <form id="search-form">
                <input type="text" id="search-input" placeholder="Search movies...">
                <button type="submit"><i class="fas fa-search"></i></button>
            </form>
        </div>
        <div class="nav-container">
            <nav>
                <ul>
                    <li><a href="#" id="home-link">Home</a></li>
                    <li><a href="#" id="browse-link">Browse</a></li>
                    <li class="auth-section" id="login-section"><a href="#" id="login-link">Login</a></li>
                    <li class="auth-section" id="account-section" style="display: none;">
                        <a href="#" id="account-link">Account</a>
                        <div class="dropdown-content">
                            <a href="#" id="profile-link">Profile</a>
                            <a href="#" id="logout-link">Logout</a>
                        </div>
                    </li>
                    <li><a href="#" id="cart-link">Cart (<span id="cart-count">0</span>)</a></li>
                </ul>
            </nav>
        </div>
    </header>

    <!-- Main content section -->
    <main id="main-content">
        <!-- This will be dynamically populated by JavaScript -->
        <div id="content-container">
            <!-- Content will be loaded here -->
        </div>
    </main>

    <!-- Footer section -->
    <footer id="main-footer">
        <div class="footer-content">
            <p>&copy; 2023 FabFlix. All rights reserved.</p>
            <p>A project by Neelansh Khare</p>
        </div>
    </footer>
</div>

<!-- Login Modal -->
<div id="login-modal" class="modal">
    <div class="modal-content">
        <span class="close">&times;</span>
        <div class="modal-header">
            <h2>Login to FabFlix</h2>
        </div>
        <div class="modal-body">
            <form id="login-form">
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" required>
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <div class="form-group">
                    <div id="login-recaptcha"></div>
                </div>
                <div class="form-group">
                    <button type="submit" class="btn btn-primary">Login</button>
                </div>
                <div class="form-footer">
                    <p>Don't have an account? <a href="#" id="register-link">Register</a></p>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Register Modal -->
<div id="register-modal" class="modal">
    <div class="modal-content">
        <span class="close">&times;</span>
        <div class="modal-header">
            <h2>Create an Account</h2>
        </div>
        <div class="modal-body">
            <form id="register-form">
                <div class="form-group">
                    <label for="reg-firstname">First Name</label>
                    <input type="text" id="reg-firstname" name="firstName" required>
                </div>
                <div class="form-group">
                    <label for="reg-lastname">Last Name</label>
                    <input type="text" id="reg-lastname" name="lastName" required>
                </div>
                <div class="form-group">
                    <label for="reg-email">Email</label>
                    <input type="email" id="reg-email" name="email" required>
                </div>
                <div class="form-group">
                    <label for="reg-password">Password</label>
                    <input type="password" id="reg-password" name="password" required>
                    <div id="password-strength-meter" class="password-strength">
                        <div class="strength-bar"></div>
                    </div>
                    <div id="password-strength-text"></div>
                    <div id="password-requirements" class="password-requirements">
                        Password must be at least 8 characters long and contain at least 3 of the following:
                        <ul>
                            <li id="req-lowercase">Lowercase letters</li>
                            <li id="req-uppercase">Uppercase letters</li>
                            <li id="req-digits">Digits</li>
                            <li id="req-special">Special characters</li>
                        </ul>
                    </div>
                </div>
                <div class="form-group">
                    <label for="reg-address">Address</label>
                    <input type="text" id="reg-address" name="address" required>
                </div>
                <div class="form-group">
                    <label for="reg-ccid">Credit Card Number</label>
                    <input type="text" id="reg-ccid" name="ccId" required>
                </div>
                <div class="form-group">
                    <div id="register-recaptcha"></div>
                </div>
                <div class="form-group">
                    <button type="submit" class="btn btn-primary">Register</button>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>