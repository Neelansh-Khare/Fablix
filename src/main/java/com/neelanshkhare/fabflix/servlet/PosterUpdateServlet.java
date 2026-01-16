package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.util.MoviePosterUtil;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@WebServlet("/api/admin/update-posters")
public class PosterUpdateServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(PosterUpdateServlet.class.getName());
    private MovieService movieService;

    @Override
    public void init() throws ServletException {
        super.init();
        movieService = new MovieService();
    }

    // Simple admin check - you might want to implement proper admin authentication
    private boolean isAdminUser(HttpServletRequest request) {
        // For now, just check if user is logged in
        // In production, you'd check for admin role
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("customerId") != null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Check admin access (basic check for now)
            /*
            if (!isAdminUser(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JSONObject error = new JSONObject();
                error.put("message", "Admin access required");
                out.print(error.toString());
                return;
            }
            */

            // Check if TMDB API key is configured
            if (!MoviePosterUtil.isApiKeyConfigured()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "TMDB API key not configured. Please set your API key in MoviePosterUtil.java");
                out.print(error.toString());
                return;
            }

            // Get parameters
            String movieId = request.getParameter("movieId");
            String batchUpdate = request.getParameter("batch");
            int limit = 10; // Default batch size

            try {
                String limitParam = request.getParameter("limit");
                if (limitParam != null && !limitParam.isEmpty()) {
                    limit = Integer.parseInt(limitParam);
                    limit = Math.min(limit, 50); // Max 50 movies per batch
                }
            } catch (NumberFormatException e) {
                // Use default limit
            }

            int updatedCount = 0;
            int errorCount = 0;

            if (movieId != null && !movieId.isEmpty()) {
                // Update single movie
                Movie movie = movieService.getMovie(movieId);
                if (movie != null) {
                    boolean success = updateMoviePoster(movie);
                    if (success) {
                        updatedCount = 1;
                        LOGGER.info("Updated poster for movie: " + movie.getTitle());
                    } else {
                        errorCount = 1;
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JSONObject error = new JSONObject();
                    error.put("message", "Movie not found");
                    out.print(error.toString());
                    return;
                }
            } else if ("true".equals(batchUpdate)) {
                // Batch update movies without posters
                List<Movie> movies = movieService.getMoviesWithoutPosters(limit);

                LOGGER.info("Starting batch poster update for " + movies.size() + " movies");

                for (Movie movie : movies) {
                    try {
                        boolean success = updateMoviePoster(movie);
                        if (success) {
                            updatedCount++;
                            LOGGER.info("Updated poster for: " + movie.getTitle());
                        } else {
                            errorCount++;
                        }

                        // Add delay to avoid hitting API rate limits (TMDB allows 40 requests per 10 seconds)
                        Thread.sleep(300); // 300ms delay between requests
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.warning("Batch update interrupted");
                        break;
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error updating poster for movie " + movie.getTitle(), e);
                        errorCount++;
                    }
                }

                LOGGER.info("Batch update completed. Updated: " + updatedCount + ", Errors: " + errorCount);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Either movieId or batch=true parameter is required");
                out.print(error.toString());
                return;
            }

            // Return success response
            JSONObject result = new JSONObject();
            result.put("message", "Poster update completed");
            result.put("updatedCount", updatedCount);
            result.put("errorCount", errorCount);
            result.put("totalProcessed", updatedCount + errorCount);

            if (updatedCount > 0) {
                result.put("status", "success");
            } else if (errorCount > 0) {
                result.put("status", "partial_failure");
            } else {
                result.put("status", "no_updates_needed");
                result.put("message", "No movies needed poster updates");
            }

            out.print(result.toString());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in poster update servlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }

    private boolean updateMoviePoster(Movie movie) {
        try {
            LOGGER.info("Searching for poster: " + movie.getTitle() + " (" + movie.getYear() + ")");

            // Search for poster using TMDB API
            MoviePosterUtil.MoviePosterResult posterResult =
                    MoviePosterUtil.searchMoviePoster(movie.getTitle(), movie.getYear());

            if (posterResult != null) {
                boolean movieUpdated = false;

                // Update poster URL if found
                if (posterResult.getPosterUrl() != null) {
                    String oldBannerUrl = movie.getBannerUrl();
                    movie.setBannerUrl(posterResult.getPosterUrl());
                    movieUpdated = true;
                    LOGGER.info("Found poster URL for " + movie.getTitle() + ": " + posterResult.getPosterUrl());
                } else {
                    LOGGER.info("No poster URL found for: " + movie.getTitle());
                }

                // Update trailer URL if available
                if (posterResult.getTrailerUrl() != null &&
                        (movie.getTrailerUrl() == null || movie.getTrailerUrl().isEmpty())) {
                    movie.setTrailerUrl(posterResult.getTrailerUrl());
                    movieUpdated = true;
                    LOGGER.info("Added trailer URL for " + movie.getTitle());
                }

                // Save updated movie if any changes were made
                if (movieUpdated) {
                    boolean success = movieService.updateMovie(movie);
                    if (success) {
                        LOGGER.info("Successfully updated movie in database: " + movie.getTitle());
                        return true;
                    } else {
                        LOGGER.warning("Failed to save updated movie to database: " + movie.getTitle());
                    }
                } else {
                    LOGGER.info("No updates needed for movie: " + movie.getTitle());
                    return false;
                }
            } else {
                LOGGER.info("No TMDB results found for: " + movie.getTitle() + " (" + movie.getYear() + ")");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating poster for " + movie.getTitle(), e);
        }

        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Return configuration and status information
            JSONObject status = new JSONObject();
            status.put("tmdbApiConfigured", MoviePosterUtil.isApiKeyConfigured());
            status.put("message", MoviePosterUtil.isApiKeyConfigured() ?
                    "TMDB API is configured and ready" :
                    "TMDB API key not configured. Get your free key from https://www.themoviedb.org/settings/api");

            // Add some stats
            int totalMovies = movieService.getTotalMoviesCount();
            status.put("totalMovies", totalMovies);

            // Check admin access
            boolean isAdmin = isAdminUser(request);
            status.put("adminAccess", isAdmin);

            if (!isAdmin) {
                status.put("adminMessage", "Please log in to access admin features");
            }

            out.print(status.toString());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking poster service status", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error checking poster service status: " + e.getMessage());
            out.print(error.toString());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Handle CORS preflight requests if needed
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}