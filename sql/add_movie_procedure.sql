-- Stored Procedure: add_movie
-- Purpose: Add a new movie with its associated star and genre.
-- Handles ID generation and existing record checks.

CREATE OR REPLACE PROCEDURE add_movie(
    p_title VARCHAR(100),
    p_year INTEGER,
    p_director VARCHAR(100),
    p_star_name VARCHAR(100),
    p_genre_name VARCHAR(32),
    INOUT p_message TEXT DEFAULT ''
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_movie_id VARCHAR(10);
    v_star_id VARCHAR(10);
    v_genre_id INTEGER;
    v_movie_exists INTEGER;
    v_max_movie_id INTEGER;
    v_max_star_id INTEGER;
BEGIN
    -- 1. Check if movie already exists (same title, year, director)
    SELECT COUNT(*) INTO v_movie_exists 
    FROM movies 
    WHERE title = p_title AND year = p_year AND director = p_director;

    IF v_movie_exists > 0 THEN
        p_message := 'Movie already exists.';
        RETURN;
    END IF;

    -- 2. Generate new Movie ID (format: ttXXXXXXX)
    SELECT COALESCE(MAX(CAST(SUBSTRING(id, 3) AS INTEGER)), 0) + 1 INTO v_max_movie_id FROM movies;
    v_movie_id := 'tt' || LPAD(v_max_movie_id::text, 7, '0');

    -- 3. Find or Create Star
    SELECT id INTO v_star_id FROM stars WHERE name = p_star_name LIMIT 1;
    IF v_star_id IS NULL THEN
        SELECT COALESCE(MAX(CAST(SUBSTRING(id, 3) AS INTEGER)), 0) + 1 INTO v_max_star_id FROM stars;
        v_star_id := 'nm' || LPAD(v_max_star_id::text, 7, '0');
        INSERT INTO stars (id, name) VALUES (v_star_id, p_star_name);
    END IF;

    -- 4. Find or Create Genre
    SELECT id INTO v_genre_id FROM genres WHERE name = p_genre_name LIMIT 1;
    IF v_genre_id IS NULL THEN
        INSERT INTO genres (name) VALUES (p_genre_name) RETURNING id INTO v_genre_id;
    END IF;

    -- 5. Insert Movie
    INSERT INTO movies (id, title, year, director) 
    VALUES (v_movie_id, p_title, p_year, p_director);

    -- 6. Link Star and Genre
    INSERT INTO stars_in_movies (star_id, movie_id) VALUES (v_star_id, v_movie_id);
    INSERT INTO genres_in_movies (genre_id, movie_id) VALUES (v_genre_id, v_movie_id);

    p_message := 'Success! Movie ID: ' || v_movie_id || ', Star ID: ' || v_star_id || ', Genre ID: ' || v_genre_id;

EXCEPTION
    WHEN OTHERS THEN
        p_message := 'Error: ' || SQLERRM;
        ROLLBACK;
END;
$$;
