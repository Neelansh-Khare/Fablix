package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Star;
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

@WebServlet("/api/movies/*")
public class MovieServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MovieServlet.class);
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

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all movies with pagination
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

                List<Movie> movies = movieService.listMovies(page, pageSize);
                int totalCount = movieService.getTotalMoviesCount();

                JSONObject result = new JSONObject();
                result.put("currentPage", page);
                result.put("pageSize", pageSize);
                result.put("totalCount", totalCount);
                result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

                JSONArray moviesArray = new JSONArray();
                for (Movie movie : movies) {
                    JSONObject movieObj = new JSONObject();
                    movieObj.put("id", movie.getId());
                    movieObj.put("title", movie.getTitle());
                    movieObj.put("year", movie.getYear());
                    movieObj.put("director", movie.getDirector());
                    movieObj.put("bannerUrl", movie.getBannerUrl());
                    movieObj.put("trailerUrl", movie.getTrailerUrl());

                    // Add genres
                    JSONArray genresArray = new JSONArray();
                    for (Genre genre : movie.getGenres()) {
                        JSONObject genreObj = new JSONObject();
                        genreObj.put("id", genre.getId());
                        genreObj.put("name", genre.getName());
                        genresArray.put(genreObj);
                    }
                    movieObj.put("genres", genresArray);

                    // Add stars
                    JSONArray starsArray = new JSONArray();
                    for (Star star : movie.getStars()) {
                        JSONObject starObj = new JSONObject();
                        starObj.put("id", star.getId());
                        starObj.put("name", star.getName());
                        starsArray.put(starObj);
                    }
                    movieObj.put("stars", starsArray);

                    moviesArray.put(movieObj);
                }

                result.put("movies", moviesArray);
                out.print(result.toString());

            } else {
                // Get a single movie by ID
                String movieId = pathInfo.substring(1);
                Movie movie = movieService.getMovie(movieId);

                if (movie != null) {
                    JSONObject movieObj = new JSONObject();
                    movieObj.put("id", movie.getId());
                    movieObj.put("title", movie.getTitle());
                    movieObj.put("year", movie.getYear());
                    movieObj.put("director", movie.getDirector());
                    movieObj.put("bannerUrl", movie.getBannerUrl());
                    movieObj.put("trailerUrl", movie.getTrailerUrl());

                    // Add genres
                    JSONArray genresArray = new JSONArray();
                    for (Genre genre : movie.getGenres()) {
                        JSONObject genreObj = new JSONObject();
                        genreObj.put("id", genre.getId());
                        genreObj.put("name", genre.getName());
                        genresArray.put(genreObj);
                    }
                    movieObj.put("genres", genresArray);

                    // Add stars
                    JSONArray starsArray = new JSONArray();
                    for (Star star : movie.getStars()) {
                        JSONObject starObj = new JSONObject();
                        starObj.put("id", star.getId());
                        starObj.put("name", star.getName());
                        starObj.put("birthYear", star.getBirthYear());
                        starsArray.put(starObj);
                    }
                    movieObj.put("stars", starsArray);

                    // Add recommendations
                    JSONArray similarMoviesArray = new JSONArray();
                    List<Movie> similarMovies = movieService.getSimilarMovies(movieId, 5);
                    for (Movie simMovie : similarMovies) {
                        JSONObject simObj = new JSONObject();
                        simObj.put("id", simMovie.getId());
                        simObj.put("title", simMovie.getTitle());
                        simObj.put("year", simMovie.getYear());
                        simObj.put("bannerUrl", simMovie.getBannerUrl());
                        similarMoviesArray.put(simObj);
                    }
                    movieObj.put("similarMovies", similarMoviesArray);

                    JSONArray coPurchaseArray = new JSONArray();
                    List<Movie> coPurchaseMovies = movieService.getCoPurchaseRecommendations(movieId, 5);
                    for (Movie cpMovie : coPurchaseMovies) {
                        JSONObject cpObj = new JSONObject();
                        cpObj.put("id", cpMovie.getId());
                        cpObj.put("title", cpMovie.getTitle());
                        cpObj.put("year", cpMovie.getYear());
                        cpObj.put("bannerUrl", cpMovie.getBannerUrl());
                        coPurchaseArray.put(cpObj);
                    }
                    movieObj.put("coPurchaseRecommendations", coPurchaseArray);

                    out.print(movieObj.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JSONObject error = new JSONObject();
                    error.put("message", "Movie not found");
                    out.print(error.toString());
                }
            }
        } catch (Exception e) {
            logger.error("Error in doGet for MovieServlet", e);
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
            logger.error("Error reading request body in MovieServlet", e);
        }

        String payload = buffer.toString();
        PrintWriter out = response.getWriter();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            Movie movie = new Movie();
            movie.setId(jsonRequest.getString("id"));
            movie.setTitle(jsonRequest.getString("title"));
            movie.setYear(jsonRequest.getInt("year"));
            movie.setDirector(jsonRequest.getString("director"));

            if (jsonRequest.has("bannerUrl")) {
                movie.setBannerUrl(jsonRequest.getString("bannerUrl"));
            }

            if (jsonRequest.has("trailerUrl")) {
                movie.setTrailerUrl(jsonRequest.getString("trailerUrl"));
            }

            // Process genres if provided
            if (jsonRequest.has("genres")) {
                JSONArray genresArray = jsonRequest.getJSONArray("genres");
                for (int i = 0; i < genresArray.length(); i++) {
                    JSONObject genreObj = genresArray.getJSONObject(i);
                    Genre genre = new Genre();
                    genre.setId(genreObj.getInt("id"));
                    genre.setName(genreObj.getString("name"));
                    movie.addGenre(genre);
                }
            }

            // Process stars if provided
            if (jsonRequest.has("stars")) {
                JSONArray starsArray = jsonRequest.getJSONArray("stars");
                for (int i = 0; i < starsArray.length(); i++) {
                    JSONObject starObj = starsArray.getJSONObject(i);
                    Star star = new Star();
                    star.setId(starObj.getString("id"));
                    star.setName(starObj.getString("name"));
                    movie.addStar(star);
                }
            }

            boolean success = movieService.addMovie(movie);

            if (success) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                JSONObject result = new JSONObject();
                result.put("message", "Movie created successfully");
                result.put("id", movie.getId());
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to create movie");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doPost for MovieServlet", e);
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
            error.put("message", "Movie ID is required");
            out.print(error.toString());
            return;
        }

        String movieId = pathInfo.substring(1);
        StringBuilder buffer = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            logger.error("Error reading request body in MovieServlet", e);
        }

        String payload = buffer.toString();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            Movie movie = new Movie();
            movie.setId(movieId);
            movie.setTitle(jsonRequest.getString("title"));
            movie.setYear(jsonRequest.getInt("year"));
            movie.setDirector(jsonRequest.getString("director"));

            if (jsonRequest.has("bannerUrl")) {
                movie.setBannerUrl(jsonRequest.getString("bannerUrl"));
            }

            if (jsonRequest.has("trailerUrl")) {
                movie.setTrailerUrl(jsonRequest.getString("trailerUrl"));
            }

            // Process genres if provided
            if (jsonRequest.has("genres")) {
                JSONArray genresArray = jsonRequest.getJSONArray("genres");
                for (int i = 0; i < genresArray.length(); i++) {
                    JSONObject genreObj = genresArray.getJSONObject(i);
                    Genre genre = new Genre();
                    genre.setId(genreObj.getInt("id"));
                    movie.addGenre(genre);
                }
            }

            // Process stars if provided
            if (jsonRequest.has("stars")) {
                JSONArray starsArray = jsonRequest.getJSONArray("stars");
                for (int i = 0; i < starsArray.length(); i++) {
                    JSONObject starObj = starsArray.getJSONObject(i);
                    Star star = new Star();
                    star.setId(starObj.getString("id"));
                    movie.addStar(star);
                }
            }

            boolean success = movieService.updateMovie(movie);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Movie updated successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to update movie");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doPut for MovieServlet", e);
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
            error.put("message", "Movie ID is required");
            out.print(error.toString());
            return;
        }

        String movieId = pathInfo.substring(1);

        try {
            boolean success = movieService.deleteMovie(movieId);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Movie deleted successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSONObject error = new JSONObject();
                error.put("message", "Movie not found or could not be deleted");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error in doDelete for MovieServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
        }
    }
}