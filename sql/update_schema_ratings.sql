-- FabFlix PostgreSQL Ratings Integration
-- This script updates stored functions to include movie ratings.

-- 1. Update get_movie_details to include rating and num_votes
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
        'rating', COALESCE(r.rating, 0.0),
        'numVotes', COALESCE(r.num_votes, 0),
        'genres', (
            SELECT jsonb_agg(jsonb_build_object('id', g.id, 'name', g.name))
            FROM (
                SELECT g.id, g.name
                FROM genres g
                JOIN genres_in_movies gim ON g.id = gim.genre_id
                WHERE gim.movie_id = m.id
                ORDER BY g.name ASC
            ) g
        ),
        'stars', (
            SELECT jsonb_agg(jsonb_build_object(
                'id', s.id, 
                'name', s.name, 
                'birthYear', s.birth_year,
                'photoUrl', s.photo_url
            ))
            FROM (
                SELECT s.id, s.name, s.birth_year, s.photo_url
                FROM stars s
                JOIN stars_in_movies sim ON s.id = sim.star_id
                WHERE sim.movie_id = m.id
                ORDER BY s.name ASC
            ) s
        )
    ) INTO v_movie_json
    FROM movies m
    LEFT JOIN ratings r ON m.id = r.movie_id
    WHERE m.id = p_movie_id;

    RETURN v_movie_json;
END;
$$;

-- 2. Update search_movies_optimized to include rating and num_votes, and support sorting by rating
CREATE OR REPLACE FUNCTION search_movies_optimized(
    p_query VARCHAR DEFAULT NULL,
    p_title VARCHAR DEFAULT NULL,
    p_year INTEGER DEFAULT NULL,
    p_director VARCHAR DEFAULT NULL,
    p_star_name VARCHAR DEFAULT NULL,
    p_genre_id INTEGER DEFAULT NULL,
    p_first_letter CHAR(1) DEFAULT NULL,
    p_sort_by VARCHAR DEFAULT 'title',
    p_sort_order VARCHAR DEFAULT 'ASC',
    p_limit INTEGER DEFAULT 10,
    p_offset INTEGER DEFAULT 0,
    p_star_id VARCHAR DEFAULT NULL
)
RETURNS SETOF JSONB
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    WITH filtered_movies AS (
        SELECT m.id, m.title, m.year, m.director, m.banner_url, m.trailer_url, 
               COALESCE(r.rating, 0.0) as rating, COALESCE(r.num_votes, 0) as num_votes
        FROM movies m
        LEFT JOIN ratings r ON m.id = r.movie_id
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
            -- Filter by Star ID
            AND (p_star_id IS NULL OR EXISTS (
                SELECT 1 FROM stars_in_movies sim 
                WHERE sim.movie_id = m.id AND sim.star_id = p_star_id
            ))
            -- Filter by Genre ID
            AND (p_genre_id IS NULL OR EXISTS (
                SELECT 1 FROM genres_in_movies gim 
                WHERE gim.movie_id = m.id AND gim.genre_id = p_genre_id
            ))
        ORDER BY
            CASE WHEN p_sort_by = 'title' AND p_sort_order = 'ASC' THEN m.title END ASC,
            CASE WHEN p_sort_by = 'title' AND p_sort_order = 'DESC' THEN m.title END DESC,
            CASE WHEN p_sort_by = 'year' AND p_sort_order = 'ASC' THEN m.year END ASC,
            CASE WHEN p_sort_by = 'year' AND p_sort_order = 'DESC' THEN m.year END DESC,
            CASE WHEN p_sort_by = 'rating' AND p_sort_order = 'ASC' THEN COALESCE(r.rating, 0.0) END ASC,
            CASE WHEN p_sort_by = 'rating' AND p_sort_order = 'DESC' THEN COALESCE(r.rating, 0.0) END DESC,
            -- Fallback sort
            m.id ASC
        LIMIT p_limit
        OFFSET p_offset
    )
    SELECT jsonb_build_object(
        'id', fm.id,
        'title', fm.title,
        'year', fm.year,
        'director', fm.director,
        'bannerUrl', fm.banner_url,
        'trailerUrl', fm.trailer_url,
        'rating', fm.rating,
        'numVotes', fm.num_votes,
        'genres', (
            SELECT jsonb_agg(jsonb_build_object('id', g.id, 'name', g.name))
            FROM (
                SELECT g.id, g.name
                FROM genres g
                JOIN genres_in_movies gim ON g.id = gim.genre_id
                WHERE gim.movie_id = fm.id
                ORDER BY g.name ASC
                LIMIT 3
            ) g
        ),
        'stars', (
            SELECT jsonb_agg(jsonb_build_object('id', s.id, 'name', s.name))
            FROM (
                SELECT s.id, s.name
                FROM stars s
                JOIN stars_in_movies sim ON s.id = sim.star_id
                WHERE sim.movie_id = fm.id
                ORDER BY s.name ASC 
                LIMIT 3
            ) s
        )
    )
    FROM filtered_movies fm;
END;
$$;
