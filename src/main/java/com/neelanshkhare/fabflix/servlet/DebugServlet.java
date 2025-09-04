package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.model.Movie;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/debug/posters")
public class DebugServlet extends HttpServlet {
    private MovieService movieService;

    @Override
    public void init() throws ServletException {
        super.init();
        movieService = new MovieService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Get first 10 movies to check their poster URLs
            List<Movie> movies = movieService.listMovies(1, 10);

            JSONArray moviesArray = new JSONArray();
            for (Movie movie : movies) {
                JSONObject movieObj = new JSONObject();
                movieObj.put("id", movie.getId());
                movieObj.put("title", movie.getTitle());
                movieObj.put("year", movie.getYear());
                movieObj.put("bannerUrl", movie.getBannerUrl());
                movieObj.put("trailerUrl", movie.getTrailerUrl());

                // Check if poster URL looks valid
                String bannerUrl = movie.getBannerUrl();
                boolean hasValidPoster = bannerUrl != null &&
                        !bannerUrl.isEmpty() &&
                        !bannerUrl.contains("no-poster.jpg") &&
                        !bannerUrl.contains("placeholder") &&
                        bannerUrl.startsWith("http");

                movieObj.put("hasValidPoster", hasValidPoster);
                moviesArray.put(movieObj);
            }

            JSONObject result = new JSONObject();
            result.put("movies", moviesArray);
            result.put("cacheSize", MovieService.getCacheSize());
            result.put("timestamp", System.currentTimeMillis());

            out.print(result.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error checking posters: " + e.getMessage());
            out.print(error.toString());
            e.printStackTrace();
        }
    }
}