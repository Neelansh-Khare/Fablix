package com.neelanshkhare.fabflix.dao;

import com.neelanshkhare.fabflix.model.Genre;
import java.util.List;

public interface GenreDAO {
    Genre findById(int id);
    Genre findByName(String name);
    List<Genre> findByMovie(String movieId);
    List<Genre> listGenres();
    boolean insert(Genre genre);
    boolean update(Genre genre);
    boolean delete(int id);
}