package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.StarDAO;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarDAOImpl implements StarDAO {

    @Override
    public Star findById(String id) {
        String sql = "SELECT s.id, s.name, s.birth_year, s.photo_url, " +
                "m.id as movie_id, m.title as movie_title, m.year as movie_year, m.director as movie_director " +
                "FROM stars s " +
                "LEFT JOIN stars_in_movies sim ON s.id = sim.star_id " +
                "LEFT JOIN movies m ON sim.movie_id = m.id " +
                "WHERE s.id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Star star = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            Map<String, Movie> moviesMap = new HashMap<>();

            while (rs.next()) {
                if (star == null) {
                    star = new Star();
                    star.setId(rs.getString("id"));
                    star.setName(rs.getString("name"));
                    star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                    star.setPhotoUrl(rs.getString("photo_url"));
                }

                // Add movies
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

        return star;
    }

    @Override
    public List<Star> findByName(String name) {
        String sql = "SELECT id, name, birth_year, photo_url FROM stars WHERE name LIKE ? LIMIT 100";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Star> stars = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + name + "%");
            rs = stmt.executeQuery();

            while (rs.next()) {
                Star star = new Star();
                star.setId(rs.getString("id"));
                star.setName(rs.getString("name"));
                star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                star.setPhotoUrl(rs.getString("photo_url"));

                stars.add(star);
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

        return stars;
    }

    @Override
    public List<Star> findByMovie(String movieId) {
        String sql = "SELECT s.id, s.name, s.birth_year, s.photo_url " +
                "FROM stars s " +
                "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                "WHERE sim.movie_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Star> stars = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, movieId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Star star = new Star();
                star.setId(rs.getString("id"));
                star.setName(rs.getString("name"));
                star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                star.setPhotoUrl(rs.getString("photo_url"));

                stars.add(star);
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

        return stars;
    }

    @Override
    public List<Star> listStars(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, name, birth_year, photo_url FROM stars LIMIT ? OFFSET ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Star> stars = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Star star = new Star();
                star.setId(rs.getString("id"));
                star.setName(rs.getString("name"));
                star.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
                star.setPhotoUrl(rs.getString("photo_url"));

                stars.add(star);
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

        return stars;
    }

    @Override
    public boolean insert(Star star) {
        String sql = "INSERT INTO stars (id, name, birth_year, photo_url) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(sql);
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
                // Add movies for the star if any
                if (star.getMovies() != null && !star.getMovies().isEmpty()) {
                    insertMoviesForStar(conn, star);
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

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(sql);
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
                // Update movies (delete all and insert again)
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

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            // Delete star in movies (will be cascaded)
            deleteMoviesForStar(conn, id);

            // Delete the star
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
}