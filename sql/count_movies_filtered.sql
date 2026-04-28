-- Stored Function: count_movies_filtered
-- Purpose: Count the total number of movies matching the given filters.
-- This is used for pagination in search results.

CREATE OR REPLACE FUNCTION count_movies_filtered(
    p_query VARCHAR DEFAULT NULL,
    p_title VARCHAR DEFAULT NULL,
    p_year INTEGER DEFAULT NULL,
    p_director VARCHAR DEFAULT NULL,
    p_star_name VARCHAR DEFAULT NULL,
    p_genre_id INTEGER DEFAULT NULL,
    p_first_letter CHAR(1) DEFAULT NULL
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM movies m
    WHERE 
        -- Full-Text Search
        (p_query IS NULL OR to_tsvector('english', m.title || ' ' || m.director) @@ plainto_tsquery('english', p_query))
        -- Individual Column Filters
        AND (p_title IS NULL OR m.title ILIKE '%' || p_title || '%')
        AND (p_year IS NULL OR m.year = p_year)
        AND (p_director IS NULL OR m.director ILIKE '%' || p_director || '%')
        -- First Letter Filter
        AND (p_first_letter IS NULL OR (
            CASE 
                WHEN p_first_letter = '*' THEN m.title ~ '^[^a-zA-Z0-9]'
                ELSE m.title ILIKE p_first_letter || '%'
            END
        ))
        -- Filter by Star Name
        AND (p_star_name IS NULL OR EXISTS (
            SELECT 1 FROM stars_in_movies sim 
            JOIN stars s ON sim.star_id = s.id 
            WHERE sim.movie_id = m.id AND s.name ILIKE '%' || p_star_name || '%'
        ))
        -- Filter by Genre ID
        AND (p_genre_id IS NULL OR EXISTS (
            SELECT 1 FROM genres_in_movies gim 
            WHERE gim.movie_id = m.id AND gim.genre_id = p_genre_id
        ));

    RETURN v_count;
END;
$$;
