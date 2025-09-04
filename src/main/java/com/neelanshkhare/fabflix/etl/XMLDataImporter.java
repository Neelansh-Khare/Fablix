package com.neelanshkhare.fabflix.etl;

import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import com.neelanshkhare.fabflix.service.GenreService;
import com.neelanshkhare.fabflix.service.MovieService;
import com.neelanshkhare.fabflix.service.StarService;
import com.neelanshkhare.fabflix.util.XMLParserUtil;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLDataImporter {
    private MovieService movieService;
    private StarService starService;
    private GenreService genreService;

    public XMLDataImporter() {
        this.movieService = new MovieService();
        this.starService = new StarService();
        this.genreService = new GenreService();
    }

    public void importMoviesFromXML(File moviesXmlFile) {
        try {
            List<Movie> movies = XMLParserUtil.parseMoviesXML(moviesXmlFile);

            // Process genres first to get IDs
            Map<String, Integer> genreNameToIdMap = new HashMap<>();
            List<Genre> existingGenres = genreService.getAllGenres();

            for (Genre genre : existingGenres) {
                genreNameToIdMap.put(genre.getName(), genre.getId());
            }

            // Process movies
            for (Movie movie : movies) {
                // Set genre IDs based on names
                for (Genre genre : movie.getGenres()) {
                    Integer genreId = genreNameToIdMap.get(genre.getName());

                    if (genreId == null) {
                        // New genre, add to database
                        genreService.addGenre(genre);
                        genreNameToIdMap.put(genre.getName(), genre.getId());
                    } else {
                        genre.setId(genreId);
                    }
                }

                // Add movie to database
                movieService.addMovie(movie);
            }

            System.out.println("Successfully imported " + movies.size() + " movies.");

        } catch (DocumentException e) {
            System.err.println("Error parsing XML file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void importStarsFromXML(File starsXmlFile) {
        try {
            List<Star> stars = XMLParserUtil.parseStarsXML(starsXmlFile);

            // Add stars to database
            int successCount = 0;
            for (Star star : stars) {
                if (starService.addStar(star)) {
                    successCount++;
                }
            }

            System.out.println("Successfully imported " + successCount + " out of " + stars.size() + " stars.");

        } catch (DocumentException e) {
            System.err.println("Error parsing XML file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java XMLDataImporter <movies_xml_file> <stars_xml_file>");
            return;
        }

        File moviesFile = new File(args[0]);
        File starsFile = new File(args[1]);

        if (!moviesFile.exists() || !starsFile.exists()) {
            System.out.println("One or both input files do not exist.");
            return;
        }

        XMLDataImporter importer = new XMLDataImporter();
        importer.importStarsFromXML(starsFile);
        importer.importMoviesFromXML(moviesFile);
    }
}