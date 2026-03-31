-- Stored Procedure: add_star
-- Purpose: Add a new star with automatic ID generation.

CREATE OR REPLACE PROCEDURE add_star(
    p_name VARCHAR(100),
    p_birth_year INTEGER,
    INOUT p_message TEXT DEFAULT ''
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_star_id VARCHAR(10);
    v_max_star_id INTEGER;
BEGIN
    -- Generate new Star ID (format: nmXXXXXXX)
    SELECT COALESCE(MAX(CAST(SUBSTRING(id, 3) AS INTEGER)), 0) + 1 INTO v_max_star_id FROM stars;
    v_star_id := 'nm' || LPAD(v_max_star_id::text, 7, '0');

    -- Insert Star
    INSERT INTO stars (id, name, birth_year) 
    VALUES (v_star_id, p_name, p_birth_year);

    p_message := 'Success! Star ID: ' || v_star_id;

EXCEPTION
    WHEN OTHERS THEN
        p_message := 'Error: ' || SQLERRM;
        ROLLBACK;
END;
$$;
