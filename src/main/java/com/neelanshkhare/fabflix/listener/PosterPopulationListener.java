package com.neelanshkhare.fabflix.listener;

import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.util.MoviePosterUtil;
import com.neelanshkhare.fabflix.util.MoviePosterUtil.MoviePosterResult;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServletContextListener that automatically populates movie posters on application startup.
 *
 * Features:
 * - Scans for movies without posters on startup
 * - Asynchronously fetches posters from TMDB (respecting rate limits)
 * - Optional periodic check for new movies without posters
 * - Graceful shutdown handling
 */
@WebListener
public class PosterPopulationListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(PosterPopulationListener.class.getName());

    // Configuration
    private static final int STARTUP_DELAY_SECONDS = 10;      // Wait for app to fully initialize
    private static final int BATCH_SIZE = 50;                  // Movies to process per batch
    private static final int DELAY_BETWEEN_REQUESTS_MS = 300;  // Respect TMDB rate limit (40 req/10 sec)
    private static final int PERIODIC_CHECK_HOURS = 6;         // Check for new movies every N hours
    private static final boolean ENABLE_PERIODIC_CHECK = true;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("PosterPopulationListener initialized - scheduling poster population task");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PosterPopulation-Thread");
            t.setDaemon(true);
            return t;
        });

        // Schedule initial run after startup delay
        scheduler.schedule(this::populatePosters, STARTUP_DELAY_SECONDS, TimeUnit.SECONDS);

        // Schedule periodic checks if enabled
        if (ENABLE_PERIODIC_CHECK) {
            scheduler.scheduleAtFixedRate(
                this::populatePosters,
                PERIODIC_CHECK_HOURS,
                PERIODIC_CHECK_HOURS,
                TimeUnit.HOURS
            );
            LOGGER.info("Periodic poster check scheduled every " + PERIODIC_CHECK_HOURS + " hours");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("PosterPopulationListener shutting down");

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    LOGGER.warning("Poster population scheduler forced shutdown");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Also shutdown MovieService's executor
        MovieService.shutdown();

        LOGGER.info("PosterPopulationListener shutdown complete");
    }

    /**
     * Main poster population logic - runs asynchronously
     */
    private void populatePosters() {
        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            LOGGER.info("Poster population already in progress, skipping");
            return;
        }

        try {
            LOGGER.info("Starting poster population task");

            // Check if TMDB API is configured
            if (!MoviePosterUtil.isApiKeyConfigured()) {
                LOGGER.warning("TMDB API key not configured - skipping poster population");
                return;
            }

            // Test API connection
            if (!MoviePosterUtil.testApiConnection()) {
                LOGGER.warning("TMDB API connection test failed - skipping poster population");
                return;
            }

            MovieService movieService = new MovieService();
            int totalUpdated = 0;
            int totalErrors = 0;
            int totalProcessed = 0;

            // Process in batches
            List<Movie> moviesWithoutPosters;
            do {
                moviesWithoutPosters = movieService.getMoviesWithoutPosters(BATCH_SIZE);

                if (moviesWithoutPosters.isEmpty()) {
                    LOGGER.info("No movies without posters found");
                    break;
                }

                LOGGER.info("Processing batch of " + moviesWithoutPosters.size() + " movies without posters");

                for (Movie movie : moviesWithoutPosters) {
                    totalProcessed++;

                    try {
                        boolean updated = updateMoviePoster(movie, movieService);
                        if (updated) {
                            totalUpdated++;
                            LOGGER.fine("Updated poster for: " + movie.getTitle() + " (" + movie.getYear() + ")");
                        }
                    } catch (Exception e) {
                        totalErrors++;
                        LOGGER.log(Level.WARNING, "Error updating poster for: " + movie.getTitle(), e);
                    }

                    // Rate limiting - respect TMDB API limits
                    Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
                }

                // Log progress after each batch
                LOGGER.info("Progress: " + totalProcessed + " processed, " +
                           totalUpdated + " updated, " + totalErrors + " errors");

            } while (!moviesWithoutPosters.isEmpty() && moviesWithoutPosters.size() == BATCH_SIZE);

            LOGGER.info("Poster population complete: " + totalProcessed + " processed, " +
                       totalUpdated + " updated, " + totalErrors + " errors");

        } catch (InterruptedException e) {
            LOGGER.info("Poster population interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during poster population", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Update a single movie's poster from TMDB
     */
    private boolean updateMoviePoster(Movie movie, MovieService movieService) {
        MoviePosterResult result = MoviePosterUtil.searchMoviePoster(
            movie.getTitle(),
            movie.getYear()
        );

        if (result == null || result.getPosterUrl() == null) {
            // Mark as not found to prevent future retries
            movie.setBannerUrl("poster_not_found");
            movieService.updateMovie(movie);
            return false;
        }

        // Update poster URL
        movie.setBannerUrl(result.getPosterUrl());

        // Update trailer if available and not already set
        String trailerUrl = movie.getTrailerUrl();
        if ((trailerUrl == null || trailerUrl.isEmpty()) && result.getTrailerUrl() != null) {
            movie.setTrailerUrl(result.getTrailerUrl());
        }

        return movieService.updateMovie(movie);
    }
}
