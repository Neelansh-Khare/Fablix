package com.neelanshkhare.fabflix.listener;

import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.service.StarService;
import com.neelanshkhare.fabflix.service.TmdbIngestService;
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
 * On startup (after a short delay) and every PERIODIC_HOURS hours:
 *   1. Pull posters for any movies in the DB that don't have one.
 *   2. Pull profile photos for any actors that don't have one.
 *   3. Ingest new movies from TMDB discover (with posters + cast inline).
 *
 * New movies ingested via TMDB already have their poster set, so they skip
 * step 1 on subsequent runs.
 */
@WebListener
public class PosterPopulationListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(PosterPopulationListener.class.getName());

    private static final int STARTUP_DELAY_SECONDS = 10;
    private static final int PERIODIC_HOURS = 6;
    private static final int DELAY_BETWEEN_REQUESTS_MS = 300;
    private static final int DISCOVER_PAGES = 5; // 100 movies per run

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("PosterPopulationListener initializing");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PosterPopulation-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.schedule(this::runAll, STARTUP_DELAY_SECONDS, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::runAll, PERIODIC_HOURS, PERIODIC_HOURS, TimeUnit.HOURS);

        LOGGER.info("Poster population scheduled every " + PERIODIC_HOURS + " hours");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        MovieService.shutdown();
        LOGGER.info("PosterPopulationListener stopped");
    }

    private void runAll() {
        if (!isRunning.compareAndSet(false, true)) {
            LOGGER.info("Poster population already running, skipping");
            return;
        }
        try {
            if (!MoviePosterUtil.isApiKeyConfigured()) {
                LOGGER.warning("TMDB API key not configured — skipping all poster tasks");
                return;
            }
            populateMoviePosters();
            populateActorPhotos();
            ingestNewMovies();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during poster population run", e);
        } finally {
            isRunning.set(false);
        }
    }

    // ------------------------------------------------------------------
    // Step 1: posters for movies already in DB
    // ------------------------------------------------------------------

    private void populateMoviePosters() throws InterruptedException {
        MovieService movieService = new MovieService();
        List<Movie> movies = movieService.getAllMoviesWithoutPosters();

        if (movies.isEmpty()) {
            LOGGER.info("All movies already have posters");
            return;
        }

        LOGGER.info("Fetching posters for " + movies.size() + " movies");
        int updated = 0, errors = 0;

        for (Movie movie : movies) {
            try {
                MoviePosterResult result = MoviePosterUtil.searchMoviePoster(movie.getTitle(), movie.getYear());
                if (result != null && result.getPosterUrl() != null) {
                    movieService.updateMoviePosterFields(
                        movie.getId(),
                        result.getPosterUrl(),
                        result.getTrailerUrl(),
                        result.getRating(),
                        result.getNumVotes()
                    );
                    updated++;
                } else {
                    movieService.updateMoviePosterFields(movie.getId(), "poster_not_found", null, 0.0, 0);
                }
            } catch (Exception e) {
                errors++;
                LOGGER.log(Level.WARNING, "Error updating poster for: " + movie.getTitle(), e);
            }
            Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
        }

        LOGGER.info("Movie posters: " + updated + " updated, " + errors + " errors");
    }

    // ------------------------------------------------------------------
    // Step 2: profile photos for actors already in DB
    // ------------------------------------------------------------------

    private void populateActorPhotos() throws InterruptedException {
        StarService starService = new StarService();
        List<Star> stars = starService.getStarsWithoutPhotos();

        if (stars.isEmpty()) {
            LOGGER.info("All actors already have photos");
            return;
        }

        LOGGER.info("Fetching photos for " + stars.size() + " actors");
        int updated = 0, errors = 0;

        for (Star star : stars) {
            try {
                String photoUrl = MoviePosterUtil.searchPersonPhoto(star.getName());
                if (photoUrl != null) {
                    starService.updateStarPhotoUrl(star.getId(), photoUrl);
                    updated++;
                } else {
                    starService.updateStarPhotoUrl(star.getId(), "photo_not_found");
                }
            } catch (Exception e) {
                errors++;
                LOGGER.log(Level.WARNING, "Error updating photo for: " + star.getName(), e);
            }
            Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
        }

        LOGGER.info("Actor photos: " + updated + " updated, " + errors + " errors");
    }

    // ------------------------------------------------------------------
    // Step 3: ingest new movies from TMDB discover
    // ------------------------------------------------------------------

    private void ingestNewMovies() {
        TmdbIngestService ingestService = new TmdbIngestService();
        TmdbIngestService.IngestStats stats = ingestService.ingestDiscoverMovies(DISCOVER_PAGES);
        LOGGER.info("TMDB discover ingestion: " + stats);
    }
}
