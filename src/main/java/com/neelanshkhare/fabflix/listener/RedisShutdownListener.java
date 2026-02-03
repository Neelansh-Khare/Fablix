package com.neelanshkhare.fabflix.listener;

import com.neelanshkhare.fabflix.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Listener to manage Redis connection pool lifecycle
 * Ensures proper shutdown of Redis connections when the application stops
 */
@WebListener
public class RedisShutdownListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisShutdownListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("FabFlix application starting - Redis connection pool will be initialized on first use");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("FabFlix application shutting down - closing Redis connection pool");
        RedisUtil.shutdown();
    }
}
