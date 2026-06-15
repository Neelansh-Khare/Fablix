package com.neelanshkhare.fabflix.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;
    // Map of Movie ID -> Quantity
    private Map<String, Integer> items;

    public Cart() {
        this.items = new HashMap<>();
    }

    public void addItem(String movieId, int quantity) {
        if (items.containsKey(movieId)) {
            items.put(movieId, items.get(movieId) + quantity);
        } else {
            items.put(movieId, quantity);
        }
    }

    public void updateItem(String movieId, int quantity) {
        if (quantity <= 0) {
            items.remove(movieId);
        } else {
            items.put(movieId, quantity);
        }
    }

    public void removeItem(String movieId) {
        items.remove(movieId);
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public int getTotalQuantity() {
        int total = 0;
        for (int quantity : items.values()) {
            total += quantity;
        }
        return total;
    }

    public void clear() {
        items.clear();
    }
}