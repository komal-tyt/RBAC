package org.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtils {

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String normalized = normalizeString(username);
        return normalized.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = normalizeString(email);
        return normalized.matches("^.+@.+\\..+$");
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }


}
