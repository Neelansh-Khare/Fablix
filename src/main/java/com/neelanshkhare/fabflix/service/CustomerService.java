package com.neelanshkhare.fabflix.service;

import com.neelanshkhare.fabflix.dao.CustomerDAO;
import com.neelanshkhare.fabflix.dao.impl.CustomerDAOImpl;
import com.neelanshkhare.fabflix.model.Customer;

public class CustomerService {
    private CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAOImpl();
    }

    public Customer getCustomer(int id) {
        return customerDAO.findById(id);
    }

    public Customer getCustomerByEmail(String email) {
        return customerDAO.findByEmail(email);
    }

    public boolean registerCustomer(Customer customer) {
        return customerDAO.insert(customer);
    }

    public boolean updateCustomer(Customer customer) {
        return customerDAO.update(customer);
    }

    public boolean deleteCustomer(int id) {
        return customerDAO.delete(id);
    }

    public boolean login(String email, String password) {
        return customerDAO.verifyPassword(email, password);
    }
}