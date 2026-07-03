package com.floristeria.floristeria.util;

public class StringUtil {

    public static String capitalize(String str) {
        if (str == null || str.isBlank()) {
            return str;
        }
        String trimmed = str.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }
}
