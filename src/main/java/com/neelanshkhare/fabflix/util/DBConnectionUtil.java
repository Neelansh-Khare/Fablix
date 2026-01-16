package com.neelanshkhare.fabflix.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBConnectionUtil.class);
    private static HikariDataSource dataSource;

    static {
        try {
            // Load database properties
            Properties prop = new Properties();
            try (InputStream input = DBConnectionUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (input == null) {
                    logger.error("Unable to find db.properties");
                } else {
                    prop.load(input);
                }
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(prop.getProperty("db.driver"));
            config.setJdbcUrl(prop.getProperty("db.url"));
            config.setUsername(prop.getProperty("db.username"));
            config.setPassword(prop.getProperty("db.password"));

            // Pool settings
            int minConnections = Integer.parseInt(prop.getProperty("db.min_connections", "5"));
            int maxConnections = Integer.parseInt(prop.getProperty("db.max_connections", "10"));
            
            config.setMinimumIdle(minConnections);
            config.setMaximumPoolSize(maxConnections);
            
            // Optimization settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing HikariCP connection pool", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    public static void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Error releasing database connection", e);
        }
    }

    public static void closeAllConnections() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }
}