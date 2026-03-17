$(document).ready(function() {
    // Check login status
    checkLoginStatus();

    // Load homepage content
    loadHomePage();

    // Event listeners
    setupEventListeners();

    // Setup password validation
    setupPasswordValidation();

    // Initialize autocomplete
    initializeAutocomplete();

    // Initialize cart count
    updateCartCount();
});

// Add Loading Overlay Styles
$('<style>')
    .prop('type', 'text/css')
    .html(`
        #loading-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255, 255, 255, 0.95);
            z-index: 10000;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }
        .loader {
            border: 8px solid #f3f3f3;
            border-top: 8px solid #e74c3c;
            border-radius: 50%;
            width: 60px;
            height: 60px;
            animation: spin 1s linear infinite;
            margin-bottom: 20px;
        }
        .loading-text {
            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
            font-size: 1.2em;
            color: #333;
            font-weight: 500;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    `)
    .appendTo('head');

// Add Loading Overlay HTML
$('body').append(`
    <div id="loading-overlay" style="display: none;">
        <div class="loader"></div>
        <div class="loading-text">Fetching movie posters...</div>
    </div>
`);

function checkLoginStatus() {
    $.ajax({
        url: 'api/auth/status',
        method: 'GET',
        success: function(response) {
            if (response.loggedIn) {
                // User is logged in
                $('#login-section').hide();
                $('#account-section').show();
                $('#account-link').text(response.name);
                
                // Add poster admin link if user is logged in
                if (typeof addPosterAdminLink === 'function') {
                    addPosterAdminLink();
                }

                // Add Admin Dashboard link if user is an admin
                if (response.role === 'admin') {
                    if ($('#admin-dashboard-link').length === 0) {
                        const adminLinkHtml = `<li><a href="_dashboard.jsp" id="admin-dashboard-link"><i class="fas fa-chart-line"></i> Dashboard</a></li>`;
                        $('.nav-container ul').prepend(adminLinkHtml);
                    }
                }
            } else {
                // User is not logged in
                $('#login-section').show();
                $('#account-section').hide();
                $('#admin-dashboard-link').parent().remove();
            }
        },
        error: function() {
            console.error('Error checking login status');
        }
    });
}

function loadHomePage() {
    // Show loading overlay
    $('#loading-overlay').fadeIn(200);

    fetchMoviesWithPolling(1, 12);
}

function fetchMoviesWithPolling(page, pageSize, attempt = 1) {
    const timestamp = new Date().getTime();

    $.ajax({
        url: 'api/movies',
        method: 'GET',
        cache: false,
        data: {
            page: page,
            pageSize: pageSize,
            _t: timestamp
        },
        success: function(response) {
            const movies = response.movies;
            const pendingMovies = movies.filter(isPosterPending);

            if (pendingMovies.length > 0 && attempt < 20) { // Max 20 attempts (~10-20 seconds)
                console.log(`Waiting for ${pendingMovies.length} posters... (Attempt ${attempt})`);
                setTimeout(() => fetchMoviesWithPolling(page, pageSize, attempt + 1), 1000);
            } else {
                // Done waiting or timeout
                $('#loading-overlay').fadeOut(200);
                displayMovies(movies);
            }
        },
        error: function(xhr, status, error) {
            console.error('Error loading movies:', error);
            $('#loading-overlay').hide();
            showErrorMessage('Error loading movies. Please try again later.');
        }
    });
}

function isPosterPending(movie) {
    if (!movie.bannerUrl) return true;
    if (movie.bannerUrl === 'poster_not_found') return false; // Explicitly failed
    if (movie.bannerUrl.startsWith('http')) return false; // Success
    // If it's the default placeholder, it's pending
    if (movie.bannerUrl.includes('no-poster.jpg') || movie.bannerUrl.includes('placeholder')) return true;
    return false;
}

// Enhanced displayMovies with better debugging
function displayMovies(movies) {
    const container = $('#content-container');
    container.empty();

    // Add header
    container.append(`
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2>Featured Movies</h2>
        </div>
    `);

    // Create movie grid
    const grid = $('<div class="movie-grid"></div>');

    // Add movie cards with enhanced debugging
    movies.forEach(function(movie) {
        const hasValidPoster = movie.bannerUrl &&
            !movie.bannerUrl.includes('no-poster.jpg') &&
            !movie.bannerUrl.includes('placeholder') &&
            movie.bannerUrl.startsWith('http');

        const card = `
            <div class="movie-card" data-id="${movie.id}">
                <div class="movie-poster">
                    <img src="${hasValidPoster ? movie.bannerUrl : 'images/no-poster.jpg'}" 
                         alt="${movie.title} poster" 
                         onerror="this.src='images/no-poster.jpg';"
                         onload="console.log('Successfully loaded poster for ${movie.title}');">
                </div>
                <div class="movie-info">
                    <div class="movie-title">${movie.title}</div>
                    <div class="movie-year">${movie.year}</div>
                    <div class="movie-director">Dir. ${movie.director}</div>
                    <button class="btn btn-primary add-to-cart" data-id="${movie.id}" data-title="${movie.title}">Add to Cart</button>
                </div>
            </div>
        `;
        grid.append(card);
    });

    container.append(grid);
}

function setupEventListeners() {
    // Navigation links
    $('#home-link').click(function(e) {
        e.preventDefault();
        loadHomePage();
    });

    $('#browse-link').click(function(e) {
        e.preventDefault();
        loadBrowsePage();
    });

    // Movie card click
    $(document).on('click', '.movie-card', function(e) {
        // Prevent card click when clicking the button
        if (!$(e.target).hasClass('add-to-cart')) {
            const movieId = $(this).data('id');
            loadMovieDetails(movieId);
        }
    });

    // Add to cart functionality
    $(document).on('click', '.add-to-cart', function(e) {
        e.stopPropagation();
        const movieId = $(this).data('id');
        const movieTitle = $(this).data('title');
        addToCart(movieId, movieTitle);
    });

    // Cart link
    $('#cart-link').click(function(e) {
        e.preventDefault();
        loadCart();
    });

    // Login/Register modals
    $('#login-link').click(function(e) {
        e.preventDefault();
        $('#login-modal').css('display', 'block');
        // Initialize reCAPTCHA if not already done
        setTimeout(function() {
            if (typeof grecaptcha !== 'undefined' && !loginRecaptchaWidget) {
                initRecaptcha();
            }
        }, 100);
    });

    $('#register-link').click(function(e) {
        e.preventDefault();
        $('#login-modal').css('display', 'none');
        $('#register-modal').css('display', 'block');
        // Initialize reCAPTCHA if not already done
        setTimeout(function() {
            if (typeof grecaptcha !== 'undefined' && !registerRecaptchaWidget) {
                initRecaptcha();
            }
        }, 100);
    });

    // Close modals when clicking the X
    $('.close').click(function() {
        $('.modal').css('display', 'none');
    });

    // Close modals when clicking outside
    $(window).click(function(e) {
        if ($(e.target).hasClass('modal')) {
            $('.modal').css('display', 'none');
        }
    });

    // Login form submission
    $('#login-form').submit(function(e) {
        e.preventDefault();

        const email = $('#email').val();
        const password = $('#password').val();

        // Use the enhanced login function from auth.js
        if (typeof performLogin === 'function') {
             performLogin(email, password);
        } else {
             // Fallback if auth.js is not loaded
             $.ajax({
                 url: 'api/auth/login', // Adjust if needed based on your API
                 method: 'POST',
                 data: { email: email, password: password },
                 success: function() {
                     checkLoginStatus();
                     $('#login-modal').css('display', 'none');
                     showSuccessMessage('Login successful!');
                 },
                 error: function() {
                     showErrorMessage('Login failed. Please check your credentials.');
                 }
             });
        }
    });

    // Register form submission
    $('#register-form').submit(function(e) {
        e.preventDefault();

        const formData = {
            firstName: $('#reg-firstname').val(),
            lastName: $('#reg-lastname').val(),
            email: $('#reg-email').val(),
            password: $('#reg-password').val(),
            address: $('#reg-address').val(),
            ccId: $('#reg-ccid').val()
        };

        // Validate password
        if (typeof validatePassword === 'function') {
            const passwordValidation = validatePassword(formData.password);
            if (!passwordValidation.valid) {
                showErrorMessage(passwordValidation.message);
                return;
            }
        }

        // Use the enhanced registration function from auth.js
        if (typeof performRegistration === 'function') {
            performRegistration(formData);
        } else {
             // Fallback
             $.ajax({
                 url: 'api/auth/register',
                 method: 'POST',
                 data: formData,
                 success: function() {
                     $('#register-modal').css('display', 'none');
                     showSuccessMessage('Registration successful! Please log in.');
                     $('#login-modal').css('display', 'block');
                 },
                 error: function() {
                     showErrorMessage('Registration failed. Please try again.');
                 }
             });
        }
    });

    // Logout link
    $('#logout-link').click(function(e) {
        e.preventDefault();

        $.ajax({
            url: 'api/auth/logout',
            method: 'GET',
            success: function() {
                checkLoginStatus();
                loadHomePage();
                showSuccessMessage('You have been logged out successfully.');
                // Clear cart
                localStorage.removeItem('cart');
            },
            error: function() {
                showErrorMessage('Error logging out. Please try again.');
            }
        });
    });

    // Search form
    $('#search-form').submit(function(e) {
        e.preventDefault();

        const query = $('#search-input').val().trim();

        if (query) {
            $.ajax({
                url: 'api/search',
                method: 'GET',
                data: {
                    query: query
                },
                success: function(response) {
                    if (response.movies.length === 0) {
                        showInfoMessage('No movies found matching your search.');
                    } else {
                        $('#content-container').empty()
                            .append(`<h2>Search Results for "${query}"</h2>`);
                        displayMovies(response.movies);
                    }
                },
                error: function() {
                    showErrorMessage('Error performing search. Please try again.');
                }
            });
        }
    });
}

function setupPasswordValidation() {
    $('#reg-password').on('input', function() {
        const password = $(this).val();
        if (typeof updatePasswordStrength === 'function') {
            const strength = updatePasswordStrength(password);

            // Update strength bar
            const strengthBar = $('.strength-bar');
            strengthBar.removeClass('weak medium strong');
            strengthBar.addClass(strength.strengthClass);

            // Update strength text
            const strengthText = $('#password-strength-text');
            strengthText.removeClass('weak medium strong');
            strengthText.addClass(strength.strengthClass);
            strengthText.text(strength.strengthLabel);
        }
        
        // Update requirements
        updatePasswordRequirements(password);
    });
}

function updatePasswordRequirements(password) {
    const requirements = {
        'req-lowercase': /[a-z]/.test(password),
        'req-uppercase': /[A-Z]/.test(password),
        'req-digits': /\d/.test(password),
        'req-special': /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
    };

    Object.keys(requirements).forEach(reqId => {
        const element = $(`#${reqId}`);
        if (requirements[reqId]) {
            element.addClass('requirement-met');
        } else {
            element.removeClass('requirement-met');
        }
    });
}

// Shopping Cart Functions - Server Side
function addToCart(movieId, movieTitle) {
    $.ajax({
        url: 'api/cart',
        method: 'POST',
        data: {
            movieId: movieId,
            action: 'add',
            quantity: 1
        },
        success: function(response) {
            showSuccessMessage(`${movieTitle} added to cart!`);
            updateCartCount();
        },
        error: function() {
            showErrorMessage('Error adding to cart. Please try again.');
        }
    });
}

function updateCartCount() {
    $.ajax({
        url: 'api/cart',
        method: 'GET',
        success: function(response) {
            let totalItems = response.count;
            let cartLink = $('#cart-link');
            if (cartLink.length === 0) {
                // Add cart link if it doesn't exist
                const cartHtml = `<li><a href="#" id="cart-link">Cart (<span id="cart-count">0</span>)</a></li>`;
                $('.nav-container ul').append(cartHtml);
                cartLink = $('#cart-link');

                // Re-bind event listener
                cartLink.click(function(e) {
                    e.preventDefault();
                    loadCart();
                });
            }
            $('#cart-count').text(totalItems);
        }
    });
}

function loadCart() {
    $.ajax({
        url: 'api/cart',
        method: 'GET',
        success: function(response) {
            displayCart(response.items, response.total);
        },
        error: function() {
            showErrorMessage('Error loading cart.');
        }
    });
}

function displayCart(cartItems, total) {
    const container = $('#content-container');
    container.empty();

    if (!cartItems || cartItems.length === 0) {
        container.append(`
            <div class="cart-container">
                <h2>Shopping Cart</h2>
                <p>Your cart is empty.</p>
                <button class="btn btn-primary" onclick="loadHomePage()">Continue Shopping</button>
            </div>
        `);
        return;
    }

    let cartHtml = `
        <div class="cart-container">
            <h2>Shopping Cart</h2>
            <div class="cart-items">
    `;

    cartItems.forEach(item => {
        cartHtml += `
            <div class="cart-item" data-id="${item.id}">
                <div class="item-info">
                    <h4>${item.title}</h4>
                    <p>Price: $${item.price.toFixed(2)}</p>
                </div>
                <div class="item-controls">
                    <button class="btn btn-sm decrease-qty" data-id="${item.id}" data-qty="${item.quantity}">-</button>
                    <span class="quantity">${item.quantity}</span>
                    <button class="btn btn-sm increase-qty" data-id="${item.id}" data-qty="${item.quantity}">+</button>
                    <button class="btn btn-danger remove-item" data-id="${item.id}">Remove</button>
                </div>
                <div class="item-total">$${item.total.toFixed(2)}</div>
            </div>
        `;
    });

    cartHtml += `
            </div>
            <div class="cart-summary">
                <h3>Total: $${total.toFixed(2)}</h3>
                <button class="btn btn-primary" onclick="proceedToCheckout()">Proceed to Checkout</button>
                <button class="btn btn-secondary" onclick="loadHomePage()">Continue Shopping</button>
            </div>
        </div>
    `;

    container.append(cartHtml);
    bindCartEventListeners();
}

function bindCartEventListeners() {
    $('.increase-qty').click(function() {
        const movieId = $(this).data('id');
        const currentQty = $(this).data('qty');
        updateCartQuantity(movieId, currentQty + 1);
    });

    $('.decrease-qty').click(function() {
        const movieId = $(this).data('id');
        const currentQty = $(this).data('qty');
        updateCartQuantity(movieId, currentQty - 1);
    });

    $('.remove-item').click(function() {
        const movieId = $(this).data('id');
        removeFromCart(movieId);
    });
}

function updateCartQuantity(movieId, newQuantity) {
    if (newQuantity <= 0) {
        removeFromCart(movieId);
        return;
    }
    
    $.ajax({
        url: 'api/cart',
        method: 'POST',
        data: {
            movieId: movieId,
            action: 'update',
            quantity: newQuantity
        },
        success: function(response) {
            loadCart(); // Reload to refresh totals
            updateCartCount();
        },
        error: function() {
            showErrorMessage('Error updating cart.');
        }
    });
}

function removeFromCart(movieId) {
    $.ajax({
        url: 'api/cart',
        method: 'POST',
        data: {
            movieId: movieId,
            action: 'remove'
        },
        success: function(response) {
            loadCart();
            updateCartCount();
        },
        error: function() {
            showErrorMessage('Error removing item.');
        }
    });
}

function proceedToCheckout() {
    // Check if user is logged in
    $.ajax({
        url: 'api/auth/status',
        method: 'GET',
        success: function(response) {
            if (response.loggedIn) {
                loadCheckout();
            } else {
                showInfoMessage('Please log in to proceed with checkout.');
                $('#login-modal').css('display', 'block');
            }
        },
        error: function() {
            showErrorMessage('Error checking login status. Please try again.');
        }
    });
}

function loadCheckout() {
    $.ajax({
        url: 'api/cart',
        method: 'GET',
        success: function(response) {
            if (!response.items || response.items.length === 0) {
                showErrorMessage('Your cart is empty.');
                return;
            }
            displayCheckout(response.items, response.total);
        },
        error: function() {
            showErrorMessage('Error loading checkout.');
        }
    });
}

function displayCheckout(cartItems, total) {
    const container = $('#content-container');
    container.empty();

    const checkoutHtml = `
        <div class="checkout-container">
            <h2>Checkout</h2>
            <div class="checkout-content">
                <div class="order-summary">
                    <h3>Order Summary</h3>
                    ${cartItems.map(item => `
                        <div class="order-item">
                            <span>${item.title} x ${item.quantity}</span>
                            <span>$${item.total.toFixed(2)}</span>
                        </div>
                    `).join('')}
                    <div class="order-total">
                        <strong>Total: $${total.toFixed(2)}</strong>
                    </div>
                </div>
                <div class="payment-form">
                    <h3>Payment Information</h3>
                    <form id="checkout-form">
                        <div class="form-group">
                            <label for="checkout-cc">Credit Card Number</label>
                            <input type="text" id="checkout-cc" name="creditCard" required>
                        </div>
                        <div class="form-group">
                            <label for="checkout-exp">Expiration Date</label>
                            <input type="text" id="checkout-exp" name="expirationDate" placeholder="MM/YY" required>
                        </div>
                        <div class="form-group">
                            <label for="checkout-cvv">CVV</label>
                            <input type="text" id="checkout-cvv" name="cvv" required>
                        </div>
                        <div class="form-group">
                            <label for="checkout-name">Cardholder Name</label>
                            <input type="text" id="checkout-name" name="cardholderName" required>
                        </div>
                        <div class="form-group">
                            <button type="submit" class="btn btn-primary">Complete Purchase</button>
                            <button type="button" class="btn btn-secondary" onclick="loadCart()">Back to Cart</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `;

    container.append(checkoutHtml);

    // Bind checkout form submission
    $('#checkout-form').submit(function(e) {
        e.preventDefault();
        processPayment();
    });
}

function processPayment() {
    showSuccessMessage('Processing payment...');
    
    const formData = {
        creditCard: $('#checkout-cc').val(),
        expirationDate: $('#checkout-exp').val(),
        cvv: $('#checkout-cvv').val(),
        cardholderName: $('#checkout-name').val()
    };

    $.ajax({
        url: 'api/checkout',
        method: 'POST',
        data: formData,
        success: function(response) {
            updateCartCount();
            
            const container = $('#content-container');
            container.empty();
            container.append(`
                <div class="order-confirmation">
                    <h2>Order Confirmed!</h2>
                    <p>Thank you for your purchase. Your order has been processed successfully.</p>
                    <p>Order ID: #${response.orderId}</p>
                    <button class="btn btn-primary" onclick="loadHomePage()">Continue Shopping</button>
                </div>
            `);
            showSuccessMessage('Payment successful!');
        },
        error: function(xhr) {
            let errorMsg = 'Payment failed.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            showErrorMessage(errorMsg);
        }
    });
}

// Initialize cart count on page load
$(document).ready(function() {
    updateCartCount();
});

// Rest of the existing functions remain the same...
function loadBrowsePage() {
    // Fetch all genres
    $.ajax({
        url: 'api/genres',
        method: 'GET',
        success: function(response) {
            displayBrowseOptions(response.genres);
        },
        error: function() {
            showErrorMessage('Error loading browse options. Please try again later.');
        }
    });
}

function displayBrowseOptions(genres) {
    const container = $('#content-container');
    container.empty();

    // Add header
    container.append(`<h2>Browse Movies</h2>`);

    // Add browse by letter section
    const alphabetSection = $('<div class="browse-section"></div>');
    alphabetSection.append(`<h3>Browse by Title</h3>`);

    const alphabetList = $('<div class="alphabet-list"></div>');
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.split('').forEach(function(letter) {
        alphabetList.append(`<a href="#" class="letter-link" data-letter="${letter}">${letter}</a>`);
    });
    alphabetSection.append(alphabetList);
    container.append(alphabetSection);

    // Add browse by genre section
    const genreSection = $('<div class="browse-section"></div>');
    genreSection.append(`<h3>Browse by Genre</h3>`);

    const genreList = $('<div class="genre-list"></div>');
    genres.forEach(function(genre) {
        genreList.append(`<a href="#" class="genre-link" data-id="${genre.id}">${genre.name}</a>`);
    });
    genreSection.append(genreList);
    container.append(genreSection);

    // Event listeners for browse links
    $('.letter-link').click(function(e) {
        e.preventDefault();
        const letter = $(this).data('letter');

        $.ajax({
            url: 'api/search',
            method: 'GET',
            data: {
                title: letter + '%'
            },
            success: function(response) {
                if (response.movies.length === 0) {
                    showInfoMessage(`No movies found starting with "${letter}".`);
                } else {
                    $('#content-container').empty()
                        .append(`<h2>Movies Starting with "${letter}"</h2>`);
                    displayMovies(response.movies);
                }
            },
            error: function() {
                showErrorMessage('Error browsing by letter. Please try again.');
            }
        });
    });

    $('.genre-link').click(function(e) {
        e.preventDefault();
        const genreId = $(this).data('id');
        const genreName = $(this).text();

        $.ajax({
            url: 'api/search',
            method: 'GET',
            data: {
                genre: genreId
            },
            success: function(response) {
                if (response.movies.length === 0) {
                    showInfoMessage(`No movies found in the "${genreName}" genre.`);
                } else {
                    $('#content-container').empty()
                        .append(`<h2>${genreName} Movies</h2>`);
                    displayMovies(response.movies);
                }
            },
            error: function() {
                showErrorMessage('Error browsing by genre. Please try again.');
            }
        });
    });
}

function loadMovieDetails(movieId) {
    $.ajax({
        url: `api/movies/${movieId}`,
        method: 'GET',
        success: function(movie) {
            displayMovieDetails(movie);
        },
        error: function() {
            showErrorMessage('Error loading movie details. Please try again later.');
        }
    });
}

function displayMovieDetails(movie) {
    const container = $('#content-container');
    container.empty();

    // Determine valid poster URL
    const hasValidPoster = movie.bannerUrl &&
        movie.bannerUrl !== 'poster_not_found' &&
        !movie.bannerUrl.includes('no-poster.jpg') &&
        !movie.bannerUrl.includes('placeholder') &&
        movie.bannerUrl.startsWith('http');

    const posterUrl = hasValidPoster ? movie.bannerUrl : 'images/no-poster.jpg';

    // Create movie detail section
    const detailHtml = `
        <div class="movie-detail">
            <div class="movie-detail-poster">
                <img src="${posterUrl}" alt="${movie.title} poster" onerror="this.src='images/no-poster.jpg'">
            </div>
            <div class="movie-detail-info">
                <h1 class="movie-detail-title">${movie.title}</h1>
                <div class="movie-detail-meta">
                    <div class="movie-detail-year">${movie.year}</div>
                    <div class="movie-detail-director">Directed by ${movie.director}</div>
                </div>
                
                <div class="movie-detail-genres">
                    <h3>Genres</h3>
                    <div class="genre-tags">
                        ${movie.genres.map(genre =>
        `<span class="genre-tag">${genre.name}</span>`
    ).join('')}
                    </div>
                </div>
                
                <div class="movie-detail-stars">
                    <h3>Starring</h3>
                    <div class="star-list">
                        ${movie.stars.map(star =>
        `<a href="#" class="star-link" data-id="${star.id}">${star.name}</a>`
    ).join('')}
                    </div>
                </div>
                
                <div class="movie-actions">
                    <button class="btn btn-primary add-to-cart" data-id="${movie.id}" data-title="${movie.title}">Add to Cart - $9.99</button>
                </div>
                
                ${movie.trailerUrl ?
        `<div class="movie-trailer">
                        <h3>Trailer</h3>
                        <div class="trailer-container">
                            <iframe width="560" height="315" src="${movie.trailerUrl}" frameborder="0" allowfullscreen></iframe>
                        </div>
                    </div>`
        : ''}
            </div>
        </div>
    `;

    container.append(detailHtml);

    // Add event listener for star links
    $('.star-link').click(function(e) {
        e.preventDefault();
        const starId = $(this).data('id');
        loadStarDetails(starId);
    });

    // Add event listener for add to cart in movie details
    $('.add-to-cart').click(function() {
        const movieId = $(this).data('id');
        const movieTitle = $(this).data('title');
        addToCart(movieId, movieTitle);
    });
}

function loadStarDetails(starId) {
    $.ajax({
        url: `api/stars/${starId}`,
        method: 'GET',
        success: function(star) {
            displayStarDetails(star);
        },
        error: function() {
            showErrorMessage('Error loading star details. Please try again later.');
        }
    });
}

function displayStarDetails(star) {
    const container = $('#content-container');
    container.empty();

    // Create star detail section
    const detailHtml = `
        <div class="star-detail">
            <div class="star-detail-photo">
                <img src="${star.photoUrl || 'images/no-photo.jpg'}" alt="${star.name}" onerror="this.src='images/no-photo.jpg'">
            </div>
            <div class="star-detail-info">
                <h1 class="star-detail-name">${star.name}</h1>
                ${star.birthYear ? `<div class="star-detail-birthyear">Born: ${star.birthYear}</div>` : ''}
                
                <div class="star-movies">
                    <h3>Filmography</h3>
                    <div class="movie-list">
                        ${star.movies && star.movies.length > 0 ?
        star.movies.map(movie =>
            `<a href="#" class="movie-link" data-id="${movie.id}">${movie.title} (${movie.year})</a>`
        ).join('')
        : '<p>No movies found for this star.</p>'
    }
                    </div>
                </div>
            </div>
        </div>
    `;

    container.append(detailHtml);

    // Add event listener for movie links
    $('.movie-link').click(function(e) {
        e.preventDefault();
        const movieId = $(this).data('id');
        loadMovieDetails(movieId);
    });
}

// Helper functions for displaying messages
function showSuccessMessage(message) {
    showMessage(message, 'success');
}

function showErrorMessage(message) {
    showMessage(message, 'error');
}

function showInfoMessage(message) {
    showMessage(message, 'info');
}

function showMessage(message, type) {
    // Remove any existing messages
    $('.message-container').remove();

    // Create message container
    const messageContainer = $(`<div class="message-container ${type}-message"></div>`);
    messageContainer.append(`<p>${message}</p>`);
    messageContainer.append('<span class="message-close">&times;</span>');

    // Add to DOM
    $('body').append(messageContainer);

    // Show message with animation
    setTimeout(function() {
        messageContainer.addClass('show');
    }, 10);

    // Auto hide after 5 seconds
    setTimeout(function() {
        messageContainer.removeClass('show');
        setTimeout(function() {
            messageContainer.remove();
        }, 300);
    }, 5000);

    // Close button functionality
    $('.message-close').click(function() {
        messageContainer.removeClass('show');
        setTimeout(function() {
            messageContainer.remove();
        }, 300);
    });
}

// Additional styling for messages
$('<style>')
    .prop('type', 'text/css')
    .html(`
        .message-container {
            position: fixed;
            bottom: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 4px;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
            color: white;
            font-weight: 500;
            max-width: 300px;
            z-index: 9999;
            transform: translateX(120%);
            transition: transform 0.3s ease-out;
        }
        
        .message-container.show {
            transform: translateX(0);
        }
        
        .success-message {
            background-color: #2ecc71;
        }
        
        .error-message {
            background-color: #e74c3c;
        }
        
        .info-message {
            background-color: #3498db;
        }
        
        .message-container p {
            margin: 0;
            padding-right: 20px;
        }
        
        .message-close {
            position: absolute;
            top: 5px;
            right: 10px;
            font-size: 18px;
            font-weight: bold;
            cursor: pointer;
        }
        
        /* Cart and Checkout Styles */
        .cart-container, .checkout-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }
        
        .cart-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 15px;
            border-bottom: 1px solid #eee;
        }
        
        .item-info h4 {
            margin: 0 0 5px 0;
        }
        
        .item-controls {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .item-controls .quantity {
            min-width: 30px;
            text-align: center;
            font-weight: bold;
        }
        
        .cart-summary {
            text-align: right;
            padding: 20px 0;
            border-top: 2px solid #e74c3c;
            margin-top: 20px;
        }
        
        .checkout-content {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 40px;
            margin-top: 20px;
        }
        
        .order-summary {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
        }
        
        .order-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
            padding-bottom: 10px;
            border-bottom: 1px solid #dee2e6;
        }
        
        .order-total {
            margin-top: 15px;
            padding-top: 15px;
            border-top: 2px solid #e74c3c;
            font-size: 1.2em;
        }
        
        .order-confirmation {
            text-align: center;
            padding: 40px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }
        
        .order-confirmation h2 {
            color: #2ecc71;
            margin-bottom: 20px;
        }
        
        @media (max-width: 768px) {
            .checkout-content {
                grid-template-columns: 1fr;
                gap: 20px;
            }
            
            .cart-item {
                flex-direction: column;
                align-items: flex-start;
                gap: 10px;
            }
            
            .item-controls {
                justify-content: space-between;
                width: 100%;
            }
        }
    `)
    .appendTo('head');

// Autocomplete Functionality
function initializeAutocomplete() {
    const searchInput = $('#search-input');
    const searchForm = $('#search-form');
    
    // Create dropdown element
    const dropdown = $('<div class="autocomplete-dropdown"></div>');
    searchForm.append(dropdown);
    
    let debounceTimer;
    let cache = {}; // Client-side cache for immediate response
    
    searchInput.on('input', function() {
        const query = $(this).val().trim();
        
        // Clear previous timer
        clearTimeout(debounceTimer);
        
        if (query.length < 3) {
            dropdown.removeClass('show').empty();
            return;
        }
        
        // Check client cache first
        if (cache[query]) {
            displaySuggestions(cache[query], query);
            return;
        }
        
        // Debounce API call (300ms delay)
        debounceTimer = setTimeout(function() {
            console.log(`Fetching autocomplete suggestions for: ${query}`);
            
            $.ajax({
                url: 'api/autocomplete',
                method: 'GET',
                data: { query: query },
                success: function(response) {
                    // Cache the result
                    cache[query] = response.suggestions;
                    displaySuggestions(response.suggestions, query);
                },
                error: function(xhr, status, error) {
                    console.error('Autocomplete error:', error);
                }
            });
        }, 300);
    });
    
    // Hide dropdown when clicking outside
    $(document).on('click', function(e) {
        if (!$(e.target).closest('#search-form').length) {
            dropdown.removeClass('show');
        }
    });
    
    // Handle suggestion selection
    dropdown.on('click', '.autocomplete-item', function() {
        const type = $(this).data('type');
        const id = $(this).data('id');
        const value = $(this).data('value');
        
        if (type === 'movie') {
            loadMovieDetails(id);
        } else if (type === 'star') {
            loadStarDetails(id);
        }
        
        searchInput.val(value);
        dropdown.removeClass('show');
    });
    
    function displaySuggestions(suggestions, query) {
        dropdown.empty();
        
        if (!suggestions || suggestions.length === 0) {
            dropdown.removeClass('show');
            return;
        }
        
        suggestions.forEach(function(item) {
            const iconClass = item.type === 'movie' ? 'fa-film' : 'fa-user';
            const itemHtml = `
                <div class="autocomplete-item" data-type="${item.type}" data-id="${item.id}" data-value="${item.value}">
                    <div class="suggestion-content">
                        <div class="suggestion-icon">
                            <i class="fas ${iconClass}"></i>
                        </div>
                        <div class="suggestion-text">
                            <div class="suggestion-title">${highlightMatch(item.title, query)}</div>
                            <div class="suggestion-subtitle">${item.subtitle}</div>
                        </div>
                    </div>
                </div>
            `;
            dropdown.append(itemHtml);
        });
        
        dropdown.addClass('show');
    }
    
    function highlightMatch(text, query) {
        if (!query) return text;
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<span class="suggestion-highlight">$1</span>');
    }
}
