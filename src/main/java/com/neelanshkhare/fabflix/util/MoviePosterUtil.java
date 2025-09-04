package com.neelanshkhare.fabflix.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoviePosterUtil {
    private static final Logger LOGGER = Logger.getLogger(MoviePosterUtil.class.getName());

    // Replace with your actual TMDB API key from https://www.themoviedb.org/settings/api
    private static final String TMDB_API_KEY = "ABC";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String TMDB_SEARCH_URL = TMDB_BASE_URL + "/search/movie";

    // Rate limiting
    private static long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 300; // 300ms between requests

    public static class MoviePosterResult {
        private String posterUrl;
        private String backdropUrl;
        private String overview;
        private double rating;
        private int tmdbId;

        public MoviePosterResult() {}

        // Getters and setters
        public String getPosterUrl() { return posterUrl; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

        public String getBackdropUrl() { return backdropUrl; }
        public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }

        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }

        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }

        public int getTmdbId() { return tmdbId; }
        public void setTmdbId(int tmdbId) { this.tmdbId = tmdbId; }
    }

    public static MoviePosterResult searchMoviePoster(String movieTitle, Integer year) {
        if (!isApiKeyConfigured()) {
            LOGGER.warning("TMDB API key not configured properly");
            return null;
        }

        // Input validation
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            LOGGER.warning("Movie title is empty or null");
            return null;
        }

        try {
            // Rate limiting to respect TMDB API limits
            enforceRateLimit();

            // Clean and prepare movie title for search
            String cleanTitle = cleanMovieTitle(movieTitle);

            // Build search URL
            StringBuilder urlBuilder = new StringBuilder(TMDB_SEARCH_URL);
            urlBuilder.append("?api_key=").append(TMDB_API_KEY);
            urlBuilder.append("&query=").append(URLEncoder.encode(cleanTitle, "UTF-8"));

            if (year != null && year > 1800 && year < 2100) {
                urlBuilder.append("&year=").append(year);
            }

            String searchUrl = urlBuilder.toString();
            LOGGER.fine("Searching TMDB: " + cleanTitle + (year != null ? " (" + year + ")" : ""));

            // Make HTTP request with proper timeout and headers
            HttpURLConnection connection = createConnection(searchUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                String responseBody = readResponse(connection);
                return parseSearchResponse(responseBody, movieTitle, year);

            } else if (responseCode == 401) {
                LOGGER.severe("TMDB API authentication failed - check your API key");
                return null;

            } else if (responseCode == 429) {
                LOGGER.warning("TMDB API rate limit exceeded - waiting before retry");
                // Could implement exponential backoff here
                return null;

            } else {
                LOGGER.warning("TMDB API request failed with code: " + responseCode);
                return null;
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IO error fetching movie poster from TMDB for: " + movieTitle, e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error in movie poster search for: " + movieTitle, e);
        }

        return null;
    }

    private static void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;

        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
            try {
                Thread.sleep(MIN_REQUEST_INTERVAL - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    private static String cleanMovieTitle(String title) {
        if (title == null) return "";

        // Remove common prefixes/suffixes that might interfere with search
        String cleaned = title.trim();

        // Remove year in parentheses if present
        cleaned = cleaned.replaceAll("\\s*\\(\\d{4}\\)\\s*", "");

        // Remove "The " prefix for better matching (TMDB handles this)
        // cleaned = cleaned.replaceAll("^The\\s+", "");

        return cleaned;
    }

    private static HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "FabFlix-MovieApp/1.0");
        connection.setConnectTimeout(5000);  // 5 seconds
        connection.setReadTimeout(10000);    // 10 seconds

        return connection;
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static MoviePosterResult parseSearchResponse(String responseBody, String originalTitle, Integer year) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() == 0) {
                LOGGER.fine("No TMDB results found for: " + originalTitle);
                return null;
            }

            // Find best match - prefer exact year match if year is provided
            JSONObject bestMatch = findBestMatch(results, originalTitle, year);

            if (bestMatch != null) {
                MoviePosterResult result = parseMovieResult(bestMatch);
                LOGGER.fine("Found poster for: " + originalTitle + " -> " +
                        (result.getPosterUrl() != null ? "Success" : "No poster URL"));
                return result;
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing TMDB response for: " + originalTitle, e);
        }

        return null;
    }

    private static JSONObject findBestMatch(JSONArray results, String originalTitle, Integer year) {
        JSONObject exactYearMatch = null;
        JSONObject firstResult = null;

        for (int i = 0; i < results.length(); i++) {
            JSONObject movie = results.getJSONObject(i);

            if (firstResult == null) {
                firstResult = movie;
            }

            // If year is specified, try to find exact year match
            if (year != null && movie.has("release_date")) {
                String releaseDate = movie.getString("release_date");
                if (releaseDate != null && !releaseDate.isEmpty() && releaseDate.startsWith(year.toString())) {
                    exactYearMatch = movie;
                    break;
                }
            }
        }

        // Return exact year match if found, otherwise return first result
        return exactYearMatch != null ? exactYearMatch : firstResult;
    }

    private static MoviePosterResult parseMovieResult(JSONObject movieJson) {
        MoviePosterResult result = new MoviePosterResult();

        try {
            // Set TMDB ID
            if (movieJson.has("id")) {
                result.setTmdbId(movieJson.getInt("id"));
            }

            // Set poster URL
            if (movieJson.has("poster_path") && !movieJson.isNull("poster_path")) {
                String posterPath = movieJson.getString("poster_path");
                if (posterPath != null && !posterPath.isEmpty()) {
                    result.setPosterUrl(TMDB_IMAGE_BASE_URL + posterPath);
                }
            }

            // Set backdrop URL
            if (movieJson.has("backdrop_path") && !movieJson.isNull("backdrop_path")) {
                String backdropPath = movieJson.getString("backdrop_path");
                if (backdropPath != null && !backdropPath.isEmpty()) {
                    result.setBackdropUrl(TMDB_IMAGE_BASE_URL + backdropPath);
                }
            }

            // Set overview
            if (movieJson.has("overview") && !movieJson.isNull("overview")) {
                result.setOverview(movieJson.getString("overview"));
            }

            // Set rating
            if (movieJson.has("vote_average")) {
                result.setRating(movieJson.getDouble("vote_average"));
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing TMDB movie result", e);
        }

        return result;
    }

    // Method to get high-resolution poster URL
    public static String getHighResPosterUrl(String posterPath) {
        if (posterPath != null && !posterPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w780" + posterPath;
        }
        return null;
    }

    // Method to get thumbnail poster URL
    public static String getThumbnailPosterUrl(String posterPath) {
        if (posterPath != null && !posterPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w300" + posterPath;
        }
        return null;
    }

    // Method to check if API key is configured
    public static boolean isApiKeyConfigured() {
        return TMDB_API_KEY != null &&
                !TMDB_API_KEY.trim().isEmpty();
    }

    // Method to test API connectivity
    public static boolean testApiConnection() {
        try {
            // Test with a simple movie search
            MoviePosterResult result = searchMoviePoster("The Matrix", 1999);
            return result != null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "API connection test failed", e);
            return false;
        }
    }
}