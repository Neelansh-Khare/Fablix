package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.GenreDAO;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GenreDAOImpl implements GenreDAO {

    @Override
    public Genre findById(int id) {
        String sql = "SELECT id, name FROM genres WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Genre genre = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
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

        return genre;
    }

    @Override
    public Genre findByName(String name) {
        String sql = "SELECT id, name FROM genres WHERE name = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Genre genre = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            rs = stmt.executeQuery();

            if (rs.next()) {
                genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
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

        return genre;
    }

    @Override
    public List<Genre> findByMovie(String movieId) {
        String sql = "SELECT g.id, g.name " +
                "FROM genres g " +
                "JOIN genres_in_movies gim ON g.id = gim.genre_id " +
                "WHERE gim.movie_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Genre> genres = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, movieId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));

                genres.add(genre);
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

        return genres;
    }

    @Override
    public List<Genre> listGenres() {
        String sql = "SELECT id, name FROM genres ORDER BY name";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<Genre> genres = new ArrayList<>();

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));

                genres.add(genre);
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

        return genres;
    }

    @Override
    public boolean insert(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, genre.getName());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    genre.setId(generatedKeys.getInt(1));
                }
                success = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    @Override
    public boolean update(Genre genre) {
        String sql = "UPDATE genres SET name = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, genre.getName());
            stmt.setInt(2, genre.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                success = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM genres WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                success = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) DBConnectionUtil.releaseConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return success;
    }
}