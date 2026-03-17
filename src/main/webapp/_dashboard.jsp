<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Admin Dashboard | FabFlix</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="css/main.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <style>
        .dashboard-container {
            padding: 2rem;
            max-width: 1200px;
            margin: 0 auto;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 2rem;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            text-align: center;
        }
        .stat-card h3 {
            margin-bottom: 10px;
            color: #666;
            font-size: 1.1rem;
        }
        .stat-value {
            font-size: 2rem;
            font-weight: bold;
            color: #e74c3c;
        }
        .stat-subtext {
            font-size: 0.9rem;
            color: #999;
            margin-top: 5px;
        }
        .popular-searches {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .search-item {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #eee;
        }
        .search-item:last-child {
            border-bottom: none;
        }
        .rank {
            font-weight: bold;
            color: #e74c3c;
            width: 30px;
            display: inline-block;
        }
        .btn-refresh {
            background: #3498db;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            cursor: pointer;
            margin-bottom: 20px;
        }
        .btn-refresh:hover {
            background: #2980b9;
        }
        .section-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
<div id="app">
    <header id="main-header">
        <div class="logo-container">
            <h1><a href="index.jsp" style="color: inherit;"><span class="highlight">Fab</span>Flix</a> <span style="font-size: 1rem; color: #666;">Admin</span></h1>
        </div>
        <div class="nav-container">
            <nav>
                <ul>
                    <li><a href="index.jsp">Back to Store</a></li>
                </ul>
            </nav>
        </div>
    </header>

    <main id="main-content">
        <div class="dashboard-container">
            <div class="section-header">
                <h2>Analytics Dashboard</h2>
                <button class="btn-refresh" onclick="loadAnalytics()"><i class="fas fa-sync-alt"></i> Refresh Data</button>
            </div>

            <div class="stats-grid">
                <div class="stat-card">
                    <h3>Poster Cache Hits</h3>
                    <div class="stat-value" id="poster-hits">0</div>
                    <div class="stat-subtext">TMDB API calls saved</div>
                </div>
                <div class="stat-card">
                    <h3>Poster Cache Misses</h3>
                    <div class="stat-value" id="poster-misses">0</div>
                    <div class="stat-subtext">New requests made</div>
                </div>
                <div class="stat-card">
                    <h3>Autocomplete Hits</h3>
                    <div class="stat-value" id="auto-hits">0</div>
                    <div class="stat-subtext">DB queries saved</div>
                </div>
                <div class="stat-card">
                    <h3>Autocomplete Misses</h3>
                    <div class="stat-value" id="auto-misses">0</div>
                    <div class="stat-subtext">New searches performed</div>
                </div>
            </div>

            <div class="popular-searches">
                <h3>Top 10 Popular Searches</h3>
                <div id="popular-searches-list">
                    <!-- Populated by JS -->
                    <p>Loading...</p>
                </div>
            </div>
        </div>
    </main>
</div>

<script>
    $(document).ready(function() {
        loadAnalytics();
    });

    function loadAnalytics() {
        $.ajax({
            url: 'api/admin/analytics',
            method: 'GET',
            success: function(response) {
                $('#poster-hits').text(response.posterHits || 0);
                $('#poster-misses').text(response.posterMisses || 0);
                $('#auto-hits').text(response.autocompleteHits || 0);
                $('#auto-misses').text(response.autocompleteMisses || 0);

                const list = $('#popular-searches-list');
                list.empty();
                
                if (response.popularSearches && response.popularSearches.length > 0) {
                    response.popularSearches.forEach((search, index) => {
                        list.append(`
                            <div class="search-item">
                                <div><span class="rank">#${index + 1}</span> ${escapeHtml(search.query)}</div>
                                <div style="color: #666;">${search.score} searches</div>
                            </div>
                        `);
                    });
                } else {
                    list.append('<p>No search data available yet.</p>');
                }
            },
            error: function() {
                $('#popular-searches-list').html('<p style="color: red;">Error loading analytics data.</p>');
            }
        });
    }
    
    function escapeHtml(unsafe) {
        return (unsafe || '').toString()
             .replace(/&/g, "&amp;")
             .replace(/</g, "&lt;")
             .replace(/>/g, "&gt;")
             .replace(/"/g, "&quot;")
             .replace(/'/g, "&#039;");
    }
</script>
</body>
</html>