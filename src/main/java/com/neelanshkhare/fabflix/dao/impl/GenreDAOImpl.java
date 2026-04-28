package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.GenreDAO;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GenreDAOImpl implements GenreDAO {
    private static final Logger logger = LoggerFactory.getLogger(GenreDAOImpl.class);

    @Override
    public Genre findById(int id) {
        String sql = "SELECT id, name FROM genres WHERE id = ?";
        Genre genre = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding genre by ID: {}", id, e);
        }

        return genre;
    }

    @Override
    public Genre findByName(String name) {
        String sql = "SELECT id, name FROM genres WHERE name = ?";
        Genre genre = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding genre by name: {}", name, e);
        }

        return genre;
    }

    @Override
    public List<Genre> findByMovie(String movieId) {
        String sql = "SELECT g.id, g.name " +
                "FROM genres g " +
                "JOIN genres_in_movies gim ON g.id = gim.genre_id " +
                "WHERE gim.movie_id = ?";
        List<Genre> genres = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, movieId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Genre genre = new Genre();
                    genre.setId(rs.getInt("id"));
                    genre.setName(rs.getString("name"));
                    genres.add(genre);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding genres for movie ID: {}", movieId, e);
        }

        return genres;
    }

    @Override
    public List<Genre> listGenres() {
        String sql = "SELECT id, name FROM genres ORDER BY name";
        List<Genre> genres = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                genres.add(genre);
            }
        } catch (SQLException e) {
            logger.error("Error listing genres", e);
        }

        return genres;
    }

    @Override
    public boolean insert(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getWriteConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, genre.getName());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        genre.setId(generatedKeys.getInt(1));
                    }
                }
                success = true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting genre: {}", genre.getName(), e);
        }

        return success;
    }

    @Override
    public boolean update(Genre genre) {
        String sql = "UPDATE genres SET name = ? WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getWriteConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, genre.getName());
            stmt.setInt(2, genre.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 1) {
                success = true;
            }
        } catch (SQLException e) {
            logger.error("Error updating genre: {}", genre.getId(), e);
        }

        return success;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM genres WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getWriteConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 1) {
                success = true;
            }
        } catch (SQLException e) {
            logger.error("Error deleting genre ID: {}", id, e);
        }

        return success;
    }
}