package com.neelanshkhare.fabflix.service;

import com.neelanshkhare.fabflix.dao.MovieDAO;
import com.neelanshkhare.fabflix.dao.impl.MovieDAOImpl;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.util.MoviePosterUtil;
import com.neelanshkhare.fabflix.util.MoviePosterUtil.MoviePosterResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MovieService {
    private static final Logger LOGGER = Logger.getLogger(MovieService.class.getName());
    private MovieDAO movieDAO;

    // Cache to prevent duplicate poster fetching for the same movie
    private static final ConcurrentHashMap<String, Boolean> posterFetchCache = new ConcurrentHashMap<>();

    // Thread pool for background poster fetching
    private static final ExecutorService posterFetchExecutor = Executors.newFixedThreadPool(3);

    public MovieService() {
        this.movieDAO = new MovieDAOImpl();
    }

    public Movie getMovie(String id) {
        Movie movie = movieDAO.findById(id);
        if (movie != null) {
            // Check and fetch poster if needed
            checkAndFetchPoster(movie);
        }
        return movie;
    }

    public List<Movie> searchMovies(String query) {
        List<Movie> movies = movieDAO.searchMovies(query);
        // Fetch posters for movies without them
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesByTitle(String title) {
        List<Movie> movies = movieDAO.findByTitle(title);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesByDirector(String director) {
        List<Movie> movies = movieDAO.findByDirector(director);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesByYear(int year) {
        List<Movie> movies = movieDAO.findByYear(year);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesByGenre(int genreId) {
        List<Movie> movies = movieDAO.findByGenre(genreId);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesByStar(String starId) {
        List<Movie> movies = movieDAO.findByStar(starId);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> listMovies(int page, int pageSize) {
        List<Movie> movies = movieDAO.listMovies(page, pageSize);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getSimilarMovies(String movieId, int limit) {
        List<Movie> movies = movieDAO.getSimilarMovies(movieId, limit);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getCoPurchaseRecommendations(String movieId, int limit) {
        List<Movie> movies = movieDAO.getCoPurchaseRecommendations(movieId, limit);
        fetchPostersForMovieList(movies);
        return movies;
    }

    public List<Movie> getMoviesWithoutPosters(int limit) {
        return movieDAO.getMoviesWithoutPosters(limit);
    }

    public int getTotalMoviesCount() {
        return movieDAO.countMovies();
    }

    public String addMovieWithProcedure(String title, int year, String director, String starName, String genreName) {
        return movieDAO.addMovieWithProcedure(title, year, director, starName, genreName);
    }

    public boolean addMovie(Movie movie) {
        boolean success = movieDAO.insert(movie);
        if (success) {
            // Fetch poster for newly added movie
            checkAndFetchPoster(movie);
        }
        return success;
    }

    public boolean updateMovie(Movie movie) {
        return movieDAO.update(movie);
    }

    public boolean deleteMovie(String id) {
        // Remove from cache if exists
        posterFetchCache.remove(id);
        return movieDAO.delete(id);
    }

    /**
     * Check if a movie needs a poster and fetch it asynchronously
     */
    private void checkAndFetchPoster(Movie movie) {
        if (shouldFetchPoster(movie)) {
            fetchPosterAsync(movie);
        }
    }

    /**
     * Fetch posters for a list of movies
     */
    private void fetchPostersForMovieList(List<Movie> movies) {
        for (Movie movie : movies) {
            checkAndFetchPoster(movie);
        }
    }

    /**
     * Determine if a movie needs a poster
     */
    private boolean shouldFetchPoster(Movie movie) {
        // Don't fetch if already in cache (prevents duplicate requests)
        if (posterFetchCache.containsKey(movie.getId())) {
            return false;
        }

        // Don't fetch if API is not configured
        if (!MoviePosterUtil.isApiKeyConfigured()) {
            return false;
        }

        // Fetch if no poster URL or has placeholder/default image
        String bannerUrl = movie.getBannerUrl();
        return bannerUrl == null ||
                bannerUrl.isEmpty() ||
                bannerUrl.contains("no-poster.jpg") ||
                bannerUrl.contains("placeholder") ||
                bannerUrl.contains("default");
    }

    /**
     * Fetch poster asynchronously to avoid blocking the main request
     */
    private void fetchPosterAsync(Movie movie) {
        // Mark as being processed to prevent duplicate requests
        posterFetchCache.put(movie.getId(), true);

        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Fetching poster for movie: " + movie.getTitle());

                MoviePosterResult posterResult = MoviePosterUtil.searchMoviePoster(
                        movie.getTitle(),
                        movie.getYear()
                );

                if (posterResult != null && posterResult.getPosterUrl() != null) {
                    // Update movie with new poster URL
                    movie.setBannerUrl(posterResult.getPosterUrl());

                    // Update trailer if available
                    if (posterResult.getTrailerUrl() != null && !posterResult.getTrailerUrl().isEmpty()) {
                        movie.setTrailerUrl(posterResult.getTrailerUrl());
                    }

                    // Save to database
                    boolean updated = movieDAO.update(movie);

                    if (updated) {
                        LOGGER.info("Successfully updated poster for: " + movie.getTitle());
                    } else {
                        LOGGER.warning("Failed to save poster update for: " + movie.getTitle());
                    }
                } else {
                    LOGGER.info("No poster found for: " + movie.getTitle());
                    // Mark as not found to prevent infinite retries
                    movie.setBannerUrl("poster_not_found");
                    movieDAO.update(movie);
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error fetching poster for " + movie.getTitle(), e);
            }
        }, posterFetchExecutor);
    }

    /**
     * Force refresh poster for a specific movie (useful for admin operations)
     */
    public boolean refreshMoviePoster(String movieId) {
        Movie movie = movieDAO.findById(movieId);
        if (movie == null) {
            return false;
        }

        // Remove from cache to force refresh
        posterFetchCache.remove(movieId);

        try {
            MoviePosterResult posterResult = MoviePosterUtil.searchMoviePoster(
                    movie.getTitle(),
                    movie.getYear()
            );

            if (posterResult != null && posterResult.getPosterUrl() != null) {
                movie.setBannerUrl(posterResult.getPosterUrl());

                if (posterResult.getTrailerUrl() != null) {
                    movie.setTrailerUrl(posterResult.getTrailerUrl());
                }

                return movieDAO.update(movie);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error refreshing poster for " + movie.getTitle(), e);
        }

        return false;
    }

    /**
     * Clear the poster fetch cache (useful for testing or admin operations)
     */
    public static void clearPosterCache() {
        posterFetchCache.clear();
        LOGGER.info("Poster fetch cache cleared");
    }

    /**
     * Get cache status for monitoring
     */
    public static int getCacheSize() {
        return posterFetchCache.size();
    }

    /**
     * Shutdown the executor service (call this when application shuts down)
     */
    public static void shutdown() {
        posterFetchExecutor.shutdown();
        LOGGER.info("Poster fetch executor shut down");
    }
}