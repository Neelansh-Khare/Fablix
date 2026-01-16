package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.util.RecaptchaUtil;
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

@WebServlet("/api/config")
public class ConfigServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            JSONObject config = new JSONObject();
            config.put("recaptchaSiteKey", RecaptchaUtil.getSiteKey());
            out.print(config.toString());
        } catch (Exception e) {
            logger.error("Error loading configuration in ConfigServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error loading configuration");
            out.print(error.toString());
        }
    }
}