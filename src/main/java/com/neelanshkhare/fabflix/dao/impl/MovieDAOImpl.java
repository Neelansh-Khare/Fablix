package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.MovieDAO;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import org.json.JSONArray;
import org.json.JSONObject;
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
        String sql = "SELECT get_movie_details(?) as details";
        Movie movie = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String jsonDetails = rs.getString("details");
                    if (jsonDetails != null) {
                        movie = parseMovieJson(new JSONObject(jsonDetails));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding movie by ID using procedure: {}", id, e);
        }

        return movie;
    }

    private Movie parseMovieJson(JSONObject movieJson) {
        Movie movie = new Movie();
        movie.setId(movieJson.getString("id"));
        movie.setTitle(movieJson.getString("title"));
        movie.setYear(movieJson.getInt("year"));
        movie.setDirector(movieJson.getString("director"));
        movie.setBannerUrl(movieJson.optString("bannerUrl", null));
        movie.setTrailerUrl(movieJson.optString("trailerUrl", null));

        // Parse genres
        if (movieJson.has("genres") && !movieJson.isNull("genres")) {
            JSONArray genresArray = movieJson.getJSONArray("genres");
            for (int i = 0; i < genresArray.length(); i++) {
                JSONObject gJson = genresArray.getJSONObject(i);
                Genre genre = new Genre();
                genre.setId(gJson.getInt("id"));
                genre.setName(gJson.getString("name"));
                movie.addGenre(genre);
            }
        }

        // Parse stars
        if (movieJson.has("stars") && !movieJson.isNull("stars")) {
            JSONArray starsArray = movieJson.getJSONArray("stars");
            for (int i = 0; i < starsArray.length(); i++) {
                JSONObject sJson = starsArray.getJSONObject(i);
                Star star = new Star();
                star.setId(sJson.getString("id"));
                star.setName(sJson.getString("name"));
                star.setBirthYear(sJson.isNull("birthYear") ? null : sJson.getInt("birthYear"));
                star.setPhotoUrl(sJson.optString("photoUrl", null));
                movie.addStar(star);
            }
        }
        return movie;
    }

    @Override
    public List<Movie> findByTitle(String title) {
        String sql = "SELECT search_movies_optimized(NULL, ?, NULL, NULL, NULL, NULL, NULL, 'title', 'ASC', 100, 0, NULL) as result";
        return executeOptimizedMovieQuery(sql, title);
    }

    @Override
    public List<Movie> findByDirector(String director) {
        String sql = "SELECT search_movies_optimized(NULL, NULL, NULL, ?, NULL, NULL, NULL, 'title', 'ASC', 100, 0, NULL) as result";
        return executeOptimizedMovieQuery(sql, director);
    }

    @Override
    public List<Movie> findByYear(int year) {
        String sql = "SELECT search_movies_optimized(NULL, NULL, ?, NULL, NULL, NULL, NULL, 'title', 'ASC', 100, 0, NULL) as result";
        return executeOptimizedMovieQuery(sql, year);
    }

    @Override
    public List<Movie> findByGenre(int genreId) {
        String sql = "SELECT search_movies_optimized(NULL, NULL, NULL, NULL, NULL, ?, NULL, 'title', 'ASC', 100, 0, NULL) as result";
        return executeOptimizedMovieQuery(sql, genreId);
    }

    @Override
    public List<Movie> findByStar(String starId) {
        String sql = "SELECT search_movies_optimized(NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'title', 'ASC', 100, 0, ?) as result";
        return executeOptimizedMovieQuery(sql, starId);
    }

    @Override
    public List<Movie> searchMovies(String query) {
        String sql = "SELECT search_movies_optimized(?, NULL, NULL, NULL, NULL, NULL, NULL, 'title', 'ASC', 100, 0, NULL) as result";
        return executeOptimizedMovieQuery(sql, query);
    }

    @Override
    public List<Movie> searchMovies(String query, String title, Integer year, String director, String starName, Integer genreId, String firstLetter, String sortBy, String sortOrder, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT search_movies_optimized(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL) as result";
        return executeOptimizedMovieQuery(sql, query, title, year, director, starName, genreId, firstLetter, sortBy, sortOrder, pageSize, offset);
    }

    @Override
    public List<Movie> listMovies(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT search_movies_optimized(NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'title', 'ASC', ?, ?, NULL) as result";
        return executeOptimizedMovieQuery(sql, pageSize, offset);
    }

    private List<Movie> executeOptimizedMovieQuery(String sql, Object... params) {
        List<Movie> movies = new ArrayList<>();
        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, Types.NULL);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String jsonResult = rs.getString("result");
                    if (jsonResult != null) {
                        movies.add(parseMovieJson(new JSONObject(jsonResult)));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing optimized movie query", e);
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
    public int countMoviesFiltered(String query, String title, Integer year, String director, String starName, Integer genreId, String firstLetter) {
        String sql = "SELECT count_movies_filtered(?, ?, ?, ?, ?, ?, ?)";
        int count = 0;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, query);
            stmt.setString(2, title);
            if (year != null) stmt.setInt(3, year); else stmt.setNull(3, Types.INTEGER);
            stmt.setString(4, director);
            stmt.setString(5, starName);
            if (genreId != null) stmt.setInt(6, genreId); else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, firstLetter);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting filtered movies", e);
        }

        return count;
    }

    @Override
    public boolean insert(Movie movie) {
        String sql = "INSERT INTO movies (id, title, year, director, banner_url, trailer_url) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getWriteConnection()) {
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
    public List<Movie> getSimilarMovies(String movieId, int limit) {
        String sql = "SELECT id, title, year, director, banner_url FROM get_similar_movies(?, ?)";
        return executeMovieQuery(sql, movieId, limit);
    }

    @Override
    public List<Movie> getCoPurchaseRecommendations(String movieId, int limit) {
        String sql = "SELECT id, title, year, director, banner_url FROM get_co_purchase_recommendations(?, ?)";
        return executeMovieQuery(sql, movieId, limit);
    }

    @Override
    public String addMovieWithProcedure(String title, int year, String director, String starName, String genreName) {
        String sql = "CALL add_movie(?, ?, ?, ?, ?, ?)";
        String message = "Error: Procedure failed to execute";

        try (Connection conn = DBConnectionUtil.getWriteConnection();
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

        try (Connection conn = DBConnectionUtil.getWriteConnection()) {
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

        try (Connection conn = DBConnectionUtil.getWriteConnection()) {
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

    private List<Movie> executeMovieQuery(String sql, Object... params) {
        List<Movie> movies = new ArrayList<>();
        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, Types.NULL);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    
                    // Only set banner_url and trailer_url if they exist in the result set
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i).toLowerCase();
                        if ("banner_url".equals(columnName)) {
                            movie.setBannerUrl(rs.getString(i));
                        } else if ("trailer_url".equals(columnName)) {
                            movie.setTrailerUrl(rs.getString(i));
                        }
                    }
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing movie query with params: {}", params, e);
        }
        return movies;
    }
}