package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.CustomerDAO;
import com.neelanshkhare.fabflix.model.Customer;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import com.neelanshkhare.fabflix.util.SecurityUtil;

import java.sql.*;

public class CustomerDAOImpl implements CustomerDAO {

    @Override
    public Customer findById(int id) {
        String sql = "SELECT id, first_name, last_name, cc_id, address, email FROM customers WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Customer customer = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                customer = new Customer();
                customer.setId(rs.getInt("id"));
                customer.setFirstName(rs.getString("first_name"));
                customer.setLastName(rs.getString("last_name"));
                customer.setCcId(rs.getString("cc_id"));
                customer.setAddress(rs.getString("address"));
                customer.setEmail(rs.getString("email"));
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

        return customer;
    }

    @Override
    public Customer findByEmail(String email) {
        String sql = "SELECT id, first_name, last_name, cc_id, address, email FROM customers WHERE email = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Customer customer = null;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                customer = new Customer();
                customer.setId(rs.getInt("id"));
                customer.setFirstName(rs.getString("first_name"));
                customer.setLastName(rs.getString("last_name"));
                customer.setCcId(rs.getString("cc_id"));
                customer.setAddress(rs.getString("address"));
                customer.setEmail(rs.getString("email"));
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

        return customer;
    }

    @Override
    public boolean insert(Customer customer) {
        String sql = "INSERT INTO customers (first_name, last_name, cc_id, address, email, password, salt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();

            // Generate salt and hash password
            String salt = SecurityUtil.generateSalt();
            String hashedPassword = SecurityUtil.hashPassword(customer.getPassword(), salt);

            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getCcId());
            stmt.setString(4, customer.getAddress());
            stmt.setString(5, customer.getEmail());
            stmt.setString(6, hashedPassword);
            stmt.setString(7, salt);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    customer.setId(generatedKeys.getInt(1));
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
    public boolean update(Customer customer) {
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, cc_id = ?, address = ?, email = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
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
        String sql = "DELETE FROM customers WHERE id = ?";

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

    @Override
    public boolean verifyPassword(String email, String password) {
        String sql = "SELECT password, salt FROM customers WHERE email = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isValid = false;

        try {
            conn = DBConnectionUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String salt = rs.getString("salt");

                // Verify the password
                String hashedPassword = SecurityUtil.hashPassword(password, salt);
                isValid = storedPassword.equals(hashedPassword);
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

        return isValid;
    }
}