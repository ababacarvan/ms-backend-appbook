package com.msbackendappbook.contentservice.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern SEPARATOR = Pattern.compile("[-]+");

    public String generateSlug(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = SEPARATOR.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase().trim();

        // Ajouter un timestamp pour éviter les doublons
        return slug + "-" + System.currentTimeMillis();
    }
}