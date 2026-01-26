package com.neelanshkhare.fabflix.util;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * Utility to migrate plaintext passwords to BCrypt hashes.
 * Run this after importing legacy data with plaintext passwords.
 *
 * Usage: java -cp "target/fabflix/WEB-INF/lib/*:target/classes" \
 *             com.neelanshkhare.fabflix.util.PasswordMigration
 */
public class PasswordMigration {

    // BCrypt hash pattern: starts with $2a$, $2b$, or $2y$ and is 60 chars
    private static boolean isBCryptHash(String password) {
        return password != null &&
               password.length() == 60 &&
               password.matches("^\\$2[aby]\\$.*");
    }

    public static void main(String[] args) {
        String jdbcUrl = System.getenv("JDBC_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        // Defaults for local development
        if (jdbcUrl == null) {
            jdbcUrl = "jdbc:postgresql://localhost:5432/fabflix";
        }
        if (dbUser == null) {
            dbUser = System.getProperty("user.name");
        }
        if (dbPassword == null) {
            dbPassword = "";
        }

        System.out.println("=== Password Migration Utility ===");
        System.out.println("Connecting to: " + jdbcUrl);

        int migrated = 0;
        int skipped = 0;
        int errors = 0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);

            // Find all customers with plaintext passwords
            String selectSql = "SELECT id, email, password FROM customers";
            String updateSql = "UPDATE customers SET password = ? WHERE id = ?";

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 ResultSet rs = selectStmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    String password = rs.getString("password");

                    if (isBCryptHash(password)) {
                        skipped++;
                        continue;
                    }

                    try {
                        // Hash the plaintext password
                        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();

                        System.out.println("Migrated: " + email);
                        migrated++;
                    } catch (Exception e) {
                        System.err.println("Error migrating " + email + ": " + e.getMessage());
                        errors++;
                    }
                }
            }

            conn.commit();
            System.out.println("\n=== Migration Complete ===");
            System.out.println("Migrated: " + migrated);
            System.out.println("Skipped (already BCrypt): " + skipped);
            System.out.println("Errors: " + errors);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
