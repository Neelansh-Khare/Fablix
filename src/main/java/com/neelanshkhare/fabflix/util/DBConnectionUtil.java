package com.neelanshkhare.fabflix.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBConnectionUtil.class);
    private static HikariDataSource writeDataSource;
    private static HikariDataSource readDataSource;

    static {
        initializePools();
    }

    private static void initializePools() {
        try {
            // Fail fast if database URL is not configured — insecure defaults hide misconfiguration
            String writeUrl = ConfigUtil.getProperty("db.url.write", ConfigUtil.getProperty("db.url", null));
            if (writeUrl == null || writeUrl.isEmpty()) {
                throw new ExceptionInInitializerError(
                    "Database write URL is not configured. Set db.url or DB_URL environment variable.");
            }
            writeDataSource = createDataSource(writeUrl, "WritePool");
            logger.info("Write connection pool initialized successfully with url: {}", writeUrl);

            // Read Pool (Slave/Replica) - Falls back to write URL if not specified
            String readUrl = ConfigUtil.getProperty("db.url.read", writeUrl);
            readDataSource = createDataSource(readUrl, "ReadPool");
            logger.info("Read connection pool initialized successfully with url: {}", readUrl);

        } catch (Exception e) {
            logger.error("Error initializing HikariCP connection pools", e);
        }
    }

    private static HikariDataSource createDataSource(String url, String poolName) {
        String dbDriver = ConfigUtil.getProperty("db.driver", "org.postgresql.Driver");
        String dbUsername = ConfigUtil.getProperty("db.username", "postgres");
        String dbPassword = ConfigUtil.getProperty("db.password", "password");
        
        int minConnections = Integer.parseInt(ConfigUtil.getProperty("db.min_connections", "5"));
        int maxConnections = Integer.parseInt(ConfigUtil.getProperty("db.max_connections", "10"));

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(dbDriver);
        config.setJdbcUrl(url);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setPoolName(poolName);

        // Pool settings
        config.setMinimumIdle(minConnections);
        config.setMaximumPoolSize(maxConnections);
        config.setInitializationFailTimeout(-1); // Don't fail at startup if DB isn't ready yet
        
        // Optimization settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    /**
     * Get a connection for read-only operations. Falls back to write connection if read pool is unavailable.
     */
    public static Connection getConnection() throws SQLException {
        return getReadConnection();
    }

    public static Connection getReadConnection() throws SQLException {
        if (readDataSource == null) {
            return getWriteConnection();
        }
        return readDataSource.getConnection();
    }

    public static Connection getWriteConnection() throws SQLException {
        if (writeDataSource == null) {
            throw new SQLException("Write database connection pool is not initialized");
        }
        return writeDataSource.getConnection();
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
        if (writeDataSource != null && !writeDataSource.isClosed()) {
            writeDataSource.close();
        }
        if (readDataSource != null && !readDataSource.isClosed()) {
            readDataSource.close();
        }
        logger.info("HikariCP connection pools closed");
    }
}