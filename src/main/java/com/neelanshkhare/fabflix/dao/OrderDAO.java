package com.neelanshkhare.fabflix.dao;

import com.neelanshkhare.fabflix.model.Order;
import com.neelanshkhare.fabflix.model.OrderItem;

import java.util.List;

public interface OrderDAO {
    // Create a new order (and its items)
    boolean createOrder(Order order);

    // Find order by ID
    Order findById(int id);

    // Find orders by customer ID
    List<Order> findByCustomer(int customerId);
}