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

    // Add poster admin link (you can remove this in production)
    addPosterAdminLink();
});

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
            } else {
                // User is not logged in
                $('#login-section').show();
                $('#account-section').hide();
            }
        },
        error: function() {
            // Error checking login status
            console.error('Error checking login status');
        }
    });
}

function loadHomePage() {
    // Add cache busting parameter to ensure fresh data
    const timestamp = new Date().getTime();

    // Fetch featured movies
    $.ajax({
        url: 'api/movies',
        method: 'GET',
        cache: false,  // Disable jQuery cache
        data: {
            page: 1,
            pageSize: 12,
            _t: timestamp  // Cache busting parameter
        },
        success: function(response) {
            console.log('Movies loaded:', response.movies.length);
            // Log first movie to check poster URL
            if (response.movies.length > 0) {
                console.log('First movie poster URL:', response.movies[0].bannerUrl);
            }
            displayMovies(response.movies);
        },
        error: function(xhr, status, error) {
            console.error('Error loading movies:', error);
            showErrorMessage('Error loading movies. Please try again later.');
        }
    });
}

// Also add a debug function to check poster status
function checkPosterStatus() {
    $.ajax({
        url: 'api/debug/posters',
        method: 'GET',
        cache: false,
        success: function(response) {
            console.log('=== POSTER DEBUG INFO ===');
            console.log('Cache size:', response.cacheSize);
            console.log('Movies with valid posters:');

            response.movies.forEach(function(movie) {
                console.log(`${movie.title} (${movie.year}):`,
                    movie.hasValidPoster ? '✅ HAS POSTER' : '❌ NO POSTER',
                    movie.bannerUrl);
            });

            console.log('=== END DEBUG INFO ===');
        },
        error: function() {
            console.error('Failed to get poster debug info');
        }
    });
}

// Enhanced displayMovies with better debugging
function displayMovies(movies) {
    const container = $('#content-container');
    container.empty();

    // Add header with debug button
    container.append(`
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2>Featured Movies</h2>
            <button onclick="checkPosterStatus()" style="padding: 5px 10px; background: #3498db; color: white; border: none; border-radius: 3px; cursor: pointer;">
                Debug Posters
            </button>
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

        console.log(`Movie: ${movie.title}, Poster URL: ${movie.bannerUrl}, Valid: ${hasValidPoster}`);

        const card = `
            <div class="movie-card" data-id="${movie.id}">
                <div class="movie-poster">
                    <img src="${hasValidPoster ? movie.bannerUrl : 'images/no-poster.jpg'}" 
                         alt="${movie.title} poster" 
                         onerror="this.src='images/no-poster.jpg'; console.error('Failed to load poster for ${movie.title}:', this.src);"
                         onload="console.log('Successfully loaded poster for ${movie.title}');">
                    ${!hasValidPoster ? '<div class="poster-loading-indicator">🔄 Poster loading...</div>' : ''}
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

    // Start progressive poster loading for movies without posters
    if (movies.some(m => !m.bannerUrl || m.bannerUrl.includes('no-poster.jpg'))) {
        startProgressivePosterLoading();
    }
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
        performLogin(email, password);
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
        const passwordValidation = validatePassword(formData.password);
        if (!passwordValidation.valid) {
            showErrorMessage(passwordValidation.message);
            return;
        }

        // Use the enhanced registration function from auth.js
        performRegistration(formData);
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

// Shopping Cart Functions
function addToCart(movieId, movieTitle) {
    let cart = JSON.parse(localStorage.getItem('cart')) || [];

    // Check if movie is already in cart
    const existingItem = cart.find(item => item.id === movieId);

    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({
            id: movieId,
            title: movieTitle,
            quantity: 1,
            price: 9.99 // Default price
        });
    }

    localStorage.setItem('cart', JSON.stringify(cart));
    showSuccessMessage(`${movieTitle} added to cart!`);
    updateCartCount();
}

function updateCartCount() {
    const cart = JSON.parse(localStorage.getItem('cart')) || [];
    const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

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

function loadCart() {
    const cart = JSON.parse(localStorage.getItem('cart')) || [];
    const container = $('#content-container');
    container.empty();

    if (cart.length === 0) {
        container.append(`
            <div class="cart-container">
                <h2>Shopping Cart</h2>
                <p>Your cart is empty.</p>
                <button class="btn btn-primary" onclick="loadHomePage()">Continue Shopping</button>
            </div>
        `);
        return;
    }

    let total = 0;
    let cartHtml = `
        <div class="cart-container">
            <h2>Shopping Cart</h2>
            <div class="cart-items">
    `;

    cart.forEach(item => {
        const itemTotal = item.price * item.quantity;
        total += itemTotal;

        cartHtml += `
            <div class="cart-item" data-id="${item.id}">
                <div class="item-info">
                    <h4>${item.title}</h4>
                    <p>Price: $${item.price.toFixed(2)}</p>
                </div>
                <div class="item-controls">
                    <button class="btn btn-sm decrease-qty" data-id="${item.id}">-</button>
                    <span class="quantity">${item.quantity}</span>
                    <button class="btn btn-sm increase-qty" data-id="${item.id}">+</button>
                    <button class="btn btn-danger remove-item" data-id="${item.id}">Remove</button>
                </div>
                <div class="item-total">$${itemTotal.toFixed(2)}</div>
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

    // Bind cart event listeners
    bindCartEventListeners();
}

function bindCartEventListeners() {
    $('.increase-qty').click(function() {
        const movieId = $(this).data('id');
        updateCartQuantity(movieId, 1);
    });

    $('.decrease-qty').click(function() {
        const movieId = $(this).data('id');
        updateCartQuantity(movieId, -1);
    });

    $('.remove-item').click(function() {
        const movieId = $(this).data('id');
        removeFromCart(movieId);
    });
}

function updateCartQuantity(movieId, change) {
    let cart = JSON.parse(localStorage.getItem('cart')) || [];
    const item = cart.find(item => item.id === movieId);

    if (item) {
        item.quantity += change;
        if (item.quantity <= 0) {
            cart = cart.filter(item => item.id !== movieId);
        }
    }

    localStorage.setItem('cart', JSON.stringify(cart));
    loadCart();
    updateCartCount();
}

function removeFromCart(movieId) {
    let cart = JSON.parse(localStorage.getItem('cart')) || [];
    cart = cart.filter(item => item.id !== movieId);
    localStorage.setItem('cart', JSON.stringify(cart));
    loadCart();
    updateCartCount();
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
    const cart = JSON.parse(localStorage.getItem('cart')) || [];
    if (cart.length === 0) {
        showErrorMessage('Your cart is empty.');
        return;
    }

    const container = $('#content-container');
    container.empty();

    let total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    const checkoutHtml = `
        <div class="checkout-container">
            <h2>Checkout</h2>
            <div class="checkout-content">
                <div class="order-summary">
                    <h3>Order Summary</h3>
                    ${cart.map(item => `
                        <div class="order-item">
                            <span>${item.title} x ${item.quantity}</span>
                            <span>$${(item.price * item.quantity).toFixed(2)}</span>
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
    const cart = JSON.parse(localStorage.getItem('cart')) || [];
    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    const orderData = {
        items: cart,
        total: total,
        creditCard: $('#checkout-cc').val(),
        expirationDate: $('#checkout-exp').val(),
        cvv: $('#checkout-cvv').val(),
        cardholderName: $('#checkout-name').val()
    };

    // Simulate payment processing
    showSuccessMessage('Processing payment...');

    setTimeout(function() {
        // Clear cart
        localStorage.removeItem('cart');
        updateCartCount();

        // Show success message
        const container = $('#content-container');
        container.empty();
        container.append(`
            <div class="order-confirmation">
                <h2>Order Confirmed!</h2>
                <p>Thank you for your purchase. Your order has been processed successfully.</p>
                <p>Order Total: $${total.toFixed(2)}</p>
                <button class="btn btn-primary" onclick="loadHomePage()">Continue Shopping</button>
            </div>
        `);

        showSuccessMessage('Payment successful! Thank you for your purchase.');
    }, 2000);
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

    // Create movie detail section
    const detailHtml = `
        <div class="movie-detail">
            <div class="movie-detail-poster">
                <img src="${movie.bannerUrl || 'images/no-poster.jpg'}" alt="${movie.title} poster" onerror="this.src='images/no-poster.jpg'">
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