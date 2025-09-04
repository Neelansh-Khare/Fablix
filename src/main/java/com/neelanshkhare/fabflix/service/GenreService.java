package com.neelanshkhare.fabflix.service;

import com.neelanshkhare.fabflix.dao.GenreDAO;
import com.neelanshkhare.fabflix.dao.impl.GenreDAOImpl;
import com.neelanshkhare.fabflix.model.Genre;

import java.util.List;

public class GenreService {
    private GenreDAO genreDAO;

    public GenreService() {
        this.genreDAO = new GenreDAOImpl();
    }

    public Genre getGenre(int id) {
        return genreDAO.findById(id);
    }

    public Genre getGenreByName(String name) {
        return genreDAO.findByName(name);
    }

    public List<Genre> getGenresByMovie(String movieId) {
        return genreDAO.findByMovie(movieId);
    }

    public List<Genre> getAllGenres() {
        return genreDAO.listGenres();
    }

    public boolean addGenre(Genre genre) {
        return genreDAO.insert(genre);
    }

    public boolean updateGenre(Genre genre) {
        return genreDAO.update(genre);
    }

    public boolean deleteGenre(int id) {
        return genreDAO.delete(id);
    }
}