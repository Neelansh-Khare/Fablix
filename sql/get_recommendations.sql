-- PostgreSQL Stored Functions for Movie Recommendations

-- 1. Content-Based Filtering: Get similar movies based on matching genres
CREATE OR REPLACE FUNCTION get_similar_movies(p_movie_id VARCHAR(10), p_limit INTEGER DEFAULT 5)
RETURNS TABLE (
    id VARCHAR(10),
    title VARCHAR(100),
    year INTEGER,
    director VARCHAR(100),
    banner_url VARCHAR(200),
    matching_genres_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH target_genres AS (
        SELECT genre_id FROM genres_in_movies WHERE movie_id = p_movie_id
    )
    SELECT m.id, m.title, m.year, m.director, m.banner_url, COUNT(gim.genre_id) as matching_genres_count
    FROM movies m
    JOIN genres_in_movies gim ON m.id = gim.movie_id
    WHERE gim.genre_id IN (SELECT genre_id FROM target_genres)
      AND m.id <> p_movie_id
    GROUP BY m.id, m.title, m.year, m.director, m.banner_url
    ORDER BY matching_genres_count DESC, m.title ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- 2. Basic Collaborative Filtering: Get co-purchase recommendations ("Users who bought this also bought...")
CREATE OR REPLACE FUNCTION get_co_purchase_recommendations(p_movie_id VARCHAR(10), p_limit INTEGER DEFAULT 5)
RETURNS TABLE (
    id VARCHAR(10),
    title VARCHAR(100),
    year INTEGER,
    director VARCHAR(100),
    banner_url VARCHAR(200),
    co_purchase_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH buyers AS (
        SELECT customer_id FROM sales WHERE movie_id = p_movie_id
    )
    SELECT m.id, m.title, m.year, m.director, m.banner_url, COUNT(DISTINCT s.customer_id) as co_purchase_count
    FROM movies m
    JOIN sales s ON m.id = s.movie_id
    WHERE s.customer_id IN (SELECT customer_id FROM buyers)
      AND m.id <> p_movie_id
    GROUP BY m.id, m.title, m.year, m.director, m.banner_url
    ORDER BY co_purchase_count DESC, m.title ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;
