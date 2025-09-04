package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.service.GenreService;
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

@WebServlet("/api/genres/*")
public class GenreServlet extends HttpServlet {
    private GenreService genreService;

    @Override
    public void init() throws ServletException {
        super.init();
        genreService = new GenreService();
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
                // Get all genres
                List<Genre> genres = genreService.getAllGenres();

                JSONArray genresArray = new JSONArray();
                for (Genre genre : genres) {
                    JSONObject genreObj = new JSONObject();
                    genreObj.put("id", genre.getId());
                    genreObj.put("name", genre.getName());

                    genresArray.put(genreObj);
                }

                JSONObject result = new JSONObject();
                result.put("genres", genresArray);
                out.print(result.toString());

            } else {
                // Get a single genre by ID
                try {
                    int genreId = Integer.parseInt(pathInfo.substring(1));
                    Genre genre = genreService.getGenre(genreId);

                    if (genre != null) {
                        JSONObject genreObj = new JSONObject();
                        genreObj.put("id", genre.getId());
                        genreObj.put("name", genre.getName());

                        out.print(genreObj.toString());
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        JSONObject error = new JSONObject();
                        error.put("message", "Genre not found");
                        out.print(error.toString());
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Invalid genre ID format");
                    out.print(error.toString());
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            while ((line = request.getReader().readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String payload = buffer.toString();
        PrintWriter out = response.getWriter();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            Genre genre = new Genre();
            genre.setName(jsonRequest.getString("name"));

            boolean success = genreService.addGenre(genre);

            if (success) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                JSONObject result = new JSONObject();
                result.put("message", "Genre created successfully");
                result.put("id", genre.getId());
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to create genre");
                out.print(error.toString());
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
            e.printStackTrace();
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
            error.put("message", "Genre ID is required");
            out.print(error.toString());
            return;
        }

        try {
            int genreId = Integer.parseInt(pathInfo.substring(1));

            StringBuilder buffer = new StringBuilder();
            String line;
            try {
                while ((line = request.getReader().readLine()) != null) {
                    buffer.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String payload = buffer.toString();

            JSONObject jsonRequest = new JSONObject(payload);

            Genre genre = new Genre();
            genre.setId(genreId);
            genre.setName(jsonRequest.getString("name"));

            boolean success = genreService.updateGenre(genre);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Genre updated successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to update genre");
                out.print(error.toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("message", "Invalid genre ID format");
            out.print(error.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
            e.printStackTrace();
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
            error.put("message", "Genre ID is required");
            out.print(error.toString());
            return;
        }

        try {
            int genreId = Integer.parseInt(pathInfo.substring(1));
            boolean success = genreService.deleteGenre(genreId);

            if (success) {
                JSONObject result = new JSONObject();
                result.put("message", "Genre deleted successfully");
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSONObject error = new JSONObject();
                error.put("message", "Genre not found or could not be deleted");
                out.print(error.toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("message", "Invalid genre ID format");
            out.print(error.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error: " + e.getMessage());
            out.print(error.toString());
            e.printStackTrace();
        }
    }
}