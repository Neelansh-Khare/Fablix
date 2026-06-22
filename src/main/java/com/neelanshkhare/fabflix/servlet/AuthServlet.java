package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.util.CsrfUtil;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        JSONObject result = new JSONObject();

        if (pathInfo != null && pathInfo.equals("/status")) {
            // Check login status
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("customerId") != null) {
                result.put("loggedIn", true);
                result.put("id", session.getAttribute("customerId"));
                result.put("name", session.getAttribute("customerName"));
                result.put("email", session.getAttribute("customerEmail"));
                if (session.getAttribute("customerRole") != null) {
                    result.put("role", session.getAttribute("customerRole"));
                }
                result.put("csrfToken", CsrfUtil.getOrCreateToken(session));
            } else {
                result.put("loggedIn", false);
            }
            out.print(result.toString());
        } else if (pathInfo != null && pathInfo.equals("/logout")) {
            // Handle logout
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            result.put("message", "Successfully logged out");
            out.print(result.toString());
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("message", "Invalid request");
            out.print(error.toString());
        }
    }
}
