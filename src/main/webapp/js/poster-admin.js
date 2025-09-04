// poster-admin.js - Admin interface for managing movie posters

function loadPosterAdmin() {
    const container = $('#content-container');
    container.empty();

    const adminHtml = `
        <div class="admin-container">
            <h2>Movie Poster Management</h2>
            
            <div class="admin-section">
                <h3>TMDB API Status</h3>
                <div id="api-status" class="status-container">
                    <div class="loading">Checking API status...</div>
                </div>
            </div>
            
            <div class="admin-section">
                <h3>Batch Update Posters</h3>
                <p>Update posters for movies that don't have poster images.</p>
                <div class="batch-controls">
                    <label for="batch-limit">Number of movies to process:</label>
                    <select id="batch-limit">
                        <option value="10">10 movies</option>
                        <option value="25">25 movies</option>
                        <option value="50">50 movies</option>
                    </select>
                    <button id="start-batch-update" class="btn btn-primary">Start Batch Update</button>
                </div>
                <div id="batch-progress" class="progress-container" style="display: none;">
                    <div class="progress-info">
                        <span id="progress-text">Processing...</span>
                    </div>
                    <div class="progress-bar">
                        <div id="progress-fill" class="progress-fill"></div>
                    </div>
                </div>
                <div id="batch-results" class="results-container"></div>
            </div>
            
            <div class="admin-section">
                <h3>Update Single Movie</h3>
                <p>Search for a specific movie and update its poster.</p>
                <div class="single-update-controls">
                    <input type="text" id="movie-search" placeholder="Search for a movie...">
                    <div id="movie-suggestions" class="movie-suggestions"></div>
                </div>
                <div id="selected-movie" class="selected-movie" style="display: none;">
                    <div class="movie-preview">
                        <div class="movie-info">
                            <h4 id="selected-title"></h4>
                            <p id="selected-details"></p>
                        </div>
                        <div class="movie-actions">
                            <button id="update-single-poster" class="btn btn-primary">Update Poster</button>
                            <button id="clear-selection" class="btn btn-secondary">Clear</button>
                        </div>
                    </div>
                </div>
                <div id="single-results" class="results-container"></div>
            </div>
        </div>
    `;

    container.append(adminHtml);

    // Initialize admin interface
    initializePosterAdmin();
}

function initializePosterAdmin() {
    // Check API status
    checkTmdbApiStatus();

    // Bind event listeners
    $('#start-batch-update').on('click', startBatchUpdate);
    $('#update-single-poster').on('click', updateSinglePoster);
    $('#clear-selection').on('click', clearMovieSelection);
    $('#movie-search').on('input', handleMovieSearch);

    // Initialize movie search with autocomplete
    initializeMovieSearch();
}

function checkTmdbApiStatus() {
    $.ajax({
        url: 'api/admin/update-posters',
        method: 'GET',
        success: function(response) {
            displayApiStatus(response);
        },
        error: function() {
            displayApiStatus({
                tmdbApiConfigured: false,
                message: 'Error checking API status'
            });
        }
    });
}

function displayApiStatus(status) {
    const container = $('#api-status');
    const isConfigured = status.tmdbApiConfigured;

    const statusHtml = `
        <div class="status-item ${isConfigured ? 'status-success' : 'status-error'}">
            <span class="status-icon">${isConfigured ? '✅' : '❌'}</span>
            <span class="status-text">${status.message}</span>
        </div>
        ${!isConfigured ? `
            <div class="status-help">
                <p>To enable poster fetching:</p>
                <ol>
                    <li>Get a free API key from <a href="https://www.themoviedb.org/settings/api" target="_blank">TMDB</a></li>
                    <li>Update the TMDB_API_KEY in MoviePosterUtil.java</li>
                    <li>Rebuild and restart your application</li>
                </ol>
            </div>
        ` : ''}
    `;

    container.html(statusHtml);
}

function startBatchUpdate() {
    const limit = parseInt($('#batch-limit').val());
    const button = $('#start-batch-update');
    const progressContainer = $('#batch-progress');
    const resultsContainer = $('#batch-results');

    // Disable button and show progress
    button.prop('disabled', true).text('Processing...');
    progressContainer.show();
    resultsContainer.empty();

    $('#progress-text').text(`Processing up to ${limit} movies...`);
    $('#progress-fill').css('width', '0%');

    $.ajax({
        url: 'api/admin/update-posters',
        method: 'POST',
        data: {
            batch: 'true',
            limit: limit
        },
        success: function(response) {
            displayBatchResults(response);
        },
        error: function(xhr) {
            let errorMsg = 'Error during batch update';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            displayBatchResults({
                message: errorMsg,
                updatedCount: 0,
                errorCount: 0,
                totalProcessed: 0
            });
        },
        complete: function() {
            // Re-enable button and hide progress
            button.prop('disabled', false).text('Start Batch Update');
            progressContainer.hide();
        }
    });
}

function displayBatchResults(results) {
    const container = $('#batch-results');
    const successRate = results.totalProcessed > 0 ?
        (results.updatedCount / results.totalProcessed * 100).toFixed(1) : 0;

    const resultsHtml = `
        <div class="results-summary">
            <h4>Batch Update Results</h4>
            <div class="results-stats">
                <div class="stat-item">
                    <span class="stat-label">Movies Processed:</span>
                    <span class="stat-value">${results.totalProcessed}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Successfully Updated:</span>
                    <span class="stat-value success">${results.updatedCount}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Errors:</span>
                    <span class="stat-value error">${results.errorCount}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Success Rate:</span>
                    <span class="stat-value">${successRate}%</span>
                </div>
            </div>
            <p class="results-message">${results.message}</p>
        </div>
    `;

    container.html(resultsHtml);
}

let movieSearchTimeout;

function handleMovieSearch() {
    const query = $('#movie-search').val().trim();

    if (movieSearchTimeout) {
        clearTimeout(movieSearchTimeout);
    }

    if (query.length < 2) {
        $('#movie-suggestions').empty().hide();
        return;
    }

    movieSearchTimeout = setTimeout(function() {
        searchMoviesForAdmin(query);
    }, 300);
}

function searchMoviesForAdmin(query) {
    $.ajax({
        url: 'api/search',
        method: 'GET',
        data: {
            query: query
        },
        success: function(response) {
            displayMovieSuggestions(response.movies);
        },
        error: function() {
            $('#movie-suggestions').empty().hide();
        }
    });
}

function displayMovieSuggestions(movies) {
    const container = $('#movie-suggestions');
    container.empty();

    if (movies.length === 0) {
        container.hide();
        return;
    }

    movies.slice(0, 5).forEach(function(movie) {
        const suggestion = $(`
            <div class="movie-suggestion" data-id="${movie.id}">
                <div class="suggestion-title">${movie.title}</div>
                <div class="suggestion-details">${movie.year} - ${movie.director}</div>
            </div>
        `);

        suggestion.on('click', function() {
            selectMovie(movie);
        });

        container.append(suggestion);
    });

    container.show();
}

function selectMovie(movie) {
    $('#movie-search').val(movie.title);
    $('#movie-suggestions').empty().hide();

    $('#selected-title').text(movie.title);
    $('#selected-details').text(`${movie.year} - Directed by ${movie.director}`);
    $('#selected-movie').show().attr('data-id', movie.id);

    $('#single-results').empty();
}

function clearMovieSelection() {
    $('#movie-search').val('');
    $('#movie-suggestions').empty().hide();
    $('#selected-movie').hide().removeAttr('data-id');
    $('#single-results').empty();
}

function updateSinglePoster() {
    const movieId = $('#selected-movie').attr('data-id');
    if (!movieId) return;

    const button = $('#update-single-poster');
    const resultsContainer = $('#single-results');

    button.prop('disabled', true).text('Updating...');
    resultsContainer.empty();

    $.ajax({
        url: 'api/admin/update-posters',
        method: 'POST',
        data: {
            movieId: movieId
        },
        success: function(response) {
            displaySingleResults(response, true);
        },
        error: function(xhr) {
            let errorMsg = 'Error updating poster';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            displaySingleResults({message: errorMsg}, false);
        },
        complete: function() {
            button.prop('disabled', false).text('Update Poster');
        }
    });
}

function displaySingleResults(results, success) {
    const container = $('#single-results');
    const statusClass = success ? 'results-success' : 'results-error';

    const resultsHtml = `
        <div class="results-summary ${statusClass}">
            <div class="results-icon">${success ? '✅' : '❌'}</div>
            <div class="results-text">
                <p>${results.message}</p>
                ${results.updatedCount !== undefined ?
        `<p>Updated: ${results.updatedCount} movie(s)</p>` : ''}
            </div>
        </div>
    `;

    container.html(resultsHtml);
}

function initializeMovieSearch() {
    // Hide suggestions when clicking outside
    $(document).on('click', function(e) {
        if (!$(e.target).closest('.single-update-controls').length) {
            $('#movie-suggestions').hide();
        }
    });
}

// Add to main navigation or admin menu
function addPosterAdminLink() {
    // This would be called from your main.js to add admin link
    if ($('#poster-admin-link').length === 0) {
        $('.nav-container ul').append('<li><a href="#" id="poster-admin-link">Poster Admin</a></li>');

        $('#poster-admin-link').on('click', function(e) {
            e.preventDefault();
            loadPosterAdmin();
        });
    }
}