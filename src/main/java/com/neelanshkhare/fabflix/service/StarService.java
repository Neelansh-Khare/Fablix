package com.neelanshkhare.fabflix.service;

import com.neelanshkhare.fabflix.dao.StarDAO;
import com.neelanshkhare.fabflix.dao.impl.StarDAOImpl;
import com.neelanshkhare.fabflix.model.Star;

import java.util.List;

public class StarService {
    private StarDAO starDAO;

    public StarService() {
        this.starDAO = new StarDAOImpl();
    }

    public Star getStar(String id) {
        return starDAO.findById(id);
    }

    public List<Star> getStarsByName(String name) {
        return starDAO.findByName(name);
    }

    public List<Star> getStarsByMovie(String movieId) {
        return starDAO.findByMovie(movieId);
    }

    public List<Star> listStars(int page, int pageSize) {
        return starDAO.listStars(page, pageSize);
    }

    public boolean addStar(Star star) {
        return starDAO.insert(star);
    }

    public boolean updateStar(Star star) {
        return starDAO.update(star);
    }

    public boolean deleteStar(String id) {
        return starDAO.delete(id);
    }
}