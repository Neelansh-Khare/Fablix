package com.neelanshkhare.fabflix.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DBConnectionUtil {
    private static final ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static int maxConnections;
    private static boolean initialized = false;

    static {
        try {
            // Load database properties
            Properties prop = new Properties();
            InputStream input = DBConnectionUtil.class.getClassLoader().getResourceAsStream("db.properties");
            prop.load(input);

            // Register JDBC driver
            Class.forName(prop.getProperty("db.driver"));

            // Set database connection info
            dbUrl = prop.getProperty("db.url");
            dbUsername = prop.getProperty("db.username");
            dbPassword = prop.getProperty("db.password");
            maxConnections = Integer.parseInt(prop.getProperty("db.max_connections"));

            // Initialize connection pool
            int minConnections = Integer.parseInt(prop.getProperty("db.min_connections"));
            for (int i = 0; i < minConnections; i++) {
                connectionPool.add(createNewConnection());
            }

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database connection pool is not initialized");
        }

        Connection connection = connectionPool.poll();
        if (connection == null || connection.isClosed()) {
            connection = createNewConnection();
        }

        return connection;
    }

    public static void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                if (connectionPool.size() < maxConnections) {
                    connectionPool.add(connection);
                } else {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeAllConnections() {
        Connection connection;
        while ((connection = connectionPool.poll()) != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}