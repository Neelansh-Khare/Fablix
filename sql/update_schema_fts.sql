-- FabFlix PostgreSQL Full-Text Search Optimization
-- This script adds a GIN index to the movies table for faster searching across title and director.

-- 1. Create the GIN index using a combined vector of title and director
CREATE INDEX IF NOT EXISTS idx_movies_fts ON movies USING gin(to_tsvector('english', title || ' ' || director));

-- 2. Optional: Add separate indexes if individual column search needs optimization beyond simple B-tree
-- (Already handled by idx_movie_title and idx_movie_year in createtable.sql)

-- 3. Verify the index
-- SELECT * FROM pg_indexes WHERE tablename = 'movies';
