package com.neelanshkhare.fabflix.util;

import com.neelanshkhare.fabflix.model.Genre;
import com.neelanshkhare.fabflix.model.Movie;
import com.neelanshkhare.fabflix.model.Star;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLParserUtil {

    // Parse movies XML file
    public static List<Movie> parseMoviesXML(File xmlFile) throws DocumentException {
        List<Movie> movies = new ArrayList<>();
        Map<String, Genre> genresMap = new HashMap<>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlFile);
        Element root = document.getRootElement();

        for (Element movieElement : root.elements("movie")) {
            Movie movie = new Movie();

            // Parse basic movie info
            String id = movieElement.elementText("id");
            String title = movieElement.elementText("title");
            String yearStr = movieElement.elementText("year");
            String director = movieElement.elementText("director");

            movie.setId(id);
            movie.setTitle(title);

            // Parse year (handle parsing errors)
            try {
                movie.setYear(Integer.parseInt(yearStr));
            } catch (NumberFormatException e) {
                System.err.println("Error parsing year for movie: " + title);
                movie.setYear(0); // Default value
            }

            movie.setDirector(director);

            // Parse genres
            Element genresElement = movieElement.element("genres");
            if (genresElement != null) {
                for (Element genreElement : genresElement.elements("genre")) {
                    String genreName = genreElement.getTextTrim();

                    // Reuse existing genre object if we've seen this genre before
                    Genre genre = genresMap.get(genreName);
                    if (genre == null) {
                        genre = new Genre();
                        genre.setName(genreName);
                        genresMap.put(genreName, genre);
                    }

                    movie.addGenre(genre);
                }
            }

            // Parse actors/stars
            Element starsElement = movieElement.element("stars");
            if (starsElement != null) {
                for (Element starElement : starsElement.elements("star")) {
                    String starId = starElement.attributeValue("id");
                    String starName = starElement.getTextTrim();

                    Star star = new Star(starId, starName);
                    movie.addStar(star);
                }
            }

            movies.add(movie);
        }

        return movies;
    }

    // Parse stars XML file
    public static List<Star> parseStarsXML(File xmlFile) throws DocumentException {
        List<Star> stars = new ArrayList<>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlFile);
        Element root = document.getRootElement();

        for (Element starElement : root.elements("star")) {
            Star star = new Star();

            // Parse basic star info
            String id = starElement.elementText("id");
            String name = starElement.elementText("name");
            String birthYearStr = starElement.elementText("birthYear");

            star.setId(id);
            star.setName(name);

            // Parse birth year (handle parsing errors and empty values)
            if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
                try {
                    star.setBirthYear(Integer.parseInt(birthYearStr));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing birth year for star: " + name);
                }
            }

            stars.add(star);
        }

        return stars;
    }
}