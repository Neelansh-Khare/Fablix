-- Insert sample genres
INSERT INTO genres (name) VALUES
('Action'),
('Adventure'),
('Animation'),
('Comedy'),
('Crime'),
('Documentary'),
('Drama'),
('Family'),
('Fantasy'),
('Horror'),
('Musical'),
('Mystery'),
('Romance'),
('Sci-Fi'),
('Thriller'),
('Western');

-- Insert sample movies
INSERT INTO movies (id, title, year, director) VALUES
('tt0111161', 'The Shawshank Redemption', 1994, 'Frank Darabont'),
('tt0068646', 'The Godfather', 1972, 'Francis Ford Coppola'),
('tt0071562', 'The Godfather: Part II', 1974, 'Francis Ford Coppola'),
('tt0468569', 'The Dark Knight', 2008, 'Christopher Nolan'),
('tt0050083', '12 Angry Men', 1957, 'Sidney Lumet');

-- Insert sample stars
INSERT INTO stars (id, name, birth_year) VALUES
('nm0000209', 'Morgan Freeman', 1937),
('nm0000151', 'Tim Robbins', 1958),
('nm0000008', 'Marlon Brando', 1924),
('nm0000199', 'Al Pacino', 1940),
('nm0000134', 'Robert De Niro', 1943),
('nm0000288', 'Christian Bale', 1974),
('nm0005132', 'Heath Ledger', 1979),
('nm0000842', 'Henry Fonda', 1905);

-- Connect movies and stars
INSERT INTO stars_in_movies (star_id, movie_id) VALUES
('nm0000209', 'tt0111161'),
('nm0000151', 'tt0111161'),
('nm0000008', 'tt0068646'),
('nm0000199', 'tt0068646'),
('nm0000199', 'tt0071562'),
('nm0000134', 'tt0071562'),
('nm0000288', 'tt0468569'),
('nm0005132', 'tt0468569'),
('nm0000842', 'tt0050083');

-- Connect movies and genres
INSERT INTO genres_in_movies (genre_id, movie_id) VALUES
(7, 'tt0111161'),  -- Drama - Shawshank
(5, 'tt0068646'),  -- Crime - Godfather
(7, 'tt0068646'),  -- Drama - Godfather
(5, 'tt0071562'),  -- Crime - Godfather II
(7, 'tt0071562'),  -- Drama - Godfather II
(1, 'tt0468569'),  -- Action - Dark Knight
(5, 'tt0468569'),  -- Crime - Dark Knight
(7, 'tt0050083');  -- Drama - 12 Angry Men