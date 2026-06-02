package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import com.neelanshkhare.fabflix.util.RedisUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Health check endpoint for load balancers (AWS ELB, Apache) and Kubernetes probes.
 * Returns HTTP 200 if the application, database, and Redis are reachable.
 * Returns HTTP 503 Service Unavailable if critical components are down.
 */
@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HealthServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject result = new JSONObject();
        boolean isHealthy = true;

        // 1. Check Database (Read Pool)
        boolean dbOk = false;
        try (Connection conn = DBConnectionUtil.getReadConnection()) {
            if (conn != null && conn.isValid(2)) {
                dbOk = true;
            }
        } catch (SQLException e) {
            logger.error("Health check failed: Database connection error", e);
        }
        
        if (!dbOk) {
            isHealthy = false;
        }

        // 2. Check Redis Cluster
        boolean redisOk = RedisUtil.isRedisAvailable();
        // Even if Redis is down, we might not want to fail the health check if the app can degrade gracefully,
        // but for a strict health check in a cluster, we might report it as down.
        // For Fabflix, Redis is used for caching and sessions. Let's report its status.
        
        // 3. Assemble response
        result.put("status", isHealthy ? "UP" : "DOWN");
        result.put("database", dbOk ? "UP" : "DOWN");
        result.put("redis", redisOk ? "UP" : "DOWN");
        result.put("timestamp", System.currentTimeMillis());

        if (isHealthy) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        PrintWriter out = response.getWriter();
        out.print(result.toString());
        out.flush();
    }
}
