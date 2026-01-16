package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.CustomerDAO;
import com.neelanshkhare.fabflix.model.Customer;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import com.neelanshkhare.fabflix.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class CustomerDAOImpl implements CustomerDAO {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDAOImpl.class);

    @Override
    public Customer findById(int id) {
        String sql = "SELECT id, first_name, last_name, cc_id, address, email, role FROM customers WHERE id = ?";
        Customer customer = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    customer = new Customer();
                    customer.setId(rs.getInt("id"));
                    customer.setFirstName(rs.getString("first_name"));
                    customer.setLastName(rs.getString("last_name"));
                    customer.setCcId(rs.getString("cc_id"));
                    customer.setAddress(rs.getString("address"));
                    customer.setEmail(rs.getString("email"));
                    customer.setRole(rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding customer by ID: {}", id, e);
        }

        return customer;
    }

    @Override
    public Customer findByEmail(String email) {
        String sql = "SELECT id, first_name, last_name, cc_id, address, email, role FROM customers WHERE email = ?";
        Customer customer = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    customer = new Customer();
                    customer.setId(rs.getInt("id"));
                    customer.setFirstName(rs.getString("first_name"));
                    customer.setLastName(rs.getString("last_name"));
                    customer.setCcId(rs.getString("cc_id"));
                    customer.setAddress(rs.getString("address"));
                    customer.setEmail(rs.getString("email"));
                    customer.setRole(rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding customer by email: {}", email, e);
        }

        return customer;
    }

    @Override
    public boolean insert(Customer customer) {
        String sql = "INSERT INTO customers (first_name, last_name, cc_id, address, email, password, salt, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            // Hash password using BCrypt (salt is embedded)
            String hashedPassword = SecurityUtil.hashPassword(customer.getPassword());

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, customer.getFirstName());
                stmt.setString(2, customer.getLastName());
                stmt.setString(3, customer.getCcId());
                stmt.setString(4, customer.getAddress());
                stmt.setString(5, customer.getEmail());
                stmt.setString(6, hashedPassword);
                stmt.setNull(7, Types.VARCHAR); // No separate salt for BCrypt
                stmt.setString(8, "customer"); // Default role

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            customer.setId(generatedKeys.getInt(1));
                        }
                    }
                    success = true;
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting customer: {}", customer.getEmail(), e);
        }

        return success;
    }

    @Override
    public boolean update(Customer customer) {
        // ... (existing update implementation) ...
        // Note: Password update logic should be separate or handled carefully.
        // For general updates, we usually don't touch the password unless specified.
        // The current update method in DAO doesn't update password, which is fine.
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, cc_id = ?, address = ?, email = ? WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getCcId());
            stmt.setString(4, customer.getAddress());
            stmt.setString(5, customer.getEmail());
            stmt.setInt(6, customer.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 1) {
                success = true;
            }
        } catch (SQLException e) {
            logger.error("Error updating customer ID: {}", customer.getId(), e);
        }

        return success;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 1) {
                success = true;
            }
        } catch (SQLException e) {
            logger.error("Error deleting customer ID: {}", id, e);
        }

        return success;
    }

    @Override
    public boolean verifyPassword(String email, String password) {
        String sql = "SELECT id, password, salt FROM customers WHERE email = ?";
        boolean isValid = false;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    String salt = rs.getString("salt");
                    int userId = rs.getInt("id");

                    if (salt == null || salt.isEmpty()) {
                        // Use BCrypt check
                        isValid = SecurityUtil.checkPassword(password, storedPassword);
                    } else {
                        // Legacy SHA-256 check
                        String hashedPassword = SecurityUtil.hashPassword(password, salt);
                        isValid = storedPassword.equals(hashedPassword);

                        if (isValid) {
                            // Upgrade to BCrypt
                            upgradePassword(userId, password);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error verifying password for customer: {}", email, e);
        }

        return isValid;
    }

    private void upgradePassword(int userId, String rawPassword) {
        String sql = "UPDATE customers SET password = ?, salt = NULL WHERE id = ?";
        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String newHash = SecurityUtil.hashPassword(rawPassword);
            stmt.setString(1, newHash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            logger.info("Upgraded password security for user ID: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to auto-upgrade password for user ID: {}", userId, e);
        }
    }
}