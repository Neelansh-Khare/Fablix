// autocomplete.js - Intelligent search with autocomplete

function escapeHtml(text) {
    return $('<div>').text(text || '').html();
}

let autocompleteTimeout;
let currentSuggestions = [];
let selectedSuggestionIndex = -1;

function initializeAutocomplete() {
    const searchInput = $('#search-input');
    const searchForm = $('#search-form');

    // Create autocomplete container
    if ($('#autocomplete-container').length === 0) {
        searchInput.parent().append('<div id="autocomplete-container" class="autocomplete-dropdown"></div>');
    }

    // Bind input events
    searchInput.on('input', handleSearchInput);
    searchInput.on('keydown', handleSearchKeydown);
    searchInput.on('blur', handleSearchBlur);
    searchInput.on('focus', handleSearchFocus);

    // Prevent form submission when selecting suggestion
    searchForm.on('submit', handleSearchSubmit);

    // Hide suggestions when clicking outside
    $(document).on('click', function(e) {
        if (!$(e.target).closest('.search-container').length) {
            hideAutocomplete();
        }
    });
}

function handleSearchInput(e) {
    const query = $(this).val().trim();

    // Clear previous timeout
    if (autocompleteTimeout) {
        clearTimeout(autocompleteTimeout);
    }

    if (query.length < 2) {
        hideAutocomplete();
        return;
    }

    // Debounce the search to avoid too many requests
    autocompleteTimeout = setTimeout(function() {
        fetchAutocompleteSuggestions(query);
    }, 300);
}

function handleSearchKeydown(e) {
    const suggestions = $('.autocomplete-item');

    switch(e.keyCode) {
        case 38: // Up arrow
            e.preventDefault();
            if (selectedSuggestionIndex > 0) {
                selectedSuggestionIndex--;
                updateSuggestionSelection();
            }
            break;

        case 40: // Down arrow
            e.preventDefault();
            if (selectedSuggestionIndex < suggestions.length - 1) {
                selectedSuggestionIndex++;
                updateSuggestionSelection();
            }
            break;

        case 13: // Enter
            if (selectedSuggestionIndex >= 0 && suggestions.length > 0) {
                e.preventDefault();
                selectSuggestion(selectedSuggestionIndex);
            }
            break;

        case 27: // Escape
            hideAutocomplete();
            break;
    }
}

function handleSearchBlur(e) {
    // Delay hiding to allow for suggestion clicks
    setTimeout(function() {
        hideAutocomplete();
    }, 150);
}

function handleSearchFocus(e) {
    const query = $(this).val().trim();
    if (query.length >= 2 && currentSuggestions.length > 0) {
        showAutocomplete();
    }
}

function handleSearchSubmit(e) {
    if (selectedSuggestionIndex >= 0) {
        e.preventDefault();
        selectSuggestion(selectedSuggestionIndex);
        return false;
    }

    // Allow normal form submission for regular search
    const query = $('#search-input').val().trim();
    if (query) {
        e.preventDefault();
        performSearch(query);
    }
}

function fetchAutocompleteSuggestions(query) {
    $.ajax({
        url: 'api/autocomplete',
        method: 'GET',
        data: {
            query: query,
            limit: 8
        },
        success: function(response) {
            currentSuggestions = response.suggestions || [];
            displayAutocompleteSuggestions(currentSuggestions);
        },
        error: function() {
            console.error('Error fetching autocomplete suggestions');
            hideAutocomplete();
        }
    });
}

function displayAutocompleteSuggestions(suggestions) {
    const container = $('#autocomplete-container');
    container.empty();

    if (suggestions.length === 0) {
        hideAutocomplete();
        return;
    }

    suggestions.forEach(function(suggestion, index) {
        const item = $('<div class="autocomplete-item"></div>')
            .attr('data-index', index)
            .attr('data-type', suggestion.type)
            .attr('data-id', suggestion.id);

        const icon = suggestion.type === 'movie' ? '🎬' : '⭐';

        item.html(`
            <div class="suggestion-content">
                <span class="suggestion-icon">${icon}</span>
                <div class="suggestion-text">
                    <div class="suggestion-title">${escapeHtml(suggestion.title)}</div>
                    <div class="suggestion-subtitle">${escapeHtml(suggestion.subtitle)}</div>
                </div>
            </div>
        `);

        item.on('click', function() {
            selectSuggestion(index);
        });

        item.on('mouseenter', function() {
            selectedSuggestionIndex = index;
            updateSuggestionSelection();
        });

        container.append(item);
    });

    selectedSuggestionIndex = -1;
    showAutocomplete();
}

function updateSuggestionSelection() {
    $('.autocomplete-item').removeClass('selected');
    if (selectedSuggestionIndex >= 0) {
        $('.autocomplete-item').eq(selectedSuggestionIndex).addClass('selected');
    }
}

function selectSuggestion(index) {
    if (index < 0 || index >= currentSuggestions.length) return;

    const suggestion = currentSuggestions[index];

    // Update search input
    $('#search-input').val(suggestion.value);

    // Hide autocomplete
    hideAutocomplete();

    // Navigate based on suggestion type
    if (suggestion.type === 'movie') {
        loadMovieDetails(suggestion.id);
    } else if (suggestion.type === 'star') {
        loadStarDetails(suggestion.id);
    }
}

function showAutocomplete() {
    $('#autocomplete-container').addClass('show');
}

function hideAutocomplete() {
    $('#autocomplete-container').removeClass('show');
    selectedSuggestionIndex = -1;
}

function performSearch(query) {
    // Existing search functionality
    $.ajax({
        url: 'api/search',
        method: 'GET',
        data: {
            query: query
        },
        success: function(response) {
            hideAutocomplete();
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

// Initialize autocomplete when document is ready
$(document).ready(function() {
    initializeAutocomplete();
});