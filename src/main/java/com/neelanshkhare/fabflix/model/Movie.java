package com.neelanshkhare.fabflix.model;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    private String id;
    private String title;
    private int year;
    private String director;
    private String bannerUrl;
    private String trailerUrl;
    private List<Star> stars;
    private List<Genre> genres;

    public Movie() {
        this.stars = new ArrayList<>();
        this.genres = new ArrayList<>();
    }

    public Movie(String id, String title, int year, String director) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        this.stars = new ArrayList<>();
        this.genres = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public List<Star> getStars() { return stars; }
    public void setStars(List<Star> stars) { this.stars = stars; }
    public void addStar(Star star) { this.stars.add(star); }

    public List<Genre> getGenres() { return genres; }
    public void setGenres(List<Genre> genres) { this.genres = genres; }
    public void addGenre(Genre genre) { this.genres.add(genre); }
}