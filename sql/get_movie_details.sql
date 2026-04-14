-- Stored Function: get_movie_details
-- Purpose: Fetch comprehensive movie details (including stars and genres) as a JSONB object.
-- This reduces the number of joins and data transfer overhead in the application layer.

CREATE OR REPLACE FUNCTION get_movie_details(p_movie_id VARCHAR(10))
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_movie_json JSONB;
BEGIN
    SELECT jsonb_build_object(
        'id', m.id,
        'title', m.title,
        'year', m.year,
        'director', m.director,
        'bannerUrl', m.banner_url,
        'trailerUrl', m.trailer_url,
        'genres', (
            SELECT jsonb_agg(jsonb_build_object('id', g.id, 'name', g.name))
            FROM genres g
            JOIN genres_in_movies gim ON g.id = gim.genre_id
            WHERE gim.movie_id = m.id
        ),
        'stars', (
            SELECT jsonb_agg(jsonb_build_object(
                'id', s.id, 
                'name', s.name, 
                'birthYear', s.birth_year,
                'photoUrl', s.photo_url
            ))
            FROM stars s
            JOIN stars_in_movies sim ON s.id = sim.star_id
            WHERE sim.movie_id = m.id
        )
    ) INTO v_movie_json
    FROM movies m
    WHERE m.id = p_movie_id;

    RETURN v_movie_json;
END;
$$;
