package com.neelanshkhare.fabflix.dao;

import com.neelanshkhare.fabflix.model.Movie;
import java.util.List;

public interface MovieDAO {
    Movie findById(String id);
    List<Movie> findByTitle(String title);
    List<Movie> findByDirector(String director);
    List<Movie> findByYear(int year);
    List<Movie> findByGenre(int genreId);
    List<Movie> findByStar(String starId);
    List<Movie> searchMovies(String query);
    List<Movie> listMovies(int page, int pageSize);
    List<Movie> getMoviesWithoutPosters(int limit);
    int countMovies();
    boolean insert(Movie movie);
    boolean update(Movie movie);
    boolean delete(String id);
}