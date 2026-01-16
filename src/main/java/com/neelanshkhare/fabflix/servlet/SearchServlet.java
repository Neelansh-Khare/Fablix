package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.service.MovieService;
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

@WebServlet("/api/search")
public class SearchServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SearchServlet.class);
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
            String query = request.getParameter("query");
            String title = request.getParameter("title");
            String director = request.getParameter("director");
            String year = request.getParameter("year");
            String genre = request.getParameter("genre");
            String star = request.getParameter("star");

            List<Movie> movies;

            // Determine which search to perform based on parameters
            if (query != null && !query.isEmpty()) {
                // Full-text search
                movies = movieService.searchMovies(query);
            } else if (title != null && !title.isEmpty()) {
                // Search by title
                movies = movieService.getMoviesByTitle(title);
            } else if (director != null && !director.isEmpty()) {
                // Search by director
                movies = movieService.getMoviesByDirector(director);
            } else if (year != null && !year.isEmpty()) {
                try {
                    // Search by year
                    int yearVal = Integer.parseInt(year);
                    movies = movieService.getMoviesByYear(yearVal);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Invalid year format");
                    out.print(error.toString());
                    return;
                }
            } else if (genre != null && !genre.isEmpty()) {
                try {
                    // Search by genre ID
                    int genreId = Integer.parseInt(genre);
                    movies = movieService.getMoviesByGenre(genreId);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Invalid genre ID format");
                    out.print(error.toString());
                    return;
                }
            } else if (star != null && !star.isEmpty()) {
                // Search by star ID
                movies = movieService.getMoviesByStar(star);
            } else {
                // No search parameters provided
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "No search criteria provided");
                out.print(error.toString());
                return;
            }

            // Create JSON response with search results
            JSONArray moviesArray = new JSONArray();
            if (movies != null) {
                for (Movie movie : movies) {
                    JSONObject movieObj = new JSONObject();
                    movieObj.put("id", movie.getId());
                    movieObj.put("title", movie.getTitle());
                    movieObj.put("year", movie.getYear());
                    movieObj.put("director", movie.getDirector());
                    movieObj.put("bannerUrl", movie.getBannerUrl());
                    movieObj.put("trailerUrl", movie.getTrailerUrl());
                    moviesArray.put(movieObj);
                }
            }

            JSONObject result = new JSONObject();
            result.put("count", movies != null ? movies.size() : 0);
            result.put("movies", moviesArray);
            out.print(result.toString());

        } catch (Exception e) {
            logger.error("Error in doGet for SearchServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }
}