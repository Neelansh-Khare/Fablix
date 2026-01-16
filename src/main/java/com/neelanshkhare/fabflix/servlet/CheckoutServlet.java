package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.dao.OrderDAO;
import com.neelanshkhare.fabflix.dao.impl.OrderDAOImpl;
import com.neelanshkhare.fabflix.model.Cart;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Order;
import com.neelanshkhare.fabflix.model.OrderItem;
import com.neelanshkhare.fabflix.service.MovieService;
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

@WebServlet("/api/checkout")
public class CheckoutServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutServlet.class);
    private OrderDAO orderDAO;
    private MovieService movieService;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("9.99");

    @Override
    public void init() throws ServletException {
        super.init();
        orderDAO = new OrderDAOImpl();
        movieService = new MovieService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Validate User Session
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("customerId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JSONObject error = new JSONObject();
                error.put("message", "User must be logged in to checkout");
                out.print(error.toString());
                return;
            }

            int customerId = (int) session.getAttribute("customerId");
            
            // Get Cart
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null || cart.getTotalQuantity() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Cart is empty");
                out.print(error.toString());
                return;
            }

            // Simulate payment processing (read params if needed, e.g. credit card)
            String creditCard = request.getParameter("creditCard");
            if (creditCard == null || creditCard.isEmpty()) {
                // For simplicity, we might allow empty for now or validate
                // In a real app, we'd validate against the creditcards table
            }

            // Create Order
            Order order = new Order();
            order.setCustomerId(customerId);
            order.setStatus("COMPLETED"); // Assume payment success
            order.setPaymentMethod("Credit Card");
            
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();
                Movie movie = movieService.getMovie(movieId);
                
                if (movie != null) {
                    OrderItem item = new OrderItem();
                    item.setMovieId(movieId);
                    item.setMovieTitle(movie.getTitle());
                    item.setQuantity(quantity);
                    item.setUnitPrice(DEFAULT_PRICE);
                    item.setTotalPrice(DEFAULT_PRICE.multiply(BigDecimal.valueOf(quantity)));
                    
                    order.addOrderItem(item);
                    totalAmount = totalAmount.add(item.getTotalPrice());
                }
            }
            order.setTotalAmount(totalAmount);

            // Save to DB
            boolean success = orderDAO.createOrder(order);

            if (success) {
                // Clear Cart
                cart.clear();
                
                JSONObject result = new JSONObject();
                result.put("message", "Order placed successfully");
                result.put("orderId", order.getId());
                out.print(result.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to process order");
                out.print(error.toString());
            }

        } catch (Exception e) {
            logger.error("Error during checkout", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Internal server error during checkout: " + e.getMessage());
            out.print(error.toString());
        }
    }
}