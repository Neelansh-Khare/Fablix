package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.service.StarService;
import org.json.JSONArray;
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
import java.util.List;

@WebServlet("/api/stars/*")
public class StarServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StarServlet.class);
    private StarService starService;

    @Override
    public void init() throws ServletException {
        super.init();
        starService = new StarService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all stars with pagination
                int page = 1;
                int pageSize = 10;

                try {
                    String pageParam = request.getParameter("page");
                    String pageSizeParam = request.getParameter("pageSize");

                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }

                    if (pageSizeParam != null && !pageSizeParam.isEmpty()) {
                        pageSize = Integer.parseInt(pageSizeParam);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid pagination parameters, using defaults", e);
                }

                // Check if search by name is requested
                String nameQuery = request.getParameter("name");
                List<Star> stars;

                if (nameQuery != null && !nameQuery.isEmpty()) {
                    stars = starService.getStarsByName(nameQuery);
                } else {
                    stars = starService.listStars(page, pageSize);
                }

                JSONArray starsArray = new JSONArray();
                for (Star star : stars) {
                    JSONObject starObj = new JSONObject();
                    starObj.put("id", star.getId());
                    starObj.put("name", star.getName());
                    starObj.put("birthYear", star.getBirthYear());
                    starObj.put("photoUrl", star.getPhotoUrl());
                    starsArray.put(starObj);
                }

                JSONObject result = new JSONObject();
                result.put("stars", starsArray);
                out.print(result.toString());

            } else {
                // Get a single star by ID
                String starId = pathInfo.substring(1);
                Star star = starService.getStar(starId);

                if (star != null) {
                    JSONObject starObj = new JSONObject();
                    starObj.put("id", star.getId());
                    starObj.put("name", star.getName());
                    starObj.put("birthYear", star.getBirthYear());
                    starObj.put("photoUrl", star.getPhotoUrl());

                    // Add movies
                    JSONArray moviesArray = new JSONArray();
                    if (star.getMovies() != null) {
                        for (Movie movie : star.getMovies()) {
                            JSONObject movieObj = new JSONObject();
                            movieObj.put("id", movie.getId());
                            movieObj.put("title", movie.getTitle());
                            movieObj.put("year", movie.getYear());
                            movieObj.put("director", movie.getDirector());
                            moviesArray.put(movieObj);
                        }
                    }
                    starObj.put("movies", moviesArray);

                    out.print(starObj.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JSONObject error = new JSONObject();
                    error.put("message", "Star not found");
                    out.print(error.toString());
                }
            }
        } catch (Exception e) {
            logger.error("Error in doGet for StarServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder buffer = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            logger.error("Error reading request body in StarServlet", e);
        }

        String payload = buffer.toString();
        PrintWriter out = response.getWriter();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            Star star = new Star();
            star.setId(jsonRequest.getString("id"));
            star.setName(jsonRequest.getString("name"));

            if (jsonRequest.has("birthYear") && !jsonRequest.isNull("birthYear")) {
                star.setBirthYear(jsonRequest.getInt("birthYear"));
            }

            if (jsonRequest.has("photoUrl")) {
                star.setPhotoUrl(jsonRequest.getString("photoUrl"));
            }

            boolean success = starService.addStar(star);

            if (success) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                JSONObject result = new JSONObject();
                result.put("message", "Star created successfully");
                result.put("id", star.getId());
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to create star");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doPost for StarServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("message", "Star ID is required");
            out.print(error.toString());
            return;
        }

        String starId = pathInfo.substring(1);
        StringBuilder buffer = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            logger.error("Error reading request body in StarServlet", e);
        }

        String payload = buffer.toString();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            Star star = new Star();
            star.setId(starId);
            star.setName(jsonRequest.getString("name"));

            if (jsonRequest.has("birthYear") && !jsonRequest.isNull("birthYear")) {
                star.setBirthYear(jsonRequest.getInt("birthYear"));
            }

            if (jsonRequest.has("photoUrl")) {
                star.setPhotoUrl(jsonRequest.getString("photoUrl"));
            }

            boolean success = starService.updateStar(star);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Star updated successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to update star");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doPut for StarServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("message", "Star ID is required");
            out.print(error.toString());
            return;
        }

        String starId = pathInfo.substring(1);

        try {
            boolean success = starService.deleteStar(starId);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Star deleted successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSONObject error = new JSONObject();
                error.put("message", "Star not found or could not be deleted");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doDelete for StarServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }
}