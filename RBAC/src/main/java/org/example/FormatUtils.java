package org.example;

import java.util.List;

public class FormatUtils {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        if (rows != null) {
            for (String[] row : rows) {
                if (row != null) {
                    for (int i = 0; i < Math.min(row.length, columnWidths.length); i++) {
                        if (row[i] != null) {
                            columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                        }
                    }
                }
            }
        }

        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] += 2;
        }

        StringBuilder result = new StringBuilder();

        result.append("+");
        for (int width : columnWidths) {
            result.append("-".repeat(width)).append("+");
        }
        result.append("\n");

        result.append("|");
        for (int i = 0; i < headers.length; i++) {
            result.append(padCenter(headers[i], columnWidths[i])).append("|");
        }
        result.append("\n");

        result.append("+");
        for (int width : columnWidths) {
            result.append("-".repeat(width)).append("+");
        }
        result.append("\n");

        if (rows != null) {
            for (String[] row : rows) {
                result.append("|");
                for (int i = 0; i < headers.length; i++) {
                    String value = (i < row.length && row[i] != null) ? row[i] : "";
                    result.append(padRight(value, columnWidths[i])).append("|");
                }
                result.append("\n");
            }
        }

        result.append("+");
        for (int width : columnWidths) {
            result.append("-".repeat(width)).append("+");
        }

        return result.toString();
    }

    public static String formatBox(String text) {
        if (text == null) text = "";
        int width = text.length() + 4;

        StringBuilder box = new StringBuilder();
        box.append("┌").append("─".repeat(width)).append("┐\n");
        box.append("│  ").append(text).append("  │\n");
        box.append("└").append("─".repeat(width)).append("┘");

        return box.toString();
    }

    public static String formatHeader(String text) {
        if (text == null) text = "";
        int width = text.length() + 6;

        StringBuilder header = new StringBuilder();
        header.append("\n").append(ANSI_CYAN).append("═".repeat(width)).append(ANSI_RESET).append("\n");
        header.append(ANSI_CYAN).append("  ").append(text).append("  ").append(ANSI_RESET).append("\n");
        header.append(ANSI_CYAN).append("═".repeat(width)).append(ANSI_RESET);

        return header.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return " ".repeat(length - text.length()) + text;
    }

    public static String padCenter(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        int leftPad = (length - text.length()) / 2;
        int rightPad = length - text.length() - leftPad;
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }

    public static void printTable(String[] headers, List<String[]> rows) {
        System.out.println(formatTable(headers, rows));
    }

    public static void printBox(String text) {
        System.out.println(formatBox(text));
    }

    public static void printHeader(String text) {
        System.out.println(formatHeader(text));
    }
}