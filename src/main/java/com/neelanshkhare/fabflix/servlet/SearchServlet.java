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
            String firstLetter = request.getParameter("firstLetter");
            
            // Pagination and Sorting
            String sortBy = request.getParameter("sortBy");
            if (sortBy == null || sortBy.isEmpty()) sortBy = "title";
            
            String sortOrder = request.getParameter("sortOrder");
            if (sortOrder == null || sortOrder.isEmpty()) sortOrder = "ASC";
            
            int page = 1;
            int pageSize = 10;
            
            try {
                String pageParam = request.getParameter("page");
                String pageSizeParam = request.getParameter("pageSize");
                if (pageParam != null && !pageParam.isEmpty()) page = Integer.parseInt(pageParam);
                if (pageSizeParam != null && !pageSizeParam.isEmpty()) pageSize = Integer.parseInt(pageSizeParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid pagination parameters, using defaults");
            }

            Integer yearVal = null;
            if (year != null && !year.isEmpty()) {
                try {
                    yearVal = Integer.parseInt(year);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Invalid year format");
                    out.print(error.toString());
                    return;
                }
            }

            Integer genreId = null;
            if (genre != null && !genre.isEmpty()) {
                try {
                    genreId = Integer.parseInt(genre);
                } catch (NumberFormatException e) {
                    // Could be genre name if we support it, but DAO expects ID
                    logger.warn("Invalid genre ID format: {}", genre);
                }
            }

            List<Movie> movies = movieService.searchMovies(
                query, title, yearVal, director, star, genreId, firstLetter, sortBy, sortOrder, page, pageSize
            );

            int totalCount = movieService.getCountMoviesFiltered(query, title, yearVal, director, star, genreId, firstLetter);

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
                    movieObj.put("rating", movie.getRating());
                    movieObj.put("numVotes", movie.getNumVotes());

                    // Add genres
                    JSONArray genresArray = new JSONArray();
                    for (com.neelanshkhare.fabflix.model.Genre genreObj : movie.getGenres()) {
                        JSONObject genreJson = new JSONObject();
                        genreJson.put("id", genreObj.getId());
                        genreJson.put("name", genreObj.getName());
                        genresArray.put(genreJson);
                    }
                    movieObj.put("genres", genresArray);

                    // Add stars
                    JSONArray starsArray = new JSONArray();
                    for (com.neelanshkhare.fabflix.model.Star starObj : movie.getStars()) {
                        JSONObject starJson = new JSONObject();
                        starJson.put("id", starObj.getId());
                        starJson.put("name", starObj.getName());
                        starsArray.put(starJson);
                    }
                    movieObj.put("stars", starsArray);

                    moviesArray.put(movieObj);
                }
            }

            JSONObject result = new JSONObject();
            result.put("currentPage", page);
            result.put("pageSize", pageSize);
            result.put("totalCount", totalCount);
            result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));
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