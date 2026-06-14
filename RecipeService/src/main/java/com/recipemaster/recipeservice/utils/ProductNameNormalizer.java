package com.recipemaster.recipeservice.utils;

import java.util.Locale;

public final class ProductNameNormalizer {

    private ProductNameNormalizer() {
    }

    public static String normalizeDisplayName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        String collapsed = name.trim().replaceAll("\\s+", " ");
        String lowerCased = collapsed.toLowerCase(Locale.ROOT);

        return Character.toUpperCase(lowerCased.charAt(0)) + lowerCased.substring(1);
    }
}
