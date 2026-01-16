package com.neelanshkhare.fabflix.dao.impl;

import com.neelanshkhare.fabflix.dao.OrderDAO;
import com.neelanshkhare.fabflix.model.Order;
import com.neelanshkhare.fabflix.model.OrderItem;
import com.neelanshkhare.fabflix.util.DBConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAOImpl implements OrderDAO {
    private static final Logger logger = LoggerFactory.getLogger(OrderDAOImpl.class);

    @Override
    public boolean createOrder(Order order) {
        String insertOrderSql = "INSERT INTO orders (customer_id, total_amount, status, payment_method, order_date) VALUES (?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, movie_id, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?)";
        // Also insert into legacy sales table for backward compatibility
        String insertSaleSql = "INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, ?)";

        boolean success = false;

        try (Connection conn = DBConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert Order
                int orderId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, order.getCustomerId());
                    stmt.setBigDecimal(2, order.getTotalAmount());
                    stmt.setString(3, order.getStatus());
                    stmt.setString(4, order.getPaymentMethod());
                    stmt.setTimestamp(5, order.getOrderDate());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating order failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            orderId = generatedKeys.getInt(1);
                            order.setId(orderId);
                        } else {
                            throw new SQLException("Creating order failed, no ID obtained.");
                        }
                    }
                }

                // 2. Insert Order Items
                try (PreparedStatement stmt = conn.prepareStatement(insertItemSql)) {
                    for (OrderItem item : order.getOrderItems()) {
                        stmt.setInt(1, orderId);
                        stmt.setString(2, item.getMovieId());
                        stmt.setInt(3, item.getQuantity());
                        stmt.setBigDecimal(4, item.getUnitPrice());
                        stmt.setBigDecimal(5, item.getTotalPrice());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                
                // 3. Insert into legacy sales table (one row per item)
                try (PreparedStatement stmt = conn.prepareStatement(insertSaleSql)) {
                    for (OrderItem item : order.getOrderItems()) {
                        // If quantity > 1, we could insert multiple rows, but let's just insert one per unique movie for now
                        // or loop quantity times. Let's loop quantity times to be accurate to "sales" count
                        for(int i=0; i < item.getQuantity(); i++) {
                            stmt.setInt(1, order.getCustomerId());
                            stmt.setString(2, item.getMovieId());
                            stmt.setDate(3, new java.sql.Date(order.getOrderDate().getTime()));
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                conn.commit();
                success = true;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error creating order, rolling back", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error creating order", e);
        }

        return success;
    }

    @Override
    public Order findById(int id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        Order order = null;

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    order = mapRowToOrder(rs);
                    order.setOrderItems(findItemsByOrderId(conn, id));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding order by ID: {}", id, e);
        }

        return order;
    }

    @Override
    public List<Order> findByCustomer(int customerId) {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_date DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapRowToOrder(rs);
                    // Loading items for list might be heavy, consider lazy loading or simpler DTO
                    // For now, let's load them
                    order.setOrderItems(findItemsByOrderId(conn, order.getId()));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding orders for customer ID: {}", customerId, e);
        }

        return orders;
    }

    private List<OrderItem> findItemsByOrderId(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT oi.*, m.title as movie_title FROM order_items oi " +
                     "JOIN movies m ON oi.movie_id = m.id " +
                     "WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setMovieId(rs.getString("movie_id"));
                    item.setMovieTitle(rs.getString("movie_title"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setTotalPrice(rs.getBigDecimal("total_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setOrderDate(rs.getTimestamp("order_date"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        return order;
    }
}