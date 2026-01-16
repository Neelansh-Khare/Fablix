package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.model.Cart;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/api/cart")
public class CartServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CartServlet.class);
    private MovieService movieService;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("9.99");

    @Override
    public void init() throws ServletException {
        super.init();
        movieService = new MovieService();
    }

    private Cart getCartFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Cart cart = getCartFromSession(request);
            JSONArray cartItems = new JSONArray();
            BigDecimal total = BigDecimal.ZERO;

            for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();

                Movie movie = movieService.getMovie(movieId);
                if (movie != null) {
                    JSONObject item = new JSONObject();
                    item.put("id", movie.getId());
                    item.put("title", movie.getTitle());
                    item.put("quantity", quantity);
                    item.put("price", DEFAULT_PRICE);
                    
                    BigDecimal itemTotal = DEFAULT_PRICE.multiply(BigDecimal.valueOf(quantity));
                    item.put("total", itemTotal);
                    
                    total = total.add(itemTotal);
                    cartItems.put(item);
                }
            }

            JSONObject result = new JSONObject();
            result.put("items", cartItems);
            result.put("total", total);
            result.put("count", cart.getTotalQuantity());

            out.print(result.toString());

        } catch (Exception e) {
            logger.error("Error retrieving cart", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error retrieving cart");
            out.print(error.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String movieId = request.getParameter("movieId");
            String quantityStr = request.getParameter("quantity");
            String action = request.getParameter("action"); // 'add', 'update', 'remove', 'clear'

            Cart cart = getCartFromSession(request);

            if ("clear".equals(action)) {
                cart.clear();
                JSONObject result = new JSONObject();
                result.put("message", "Cart cleared");
                result.put("count", 0);
                out.print(result.toString());
                return;
            }

            if (movieId == null || movieId.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject().put("message", "Movie ID is required").toString());
                return;
            }

            if ("remove".equals(action)) {
                cart.removeItem(movieId);
            } else {
                int quantity = 1;
                if (quantityStr != null && !quantityStr.isEmpty()) {
                    try {
                        quantity = Integer.parseInt(quantityStr);
                    } catch (NumberFormatException e) {
                        // ignore, use default 1
                    }
                }

                if ("update".equals(action)) {
                    cart.updateItem(movieId, quantity);
                } else {
                    // Default to add
                    cart.addItem(movieId, quantity);
                }
            }

            JSONObject result = new JSONObject();
            result.put("message", "Cart updated");
            result.put("count", cart.getTotalQuantity());
            out.print(result.toString());

        } catch (Exception e) {
            logger.error("Error updating cart", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("message", "Error updating cart").toString());
        }
    }
}