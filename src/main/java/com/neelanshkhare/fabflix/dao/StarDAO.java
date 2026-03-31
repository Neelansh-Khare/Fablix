package com.neelanshkhare.fabflix.dao;

import com.neelanshkhare.fabflix.model.Star;
import java.util.List;

public interface StarDAO {
    Star findById(String id);
    List<Star> findByName(String name);
    List<Star> findByMovie(String movieId);
    List<Star> listStars(int page, int pageSize);
    boolean insert(Star star);
    String addStarWithProcedure(String name, Integer birthYear);
    boolean update(Star star);
    boolean delete(String id);
}