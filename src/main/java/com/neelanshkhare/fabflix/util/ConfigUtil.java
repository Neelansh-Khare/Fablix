package com.neelanshkhare.fabflix.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigUtil {
    private static final Logger LOGGER = Logger.getLogger(ConfigUtil.class.getName());
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                LOGGER.info("Loaded config.properties");
            } else {
                LOGGER.info("config.properties not found, relying on environment variables");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading config.properties", e);
        }
    }

    public static String getProperty(String key) {
        // 1. Try Environment Variable (Upper case, dots replaced by underscores)
        // e.g. tmdb.api.key -> TMDB_API_KEY
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue.trim();
        }

        // 2. Try Properties file (trim to remove accidental whitespace)
        String value = properties.getProperty(key);
        return value != null ? value.trim() : null;
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
}
