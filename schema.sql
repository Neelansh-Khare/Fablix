CREATE TABLE movies (
    id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    director VARCHAR(100) NOT NULL,
    banner_url VARCHAR(200),
    trailer_url VARCHAR(200)
);

CREATE TABLE stars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    birth_year INTEGER,
    photo_url VARCHAR(200)
);

CREATE TABLE genres (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL
);

CREATE TABLE customers (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    cc_id VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(64) NOT NULL,
    salt VARCHAR(64) NOT NULL
);

CREATE TABLE stars_in_movies (
    star_id VARCHAR(10) NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (star_id, movie_id),
    FOREIGN KEY (star_id) REFERENCES stars(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE TABLE genres_in_movies (
    genre_id INTEGER NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (genre_id, movie_id),
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);