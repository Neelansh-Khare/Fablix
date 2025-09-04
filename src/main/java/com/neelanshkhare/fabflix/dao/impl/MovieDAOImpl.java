package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.MovieDAO;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieDAOImpl implements MovieDAO {

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Movie movie = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            rs = stmt.executeQuery();

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

                // Add stars
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

                // Add genres
                Integer genreId = rs.getObject("genre_id") != null ? rs.getInt("genre_id") : null;
                if (genreId != null && !genresMap.containsKey(genreId)) {
                    Genre genre = new Genre();
                    genre.setId(genreId);
                    genre.setName(rs.getString("genre_name"));

                    genresMap.put(genreId, genre);
                    movie.addGenre(genre);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
        String sql = "SELECT id, title, year, director, banner_url, trailer_url " +
                "FROM movies " +
                "WHERE MATCH (title, director) AGAINST (? IN BOOLEAN MODE) " +
                "LIMIT 100";
        return executeMovieQuery(sql, query + "*");
    }

    @Override
    public List<Movie> listMovies(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, title, year, director, banner_url, trailer_url FROM movies LIMIT ? OFFSET ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Movie> movies = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            rs = stmt.executeQuery();

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

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return movies;
    }

    @Override
    public int countMovies() {
        String sql = "SELECT COUNT(*) as count FROM movies";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int count = 0;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                count = rs.getInt("count");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return count;
    }

    @Override
    public boolean insert(Movie movie) {
        String sql = "INSERT INTO movies (id, title, year, director, banner_url, trailer_url) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, movie.getId());
            stmt.setString(2, movie.getTitle());
            stmt.setInt(3, movie.getYear());
            stmt.setString(4, movie.getDirector());
            stmt.setString(5, movie.getBannerUrl());
            stmt.setString(6, movie.getTrailerUrl());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                // Add stars for the movie
                if (movie.getStars() != null && !movie.getStars().isEmpty()) {
                    insertStarsInMovie(conn, movie);
                }

                // Add genres for the movie
                if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                    insertGenresInMovie(conn, movie);
                }

                conn.commit();
                success = true;
            } else {
                conn.rollback();
            }

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    DBConnectionUtil.releaseConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return success;
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

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, movie.getTitle());
            stmt.setInt(2, movie.getYear());
            stmt.setString(3, movie.getDirector());
            stmt.setString(4, movie.getBannerUrl());
            stmt.setString(5, movie.getTrailerUrl());
            stmt.setString(6, movie.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                // Update stars (delete all and insert again)
                deleteStarsInMovie(conn, movie.getId());
                if (movie.getStars() != null && !movie.getStars().isEmpty()) {
                    insertStarsInMovie(conn, movie);
                }

                // Update genres (delete all and insert again)
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
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    DBConnectionUtil.releaseConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            // Delete stars in movie (will be cascaded)
            deleteStarsInMovie(conn, id);

            // Delete genres in movie (will be cascaded)
            deleteGenresInMovie(conn, id);

            // Delete the movie
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                conn.commit();
                success = true;
            } else {
                conn.rollback();
            }

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    DBConnectionUtil.releaseConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    private List<Movie> executeMovieQuery(String sql, Object param) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Movie> movies = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);

            if (param instanceof String) {
                stmt.setString(1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(1, (Integer) param);
            }

            rs = stmt.executeQuery();

            while (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getString("id"));
                movie.setTitle(rs.getString("title"));
                movie.setYear(rs.getInt("year"));
                movie.setDirector(rs.getString("director"));

                // Make sure to get banner_url and trailer_url
                movie.setBannerUrl(rs.getString("banner_url"));
                movie.setTrailerUrl(rs.getString("trailer_url"));

                movies.add(movie);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return movies;
    }
}