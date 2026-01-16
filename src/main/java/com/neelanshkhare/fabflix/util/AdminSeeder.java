package com.neelanshkhare.fabflix.util;

import com.neelanshkhare.fabflix.dao.CustomerDAO;
import com.neelanshkhare.fabflix.dao.impl.CustomerDAOImpl;
import com.neelanshkhare.fabflix.model.Customer;

public class AdminSeeder {
    public static void main(String[] args) {
        CustomerDAO customerDAO = new CustomerDAOImpl();
        
        // Check if admin exists
        Customer existingAdmin = customerDAO.findByEmail("admin@fabflix.com");
        if (existingAdmin != null) {
            System.out.println("Admin user already exists.");
            
            // Ensure role is admin
            if (!"admin".equals(existingAdmin.getRole())) {
                updateRole(existingAdmin.getId(), "admin");
                System.out.println("Updated role to admin.");
            }
            return;
        }

        Customer admin = new Customer();
        admin.setFirstName("FabFlix");
        admin.setLastName("Admin");
        admin.setEmail("admin@fabflix.com");
        admin.setPassword("admin123"); // Change this in production!
        admin.setAddress("123 Admin St, Irvine, CA");
        admin.setCcId("941607"); // Assuming this ID exists in creditcards, if not, need to insert cc first.
        
        // Ensure credit card exists
        ensureCreditCard("941607");

        if (customerDAO.insert(admin)) {
            // Manually set role to admin (since insert defaults to customer)
            // Need to fetch again to get ID, or insert returns success but model ID is updated by DAO.
            updateRole(admin.getId(), "admin");
            System.out.println("Admin user created successfully: admin@fabflix.com / admin123");
        } else {
            System.err.println("Failed to create admin user.");
        }
    }

    private static void ensureCreditCard(String id) {
        try (java.sql.Connection conn = DBConnectionUtil.getConnection()) {
            String sql = "INSERT INTO creditcards (id, first_name, last_name, expiration) VALUES (?, 'Admin', 'User', '2030-01-01') ON CONFLICT (id) DO NOTHING";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateRole(int userId, String role) {
        try (java.sql.Connection conn = DBConnectionUtil.getConnection()) {
            String sql = "UPDATE customers SET role = ? WHERE id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, role);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}