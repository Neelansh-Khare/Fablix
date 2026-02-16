package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.service.StarService;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.util.RedisUtil;
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
import java.util.LinkedHashSet;
import java.util.Set;

@WebServlet("/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AutocompleteServlet.class);
    private MovieService movieService;
    private StarService starService;

    @Override
    public void init() throws ServletException {
        super.init();
        movieService = new MovieService();
        starService = new StarService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            String query = request.getParameter("query");
            if (query == null || query.trim().length() < 2) {
                // Need at least 2 characters for autocomplete
                JSONObject result = new JSONObject();
                result.put("suggestions", new JSONArray());
                out.print(result.toString());
                return;
            }

            int limit = 10; // Default limit
            try {
                String limitParam = request.getParameter("limit");
                if (limitParam != null && !limitParam.isEmpty()) {
                    limit = Integer.parseInt(limitParam);
                    limit = Math.min(limit, 20); // Max 20 suggestions
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid limit parameter, using default", e);
            }

            // Redis Caching Logic
            String normalizedQuery = query.toLowerCase().trim();
            String cacheKey = RedisUtil.AUTOCOMPLETE_KEY_PREFIX + normalizedQuery + ":" + limit;
            
            String cachedResponse = RedisUtil.get(cacheKey);
            if (cachedResponse != null) {
                logger.debug("Autocomplete cache hit for query: '{}', limit: {}", normalizedQuery, limit);
                out.print(cachedResponse);
                return;
            }

            logger.debug("Autocomplete cache miss for query: '{}', limit: {}", normalizedQuery, limit);

            // Get suggestions from movies and stars
            Set<JSONObject> suggestions = new LinkedHashSet<>(); // Use Set to avoid duplicates

            // Search movies by title
            List<Movie> moviesByTitle = movieService.getMoviesByTitle(query);
            for (Movie movie : moviesByTitle) {
                if (suggestions.size() >= limit) break;

                JSONObject suggestion = new JSONObject();
                suggestion.put("type", "movie");
                suggestion.put("id", movie.getId());
                suggestion.put("title", movie.getTitle());
                suggestion.put("subtitle", movie.getYear() + " - Dir. " + movie.getDirector());
                suggestion.put("value", movie.getTitle());
                suggestions.add(suggestion);
            }

            // Search movies by director
            if (suggestions.size() < limit) {
                List<Movie> moviesByDirector = movieService.getMoviesByDirector(query);
                for (Movie movie : moviesByDirector) {
                    if (suggestions.size() >= limit) break;

                    JSONObject suggestion = new JSONObject();
                    suggestion.put("type", "movie");
                    suggestion.put("id", movie.getId());
                    suggestion.put("title", movie.getTitle());
                    suggestion.put("subtitle", movie.getYear() + " - Dir. " + movie.getDirector());
                    suggestion.put("value", movie.getTitle());
                    suggestions.add(suggestion);
                }
            }

            // Search stars by name
            if (suggestions.size() < limit) {
                List<Star> starsByName = starService.getStarsByName(query);
                for (Star star : starsByName) {
                    if (suggestions.size() >= limit) break;

                    JSONObject suggestion = new JSONObject();
                    suggestion.put("type", "star");
                    suggestion.put("id", star.getId());
                    suggestion.put("title", star.getName());
                    suggestion.put("subtitle", "Actor" + (star.getBirthYear() != null ? " (Born " + star.getBirthYear() + ")" : ""));
                    suggestion.put("value", star.getName());
                    suggestions.add(suggestion);
                }
            }

            // Convert Set to JSONArray
            JSONArray suggestionsArray = new JSONArray();
            for (JSONObject suggestion : suggestions) {
                suggestionsArray.put(suggestion);
            }

            JSONObject result = new JSONObject();
            result.put("suggestions", suggestionsArray);
            result.put("query", query);
            
            String responseString = result.toString();
            
            // Store in Redis
            RedisUtil.set(cacheKey, responseString, RedisUtil.AUTOCOMPLETE_TTL);
            
            out.print(responseString);

        } catch (Exception e) {
            logger.error("Error in doGet for AutocompleteServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error getting autocomplete suggestions: " + e.getMessage());
            out.print(error.toString());
        }
    }
}