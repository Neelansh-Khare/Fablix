package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.service.StarService;
import com.neelanshkhare.fabflix.util.CsrfUtil;
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

@WebServlet("/api/admin/movie")
public class AdminMovieServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminMovieServlet.class);
    private MovieService movieService;
    private StarService starService;

    @Override
    public void init() throws ServletException {
        this.movieService = new MovieService();
        this.starService = new StarService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject result = new JSONObject();

        if (!CsrfUtil.validateToken(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.put("status", "error");
            result.put("message", "Invalid or missing CSRF token");
            out.print(result.toString());
            return;
        }

        try {
            if ("addMovie".equals(action)) {
                String title = request.getParameter("title");
                int year = Integer.parseInt(request.getParameter("year"));
                String director = request.getParameter("director");
                String starName = request.getParameter("starName");
                String genreName = request.getParameter("genreName");

                String message = movieService.addMovieWithProcedure(title, year, director, starName, genreName);
                result.put("status", message.startsWith("Success") ? "success" : "error");
                result.put("message", message);
                
            } else if ("addStar".equals(action)) {
                String name = request.getParameter("name");
                String birthYearStr = request.getParameter("birthYear");
                Integer birthYear = (birthYearStr != null && !birthYearStr.isEmpty()) ? Integer.parseInt(birthYearStr) : null;

                String message = starService.addStarWithProcedure(name, birthYear);
                result.put("status", message.startsWith("Success") ? "success" : "error");
                result.put("message", message);
                
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("status", "error");
                result.put("message", "Invalid action");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "error");
            logger.error("Error in AdminMovieServlet", e);
            result.put("message", "An unexpected error occurred. Please try again.");
        }

        out.print(result.toString());
    }
}
