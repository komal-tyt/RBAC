package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static boolean isEqual(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) == 0;
    }

    public static String addDays(String date, int days) {
        if (date == null || date.isBlank()) return getCurrentDate();

        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            return localDate.plusDays(days).format(DATE_FORMATTER);
        } catch (Exception e) {
            return getCurrentDate();
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null || date.isBlank()) return "unknown date";

        try {
            LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            long daysBetween = ChronoUnit.DAYS.between(targetDate, today);

            if (daysBetween == 0) {
                return "today";
            } else if (daysBetween > 0) {
                if (daysBetween == 1) {
                    return "yesterday";
                } else if (daysBetween < 7) {
                    return daysBetween + " days ago";
                } else if (daysBetween < 30) {
                    long weeks = daysBetween / 7;
                    return weeks + (weeks == 1 ? " week ago" : " weeks ago");
                } else if (daysBetween < 365) {
                    long months = daysBetween / 30;
                    return months + (months == 1 ? " month ago" : " months ago");
                } else {
                    long years = daysBetween / 365;
                    return years + (years == 1 ? " year ago" : " years ago");
                }
            } else {
                long daysInFuture = -daysBetween;
                if (daysInFuture == 1) {
                    return "tomorrow";
                } else if (daysInFuture < 7) {
                    return "in " + daysInFuture + " days";
                } else if (daysInFuture < 30) {
                    long weeks = daysInFuture / 7;
                    return "in " + weeks + (weeks == 1 ? " week" : " weeks");
                } else if (daysInFuture < 365) {
                    long months = daysInFuture / 30;
                    return "in " + months + (months == 1 ? " month" : " months");
                } else {
                    long years = daysInFuture / 365;
                    return "in " + years + (years == 1 ? " year" : " years");
                }
            }
        } catch (Exception e) {
            return "invalid date";
        }
    }

    public static long daysBetween(String date1, String date2) {
        if (date1 == null || date2 == null) return 0;

        try {
            LocalDate d1 = LocalDate.parse(date1, DATE_FORMATTER);
            LocalDate d2 = LocalDate.parse(date2, DATE_FORMATTER);
            return ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isValidDateFormat(String date) {
        if (date == null || date.isBlank()) return false;

        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}