package com.neelanshkhare.fabflix.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "AdminFilter", urlPatterns = {"/api/admin/*", "/_dashboard"})
public class AdminFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        boolean isLoggedIn = (session != null && session.getAttribute("customerId") != null);
        String role = isLoggedIn ? (String) session.getAttribute("customerRole") : null;

        if (isLoggedIn && "admin".equalsIgnoreCase(role)) {
            chain.doFilter(request, response);
        } else {
            // Check if it's an API call or page request
            String uri = httpRequest.getRequestURI();
            if (uri.startsWith(httpRequest.getContextPath() + "/api/")) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"message\": \"Admin access required\"}");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.jsp"); // Or login page
            }
        }
    }

    @Override
    public void destroy() {
    }
}
