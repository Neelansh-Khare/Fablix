package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.MovieDAO;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieDAOImpl implements MovieDAO {
    private static final Logger logger = LoggerFactory.getLogger(MovieDAOImpl.class);

    @Override
    public Movie findById(String id) {
        String sql = "SELECT m.id, m.title, m.year, m.director, m.banner_url, m.trailer_url, " +
                "s.id as star_id, s.name as star_name, s.birth_year as star_birth_year, s.photo_url as star_photo_url, " +
                "g.id as genre_id, g.name as genre_name " +
                "FROM movies m " +
                "LEFT JOIN stars_in_movies sim ON m.id = sim.movie_id " +
                "LEFT JOIN stars s ON sim.star_id = s.id " +
                "LEFT JOIN genres_in_movies gim ON m.id = gim.movie_id " +
                "LEFT JOIN genres g ON gim.genre_id = g.id " +
                "WHERE m.id = ?";
        Movie movie = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, Star> starsMap = new HashMap<>();
                Map<Integer, Genre> genresMap = new HashMap<>();
                while (rs.next()) {
                    if (movie == null) {
                        movie = new Movie();
                        movie.setId(rs.getString("id"));
                        movie.setTitle(rs.getString("title"));
                        movie.setYear(rs.getInt("year"));
                        movie.setDirector(rs.getString("director"));
                        movie.setBannerUrl(rs.getString("banner_url"));
                        movie.setTrailerUrl(rs.getString("trailer_url"));
                    }

                    String starId = rs.getString("star_id");
                    if (starId != null && !starsMap.containsKey(starId)) {
                        Star star = new Star();
                        star.setId(starId);
                        star.setName(rs.getString("star_name"));
                        star.setBirthYear(rs.getObject("star_birth_year") != null ? rs.getInt("star_birth_year") : null);
                        star.setPhotoUrl(rs.getString("star_photo_url"));
                        starsMap.put(starId, star);
                        movie.addStar(star);
                    }

                    Integer genreId = rs.getObject("genre_id") != null ? rs.getInt("genre_id") : null;
                    if (genreId != null && !genresMap.containsKey(genreId)) {
                        Genre genre = new Genre();
                        genre.setId(genreId);
                        genre.setName(rs.getString("genre_name"));
                        genresMap.put(genreId, genre);
                        movie.addGenre(genre);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding movie by ID: {}", id, e);
        }

        return movie;
    }

    @Override
    public List<Movie> findByTitle(String title) {
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies WHERE title LIKE ? LIMIT 100";
        return executeMovieQuery(sql, "%" + title + "%");
    }

    @Override
    public List<Movie> findByDirector(String director) {
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies WHERE director LIKE ? LIMIT 100";
        return executeMovieQuery(sql, "%" + director + "%");
    }

    @Override
    public List<Movie> findByYear(int year) {
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies WHERE year = ? LIMIT 100";
        return executeMovieQuery(sql, year);
    }

    @Override
    public List<Movie> findByGenre(int genreId) {
        String sql = "SELECT m.id, m.title, m.year, m.director, m.banner_url, m.trailer_url " +
                "FROM movies m " +
                "JOIN genres_in_movies gim ON m.id = gim.movie_id " +
                "WHERE gim.genre_id = ? " +
                "LIMIT 100";
        return executeMovieQuery(sql, genreId);
    }

    @Override
    public List<Movie> findByStar(String starId) {
        String sql = "SELECT m.id, m.title, m.year, m.director, m.banner_url, m.trailer_url " +
                "FROM movies m " +
                "JOIN stars_in_movies sim ON m.id = sim.movie_id " +
                "WHERE sim.star_id = ? " +
                "LIMIT 100";
        return executeMovieQuery(sql, starId);
    }

    @Override
    public List<Movie> searchMovies(String query) {
        // Optimized for PostgreSQL Full-Text Search
        // Using to_tsvector and plainto_tsquery for better multi-word matching and performance
        String sql = "SELECT id, title, year, director, banner_url, trailer_url " +
                "FROM movies " +
                "WHERE to_tsvector('english', title || ' ' || director) @@ plainto_tsquery('english', ?) " +
                "LIMIT 100";
        
        // Legacy LIKE-based search (fallback if needed)
        // String sql = "SELECT id, title, year, director, banner_url, trailer_url " +
        //         "FROM movies " +
        //         "WHERE LOWER(title) LIKE LOWER(?) OR LOWER(director) LIKE LOWER(?) " +
        //         "LIMIT 100";
        
        List<Movie> movies = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, query);
            // If using the legacy query, we would set two parameters
            // String searchPattern = "%" + query + "%";
            // stmt.setString(1, searchPattern);
            // stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setBannerUrl(rs.getString("banner_url"));
                    movie.setTrailerUrl(rs.getString("trailer_url"));
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching movies with query: {}", query, e);
        }

        return movies;
    }

    @Override
    public List<Movie> listMovies(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies LIMIT ? OFFSET ?";
        List<Movie> movies = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setBannerUrl(rs.getString("banner_url"));
                    movie.setTrailerUrl(rs.getString("trailer_url"));
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing movies - page: {}, pageSize: {}", page, pageSize, e);
        }

        return movies;
    }

    @Override
    public List<Movie> getMoviesWithoutPosters(int limit) {
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies " +
                "WHERE banner_url IS NULL OR banner_url = '' " +
                "OR banner_url LIKE '%no-poster.jpg%' OR banner_url LIKE '%placeholder%' " +
                "LIMIT ?";
        List<Movie> movies = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setBannerUrl(rs.getString("banner_url"));
                    movie.setTrailerUrl(rs.getString("trailer_url"));
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting movies without posters", e);
        }

        return movies;
    }

    @Override
    public int countMovies() {
        String sql = "SELECT COUNT(*) as count FROM movies";
        int count = 0;

        try (Connection conn = DBConnectionUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            logger.error("Error counting movies", e);
        }

        return count;
    }

    @Override
    public boolean insert(Movie movie) {
        String sql = "INSERT INTO movies (id, title, year, director, banner_url, trailer_url) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, movie.getId());
                stmt.setString(2, movie.getTitle());
                stmt.setInt(3, movie.getYear());
                stmt.setString(4, movie.getDirector());
                stmt.setString(5, movie.getBannerUrl());
                stmt.setString(6, movie.getTrailerUrl());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    if (movie.getStars() != null && !movie.getStars().isEmpty()) {
                        insertStarsInMovie(conn, movie);
                    }
                    if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                        insertGenresInMovie(conn, movie);
                    }
                    conn.commit();
                    success = true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error inserting movie: {}", movie.getId(), e);
        }

        return success;
    }

    @Override
    public String addMovieWithProcedure(String title, int year, String director, String starName, String genreName) {
        String sql = "CALL add_movie(?, ?, ?, ?, ?, ?)";
        String message = "Error: Procedure failed to execute";

        try (Connection conn = DBConnectionUtil.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            stmt.setString(1, title);
            stmt.setInt(2, year);
            stmt.setString(3, director);
            stmt.setString(4, starName);
            stmt.setString(5, genreName);
            stmt.registerOutParameter(6, Types.VARCHAR);

            stmt.execute();
            message = stmt.getString(6);
            
        } catch (SQLException e) {
            logger.error("Error calling add_movie procedure: {}", title, e);
            message = "Database Error: " + e.getMessage();
        }

        return message;
    }

    private void insertStarsInMovie(Connection conn, Movie movie) throws SQLException {
        String sql = "INSERT INTO stars_in_movies (star_id, movie_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Star star : movie.getStars()) {
                stmt.setString(1, star.getId());
                stmt.setString(2, movie.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertGenresInMovie(Connection conn, Movie movie) throws SQLException {
        String sql = "INSERT INTO genres_in_movies (genre_id, movie_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Genre genre : movie.getGenres()) {
                stmt.setInt(1, genre.getId());
                stmt.setString(2, movie.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean update(Movie movie) {
        String sql = "UPDATE movies SET title = ?, year = ?, director = ?, " +
                "banner_url = ?, trailer_url = ? WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, movie.getTitle());
                stmt.setInt(2, movie.getYear());
                stmt.setString(3, movie.getDirector());
                stmt.setString(4, movie.getBannerUrl());
                stmt.setString(5, movie.getTrailerUrl());
                stmt.setString(6, movie.getId());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    deleteStarsInMovie(conn, movie.getId());
                    if (movie.getStars() != null && !movie.getStars().isEmpty()) {
                        insertStarsInMovie(conn, movie);
                    }
                    deleteGenresInMovie(conn, movie.getId());
                    if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                        insertGenresInMovie(conn, movie);
                    }
                    conn.commit();
                    success = true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error updating movie ID: {}", movie.getId(), e);
        }

        return success;
    }

    private void deleteStarsInMovie(Connection conn, String movieId) throws SQLException {
        String sql = "DELETE FROM stars_in_movies WHERE movie_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, movieId);
            stmt.executeUpdate();
        }
    }

    private void deleteGenresInMovie(Connection conn, String movieId) throws SQLException {
        String sql = "DELETE FROM genres_in_movies WHERE movie_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, movieId);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM movies WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                deleteStarsInMovie(conn, id);
                deleteGenresInMovie(conn, id);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, id);
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 1) {
                        conn.commit();
                        success = true;
                    } else {
                        conn.rollback();
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error deleting movie ID: {}", id, e);
        }

        return success;
    }

    private List<Movie> executeMovieQuery(String sql, Object param) {
        List<Movie> movies = new ArrayList<>();
        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (param instanceof String) {
                stmt.setString(1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(1, (Integer) param);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setBannerUrl(rs.getString("banner_url"));
                    movie.setTrailerUrl(rs.getString("trailer_url"));
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing movie query with param: {}", param, e);
        }
        return movies;
    }
}