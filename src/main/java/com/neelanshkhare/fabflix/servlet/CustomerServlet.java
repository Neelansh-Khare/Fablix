package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.dao.OrderDAO;
import com.neelanshkhare.fabflix.dao.impl.OrderDAOImpl;
import com.neelanshkhare.fabflix.model.Customer;
import com.neelanshkhare.fabflix.model.Order;
import com.neelanshkhare.fabflix.model.OrderItem;
import com.neelanshkhare.fabflix.service.CustomerService;
import com.neelanshkhare.fabflix.util.CsrfUtil;
import com.neelanshkhare.fabflix.util.RecaptchaUtil;
import com.neelanshkhare.fabflix.util.RateLimiterUtil;
import com.neelanshkhare.fabflix.util.PasswordPolicyUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/customers/*")
public class CustomerServlet extends HttpServlet {
    private CustomerService customerService;
    private OrderDAO orderDAO;
    private static final Logger LOGGER = Logger.getLogger(CustomerServlet.class.getName());

    @Override
    public void init() throws ServletException {
        super.init();
        customerService = new CustomerService();
        orderDAO = new OrderDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JSONObject error = new JSONObject();
            error.put("message", "Unauthorized access");
            response.getWriter().print(error.toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                int customerId = (Integer) session.getAttribute("customerId");
                Customer customer = customerService.getCustomer(customerId);

                if (customer != null) {
                    JSONObject customerObj = new JSONObject();
                    customerObj.put("id", customer.getId());
                    customerObj.put("firstName", customer.getFirstName());
                    customerObj.put("lastName", customer.getLastName());
                    customerObj.put("email", customer.getEmail());
                    customerObj.put("address", customer.getAddress());
                    out.print(customerObj.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JSONObject error = new JSONObject();
                    error.put("message", "Customer not found");
                    out.print(error.toString());
                }
            } else if (pathInfo.equals("/orders")) {
                int customerId = (Integer) session.getAttribute("customerId");
                List<Order> orders = orderDAO.findByCustomer(customerId);

                JSONArray ordersArray = new JSONArray();
                for (Order order : orders) {
                    JSONObject orderObj = new JSONObject();
                    orderObj.put("id", order.getId());
                    orderObj.put("orderDate", order.getOrderDate().toString());
                    orderObj.put("totalAmount", order.getTotalAmount());
                    orderObj.put("status", order.getStatus());
                    orderObj.put("paymentMethod", order.getPaymentMethod());

                    JSONArray itemsArray = new JSONArray();
                    for (OrderItem item : order.getOrderItems()) {
                        JSONObject itemObj = new JSONObject();
                        itemObj.put("movieId", item.getMovieId());
                        itemObj.put("movieTitle", item.getMovieTitle());
                        itemObj.put("quantity", item.getQuantity());
                        itemObj.put("unitPrice", item.getUnitPrice());
                        itemObj.put("totalPrice", item.getTotalPrice());
                        itemsArray.put(itemObj);
                    }
                    orderObj.put("items", itemsArray);
                    ordersArray.put(orderObj);
                }

                out.print(ordersArray.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Invalid request");
                out.print(error.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer GET request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "An unexpected error occurred. Please try again.");
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
        try {
            while ((line = request.getReader().readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading request body", e);
        }

        String payload = buffer.toString();
        PrintWriter out = response.getWriter();

        try {
            JSONObject jsonRequest = new JSONObject(payload);
            String action = jsonRequest.getString("action");

            if (!jsonRequest.has("recaptcha")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "reCAPTCHA validation required");
                out.print(error.toString());
                LOGGER.warning("Request missing recaptcha field");
                return;
            }

            String recaptchaResponse = jsonRequest.getString("recaptcha");
            LOGGER.info("Received reCAPTCHA response: " + (recaptchaResponse.length() > 20 ?
                    recaptchaResponse.substring(0, 20) + "..." : recaptchaResponse));

            boolean isRecaptchaValid = RecaptchaUtil.verifyRecaptcha(recaptchaResponse);

            if (!isRecaptchaValid) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "reCAPTCHA validation failed. Please try again.");
                out.print(error.toString());
                LOGGER.warning("reCAPTCHA validation failed");
                return;
            }

            if ("register".equals(action)) {
                String password = jsonRequest.getString("password");
                if (!PasswordPolicyUtil.isValidPassword(password)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Password must be at least 8 characters long and contain at least 3 of the following: lowercase letters, uppercase letters, digits, and special characters.");
                    out.print(error.toString());
                    return;
                }

                Customer customer = new Customer();
                customer.setFirstName(jsonRequest.getString("firstName"));
                customer.setLastName(jsonRequest.getString("lastName"));
                customer.setEmail(jsonRequest.getString("email"));
                customer.setPassword(jsonRequest.getString("password"));
                customer.setAddress(jsonRequest.getString("address"));
                customer.setCcId(jsonRequest.getString("ccId"));

                Customer existingCustomer = customerService.getCustomerByEmail(customer.getEmail());
                if (existingCustomer != null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    JSONObject error = new JSONObject();
                    error.put("message", "Email address is already registered");
                    out.print(error.toString());
                    return;
                }

                boolean success = customerService.registerCustomer(customer);

                if (success) {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    JSONObject result = new JSONObject();
                    result.put("message", "Registration successful");
                    result.put("id", customer.getId());
                    out.print(result.toString());
                    LOGGER.log(Level.INFO, "New customer registered: " + customer.getEmail());
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Registration failed");
                    out.print(error.toString());
                    LOGGER.log(Level.WARNING, "Failed to register customer: " + customer.getEmail());
                }
            } else if ("login".equals(action)) {
                String email = jsonRequest.getString("email");
                String password = jsonRequest.getString("password");

                String ipAddress = request.getRemoteAddr();
                if (!RateLimiterUtil.allowRequest(ipAddress)) {
                    response.setStatus(429);
                    JSONObject error = new JSONObject();
                    error.put("message", "Too many login attempts. Please try again later.");
                    out.print(error.toString());
                    LOGGER.log(Level.WARNING, "Rate limit exceeded for IP: " + ipAddress);
                    return;
                }

                boolean isValid = customerService.login(email, password);

                if (isValid) {
                    Customer customer = customerService.getCustomerByEmail(email);

                    // Invalidate old session to prevent session fixation (C2)
                    HttpSession oldSession = request.getSession(false);
                    if (oldSession != null) {
                        oldSession.invalidate();
                    }
                    HttpSession session = request.getSession(true);
                    session.setAttribute("customerId", customer.getId());
                    session.setAttribute("customerEmail", customer.getEmail());
                    session.setAttribute("customerName", customer.getFirstName() + " " + customer.getLastName());
                    session.setAttribute("customerRole", customer.getRole());
                    session.setMaxInactiveInterval(30 * 60);

                    RateLimiterUtil.loginSucceeded(ipAddress);

                    // Generate and return CSRF token so client can include it in future state-changing requests
                    String csrfToken = CsrfUtil.getOrCreateToken(session);

                    JSONObject result = new JSONObject();
                    result.put("message", "Login successful");
                    result.put("id", customer.getId());
                    result.put("name", customer.getFirstName() + " " + customer.getLastName());
                    result.put("csrfToken", csrfToken);
                    out.print(result.toString());
                    LOGGER.log(Level.INFO, "Customer logged in: " + customer.getEmail());
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    JSONObject error = new JSONObject();
                    error.put("message", "Invalid credentials");
                    out.print(error.toString());
                    LOGGER.log(Level.WARNING, "Failed login attempt for email: " + email + " from IP: " + ipAddress);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Invalid action");
                out.print(error.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer POST request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "An unexpected error occurred. Please try again.");
            out.print(error.toString());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JSONObject error = new JSONObject();
            error.put("message", "Unauthorized access");
            response.getWriter().print(error.toString());
            return;
        }

        // CSRF validation (H4)
        if (!CsrfUtil.validateToken(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            JSONObject error = new JSONObject();
            error.put("message", "Invalid or missing CSRF token");
            response.getWriter().print(error.toString());
            return;
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            while ((line = request.getReader().readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading request body", e);
        }

        String payload = buffer.toString();
        PrintWriter out = response.getWriter();

        try {
            JSONObject jsonRequest = new JSONObject(payload);

            int customerId = (Integer) session.getAttribute("customerId");
            Customer customer = customerService.getCustomer(customerId);

            if (customer != null) {
                if (jsonRequest.has("password")) {
                    String newPassword = jsonRequest.getString("password");

                    if (!PasswordPolicyUtil.isValidPassword(newPassword)) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        JSONObject error = new JSONObject();
                        error.put("message", "Password must be at least 8 characters long and contain at least 3 of the following: lowercase letters, uppercase letters, digits, and special characters.");
                        out.print(error.toString());
                        return;
                    }

                    if (!jsonRequest.has("currentPassword")) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        JSONObject error = new JSONObject();
                        error.put("message", "Current password is required to update password");
                        out.print(error.toString());
                        return;
                    }

                    String currentPassword = jsonRequest.getString("currentPassword");
                    if (!customerService.login(customer.getEmail(), currentPassword)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        JSONObject error = new JSONObject();
                        error.put("message", "Current password is incorrect");
                        out.print(error.toString());
                        return;
                    }

                    customer.setPassword(newPassword);
                }

                customer.setFirstName(jsonRequest.getString("firstName"));
                customer.setLastName(jsonRequest.getString("lastName"));
                customer.setAddress(jsonRequest.getString("address"));

                String newEmail = jsonRequest.getString("email");
                if (!newEmail.equals(customer.getEmail())) {
                    Customer existingCustomer = customerService.getCustomerByEmail(newEmail);
                    if (existingCustomer != null) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        JSONObject error = new JSONObject();
                        error.put("message", "Email address is already in use");
                        out.print(error.toString());
                        return;
                    }
                }
                customer.setEmail(newEmail);

                if (jsonRequest.has("ccId")) {
                    customer.setCcId(jsonRequest.getString("ccId"));
                }

                boolean success = customerService.updateCustomer(customer);

                if (success) {
                    session.setAttribute("customerEmail", customer.getEmail());
                    session.setAttribute("customerName", customer.getFirstName() + " " + customer.getLastName());

                    JSONObject result = new JSONObject();
                    result.put("message", "Customer updated successfully");
                    out.print(result.toString());
                    LOGGER.log(Level.INFO, "Customer updated: " + customer.getEmail());
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject error = new JSONObject();
                    error.put("message", "Failed to update customer");
                    out.print(error.toString());
                    LOGGER.log(Level.WARNING, "Failed to update customer: " + customer.getEmail());
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSONObject error = new JSONObject();
                error.put("message", "Customer not found");
                out.print(error.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer PUT request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "An unexpected error occurred. Please try again.");
            out.print(error.toString());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JSONObject error = new JSONObject();
            error.put("message", "Unauthorized access");
            response.getWriter().print(error.toString());
            return;
        }

        // CSRF validation
        if (!CsrfUtil.validateToken(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            JSONObject error = new JSONObject();
            error.put("message", "Invalid or missing CSRF token");
            response.getWriter().print(error.toString());
            return;
        }

        PrintWriter out = response.getWriter();

        try {
            int customerId = (Integer) session.getAttribute("customerId");

            StringBuilder buffer = new StringBuilder();
            String line;
            try {
                while ((line = request.getReader().readLine()) != null) {
                    buffer.append(line);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error reading request body", e);
            }

            String payload = buffer.toString();
            JSONObject jsonRequest = new JSONObject(payload);

            if (!jsonRequest.has("password")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("message", "Password confirmation is required to delete account");
                out.print(error.toString());
                return;
            }

            String password = jsonRequest.getString("password");
            String email = (String) session.getAttribute("customerEmail");

            if (!customerService.login(email, password)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JSONObject error = new JSONObject();
                error.put("message", "Incorrect password");
                out.print(error.toString());
                return;
            }

            boolean success = customerService.deleteCustomer(customerId);

            if (success) {
                session.invalidate();
                JSONObject result = new JSONObject();
                result.put("message", "Account deleted successfully");
                out.print(result.toString());
                LOGGER.log(Level.INFO, "Customer account deleted: " + email);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSONObject error = new JSONObject();
                error.put("message", "Failed to delete account");
                out.print(error.toString());
                LOGGER.log(Level.WARNING, "Failed to delete customer account: " + email);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer DELETE request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "An unexpected error occurred. Please try again.");
            out.print(error.toString());
        }
    }
}
