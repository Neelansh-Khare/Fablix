package com.neelanshkhare.fabflix.service;

import com.neelanshkhare.fabflix.dao.GenreDAO;
import com.neelanshkhare.fabflix.dao.MovieDAO;
import com.neelanshkhare.fabflix.dao.StarDAO;
import com.neelanshkhare.fabflix.dao.impl.GenreDAOImpl;
import com.neelanshkhare.fabflix.dao.impl.MovieDAOImpl;
import com.neelanshkhare.fabflix.dao.impl.StarDAOImpl;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.MoviePosterUtil;
import com.neelanshkhare.fabflix.util.MoviePosterUtil.TmdbMovieDetails;
import com.neelanshkhare.fabflix.util.MoviePosterUtil.TmdbCastMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Pulls new movies from the TMDB discover endpoint and inserts them into the
 * database with posters, genres, and cast already populated — no separate
 * poster-fill pass needed for ingested movies.
 */
public class TmdbIngestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbIngestService.class);

    private final MovieDAO movieDAO;
    private final StarDAO starDAO;
    private final GenreDAO genreDAO;

    public static class IngestStats {
        public int newMovies;
        public int skipped;
        public int errors;

        @Override
        public String toString() {
            return "new=" + newMovies + " skipped=" + skipped + " errors=" + errors;
        }
    }

    public TmdbIngestService() {
        this.movieDAO = new MovieDAOImpl();
        this.starDAO = new StarDAOImpl();
        this.genreDAO = new GenreDAOImpl();
    }

    /**
     * Fetch {@code maxPages} pages (20 movies/page) from TMDB discover and
     * insert any that are not already in the database.
     */
    public IngestStats ingestDiscoverMovies(int maxPages) {
        IngestStats stats = new IngestStats();

        if (!MoviePosterUtil.isApiKeyConfigured()) {
            LOGGER.warn("TMDB API key not configured — skipping discover ingestion");
            return stats;
        }

        for (int page = 1; page <= maxPages; page++) {
            List<TmdbMovieDetails> page_results = MoviePosterUtil.fetchDiscoverMovies(page);
            if (page_results.isEmpty()) break;

            for (TmdbMovieDetails basic : page_results) {
                if (basic.getTitle().isEmpty() || basic.getYear() == 0) {
                    stats.skipped++;
                    continue;
                }

                if (movieDAO.existsByTitleAndYear(basic.getTitle(), basic.getYear())) {
                    stats.skipped++;
                    continue;
                }

                try {
                    TmdbMovieDetails details = MoviePosterUtil.fetchMovieDetails(basic.getTmdbId());
                    if (details == null) {
                        stats.errors++;
                        continue;
                    }

                    Movie movie = buildMovie(details);
                    if (movieDAO.insert(movie)) {
                        stats.newMovies++;
                        LOGGER.info("Ingested: {} ({})", details.getTitle(), details.getYear());
                    } else {
                        stats.errors++;
                        LOGGER.warn("Failed to insert: {} ({})", details.getTitle(), details.getYear());
                    }

                } catch (Exception e) {
                    stats.errors++;
                    LOGGER.warn("Error ingesting movie: {} ({})", basic.getTitle(), basic.getYear(), e);
                }
            }
        }

        return stats;
    }

    private Movie buildMovie(TmdbMovieDetails details) {
        Movie movie = new Movie();
        movie.setId(movieDAO.generateNextMovieId());
        movie.setTitle(details.getTitle());
        movie.setYear(details.getYear());
        movie.setDirector(details.getDirector() != null ? details.getDirector() : "Unknown");
        movie.setBannerUrl(details.getPosterUrl());
        movie.setTrailerUrl(details.getTrailerUrl());
        movie.setRating(details.getRating());
        movie.setNumVotes(details.getNumVotes());

        for (String genreName : details.getGenreNames()) {
            movie.addGenre(findOrCreateGenre(genreName));
        }

        for (TmdbCastMember cm : details.getCastMembers()) {
            if (cm.getName() != null && !cm.getName().isEmpty()) {
                movie.addStar(findOrCreateStar(cm));
            }
        }

        return movie;
    }

    private Genre findOrCreateGenre(String name) {
        Genre genre = genreDAO.findByName(name);
        if (genre == null) {
            genre = new Genre();
            genre.setName(name);
            genreDAO.insert(genre);
        }
        return genre;
    }

    private Star findOrCreateStar(TmdbCastMember cm) {
        Star star = starDAO.findByExactName(cm.getName());
        if (star != null) return star;

        star = new Star();
        star.setId(starDAO.generateNextStarId());
        star.setName(cm.getName());
        star.setPhotoUrl(cm.getPhotoUrl());
        starDAO.insert(star);
        return star;
    }
}
