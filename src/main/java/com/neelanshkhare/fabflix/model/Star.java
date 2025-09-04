package com.neelanshkhare.fabflix.model;

import java.util.ArrayList;
import java.util.List;

public class Star {
    private String id;
    private String name;
    private Integer birthYear;
    private String photoUrl;
    private List<Movie> movies;

    public Star() {
        this.movies = new ArrayList<>();
    }

    public Star(String id, String name) {
        this.id = id;
        this.name = name;
        this.movies = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<Movie> getMovies() { return movies; }
    public void setMovies(List<Movie> movies) { this.movies = movies; }
    public void addMovie(Movie movie) { this.movies.add(movie); }
}