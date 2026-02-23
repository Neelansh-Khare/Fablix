<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<html>
<head>
    <title>500 - Internal Server Error | FabFlix</title>
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
        .technical-details {
            margin-top: 3rem;
            text-align: left;
            background: #f8f8f8;
            padding: 1rem;
            border-radius: 4px;
            font-family: monospace;
            font-size: 0.8rem;
            overflow-x: auto;
            display: none;
        }
        .toggle-details {
            color: #3498db;
            cursor: pointer;
            font-size: 0.9rem;
            text-decoration: underline;
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
            <div class="error-code">500</div>
            <h2 class="error-message">Oops! Something went wrong.</h2>
            <p class="error-description">
                We're experiencing some technical difficulties on our end. Please try again later.
            </p>
            <a href="index.jsp" class="back-home">Back to Home</a>

            <%-- Only show details in development if needed, but for now we just have a toggle --%>
            <div style="margin-top: 2rem;">
                <span class="toggle-details" onclick="$('.technical-details').toggle()">Show Technical Details</span>
                <div class="technical-details">
                    <p><strong>Exception:</strong> <%= exception != null ? exception.getMessage() : "Unknown error" %></p>
                    <p><strong>Request URI:</strong> <%= request.getAttribute("javax.servlet.error.request_uri") %></p>
                </div>
            </div>
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
