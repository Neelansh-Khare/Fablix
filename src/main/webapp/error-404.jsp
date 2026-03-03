<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>404 - Page Not Found | FabFlix</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/main.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="js/main.js"></script>
    <style>
        .error-container {
            text-align: center;
            padding: 4rem 2rem;
            max-width: 600px;
            margin: 0 auto;
        }
        .error-code {
            font-size: 6rem;
            color: #e74c3c;
            font-weight: bold;
            margin-bottom: 1rem;
        }
        .error-message {
            font-size: 1.5rem;
            margin-bottom: 2rem;
            color: #333;
        }
        .error-description {
            color: #666;
            margin-bottom: 2rem;
        }
        .back-home {
            display: inline-block;
            padding: 0.8rem 2rem;
            background-color: #e74c3c;
            color: white;
            border-radius: 4px;
            font-weight: 500;
            transition: background-color 0.3s;
        }
        .back-home:hover {
            background-color: #c0392b;
            color: white;
        }
    </style>
</head>
<body>
<div id="app">
    <header id="main-header">
        <div class="logo-container">
            <h1><a href="index.jsp" style="color: inherit;"><span class="highlight">Fab</span>Flix</a></h1>
        </div>
        <div class="nav-container">
            <nav>
                <ul>
                    <li><a href="index.jsp">Home</a></li>
                </ul>
            </nav>
        </div>
    </header>

    <main id="main-content">
        <div class="error-container">
            <div class="error-code">404</div>
            <h2 class="error-message">Oops! Page not found.</h2>
            <p class="error-description">
                The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.
            </p>
            <a href="index.jsp" class="back-home">Back to Home</a>
        </div>
    </main>

    <footer id="main-footer">
        <div class="footer-content">
            <p>&copy; 2023 FabFlix. All rights reserved.</p>
            <p>A project by Neelansh Khare</p>
        </div>
    </footer>
</div>
</body>
</html>
