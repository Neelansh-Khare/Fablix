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
            // Priority: Environment variables (via ConfigUtil) > db.properties
            String dbDriver = ConfigUtil.getProperty("db.driver", "org.postgresql.Driver");
            String dbUrl = ConfigUtil.getProperty("db.url", "jdbc:postgresql://localhost:5432/fabflix");
            String dbUsername = ConfigUtil.getProperty("db.username", "postgres");
            String dbPassword = ConfigUtil.getProperty("db.password", "password");
            
            int minConnections = Integer.parseInt(ConfigUtil.getProperty("db.min_connections", "5"));
            int maxConnections = Integer.parseInt(ConfigUtil.getProperty("db.max_connections", "10"));

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(dbDriver);
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);

            // Pool settings
            config.setMinimumIdle(minConnections);
            config.setMaximumPoolSize(maxConnections);
            
            // Optimization settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully with url: {}", dbUrl);
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