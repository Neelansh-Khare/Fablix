package com.neelanshkhare.fabflix.dao;

import com.neelanshkhare.fabflix.model.Customer;
import java.util.List;

public interface CustomerDAO {
    Customer findById(int id);
    Customer findByEmail(String email);
    boolean insert(Customer customer);
    boolean update(Customer customer);
    boolean delete(int id);
    boolean verifyPassword(String email, String password);
}