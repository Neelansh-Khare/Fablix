package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.StarDAO;
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

public class StarDAOImpl implements StarDAO {
    private static final Logger logger = LoggerFactory.getLogger(StarDAOImpl.class);

    @Override
    public Star findById(String id) {
        String sql = "SELECT s.id, s.name, s.birth_year, s.photo_url, " +
                "m.id as movie_id, m.title as movie_title, m.year as movie_year, m.director as movie_director " +
                "FROM stars s " +
                "LEFT JOIN stars_in_movies sim ON s.id = sim.star_id " +
                "LEFT JOIN movies m ON sim.movie_id = m.id " +
                "WHERE s.id = ?";
        Star star = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, Movie> moviesMap = new HashMap<>();
                while (rs.next()) {
                    if (star == null) {
                        star = new Star();
                        star.setId(rs.getString("id"));
                        star.setName(rs.getString("name"));
                        star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                        star.setPhotoUrl(rs.getString("photo_url"));
                    }

                    String movieId = rs.getString("movie_id");
                    if (movieId != null && !moviesMap.containsKey(movieId)) {
                        Movie movie = new Movie();
                        movie.setId(movieId);
                        movie.setTitle(rs.getString("movie_title"));
                        movie.setYear(rs.getInt("movie_year"));
                        movie.setDirector(rs.getString("movie_director"));
                        moviesMap.put(movieId, movie);
                        star.addMovie(movie);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding star by ID: {}", id, e);
        }

        return star;
    }

    @Override
    public List<Star> findByName(String name) {
        String sql = "SELECT id, name, birth_year, photo_url FROM stars WHERE name LIKE ? LIMIT 100";
        List<Star> stars = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + name + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Star star = new Star();
                    star.setId(rs.getString("id"));
                    star.setName(rs.getString("name"));
                    star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                    star.setPhotoUrl(rs.getString("photo_url"));
                    stars.add(star);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding stars by name: {}", name, e);
        }

        return stars;
    }

    @Override
    public List<Star> findByMovie(String movieId) {
        String sql = "SELECT s.id, s.name, s.birth_year, s.photo_url " +
                "FROM stars s " +
                "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                "WHERE sim.movie_id = ?";
        List<Star> stars = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, movieId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Star star = new Star();
                    star.setId(rs.getString("id"));
                    star.setName(rs.getString("name"));
                    star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                    star.setPhotoUrl(rs.getString("photo_url"));
                    stars.add(star);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding stars for movie ID: {}", movieId, e);
        }

        return stars;
    }

    @Override
    public List<Star> listStars(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, name, birth_year, photo_url FROM stars LIMIT ? OFFSET ?";
        List<Star> stars = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Star star = new Star();
                    star.setId(rs.getString("id"));
                    star.setName(rs.getString("name"));
                    star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                    star.setPhotoUrl(rs.getString("photo_url"));
                    stars.add(star);
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing stars - page: {}, pageSize: {}", page, pageSize, e);
        }

        return stars;
    }

    @Override
    public boolean insert(Star star) {
        String sql = "INSERT INTO stars (id, name, birth_year, photo_url) VALUES (?, ?, ?, ?)";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, star.getId());
                stmt.setString(2, star.getName());
                if (star.getBirthYear() != null) {
                    stmt.setInt(3, star.getBirthYear());
                } else {
                    stmt.setNull(3, Types.INTEGER);
                }
                stmt.setString(4, star.getPhotoUrl());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    if (star.getMovies() != null && !star.getMovies().isEmpty()) {
                        insertMoviesForStar(conn, star);
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
            logger.error("Error inserting star: {}", star.getId(), e);
        }

        return success;
    }

    @Override
    public String addStarWithProcedure(String name, Integer birthYear) {
        String sql = "CALL add_star(?, ?, ?)";
        String message = "Error: Procedure failed to execute";

        try (Connection conn = DBConnectionUtil.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            stmt.setString(1, name);
            if (birthYear != null) {
                stmt.setInt(2, birthYear);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.registerOutParameter(3, Types.VARCHAR);

            stmt.execute();
            message = stmt.getString(3);
            
        } catch (SQLException e) {
            logger.error("Error calling add_star procedure: {}", name, e);
            message = "Database Error: " + e.getMessage();
        }

        return message;
    }

    private void insertMoviesForStar(Connection conn, Star star) throws SQLException {
        String sql = "INSERT INTO stars_in_movies (star_id, movie_id) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Movie movie : star.getMovies()) {
                stmt.setString(1, star.getId());
                stmt.setString(2, movie.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean update(Star star) {
        String sql = "UPDATE stars SET name = ?, birth_year = ?, photo_url = ? WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, star.getName());
                if (star.getBirthYear() != null) {
                    stmt.setInt(2, star.getBirthYear());
                } else {
                    stmt.setNull(2, Types.INTEGER);
                }
                stmt.setString(3, star.getPhotoUrl());
                stmt.setString(4, star.getId());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    deleteMoviesForStar(conn, star.getId());
                    if (star.getMovies() != null && !star.getMovies().isEmpty()) {
                        insertMoviesForStar(conn, star);
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
            logger.error("Error updating star ID: {}", star.getId(), e);
        }

        return success;
    }

    private void deleteMoviesForStar(Connection conn, String starId) throws SQLException {
        String sql = "DELETE FROM stars_in_movies WHERE star_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, starId);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM stars WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                deleteMoviesForStar(conn, id);
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
            logger.error("Error deleting star ID: {}", id, e);
        }

        return success;
    }
}