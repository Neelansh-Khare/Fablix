-- FabFlix Database Schema Script (PostgreSQL Compatible)

-- Drop tables in correct order to handle foreign key dependencies
DROP TABLE IF EXISTS stars_in_movies;
DROP TABLE IF EXISTS genres_in_movies;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS ratings;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS creditcards;
DROP TABLE IF EXISTS genres;
DROP TABLE IF EXISTS stars;
DROP TABLE IF EXISTS movies;

-- 1. Movies Table
CREATE TABLE movies (
    id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    director VARCHAR(100) NOT NULL,
    banner_url VARCHAR(200) DEFAULT '',
    trailer_url VARCHAR(200) DEFAULT ''
);

-- 2. Stars Table
CREATE TABLE stars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    birth_year INTEGER,
    photo_url VARCHAR(200) DEFAULT ''
);

-- 3. Genres Table
CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL
);

-- 4. Stars in Movies (Relationship)
CREATE TABLE stars_in_movies (
    star_id VARCHAR(10) NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    FOREIGN KEY (star_id) REFERENCES stars(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

-- 5. Genres in Movies (Relationship)
CREATE TABLE genres_in_movies (
    genre_id INTEGER NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

-- 6. Credit Cards Table (Required for Customer registration)
CREATE TABLE creditcards (
    id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    expiration DATE NOT NULL
);

-- 7. Customers Table
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    cc_id VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    FOREIGN KEY (cc_id) REFERENCES creditcards(id)
);

-- 8. Sales Table
CREATE TABLE sales (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    sale_date DATE NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

-- 9. Ratings Table
CREATE TABLE ratings (
    movie_id VARCHAR(10) NOT NULL,
    rating FLOAT NOT NULL,
    num_votes INTEGER NOT NULL,
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

-- Create indexes for performance (optional but recommended)
CREATE INDEX idx_movie_title ON movies(title);
CREATE INDEX idx_movie_year ON movies(year);
CREATE INDEX idx_star_name ON stars(name);
