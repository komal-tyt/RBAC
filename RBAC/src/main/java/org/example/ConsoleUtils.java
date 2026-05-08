package org.example;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(ANSI_CYAN + message + ANSI_RESET);
            String input = scanner.nextLine().trim();

            if (required && input.isEmpty()) {
                System.out.println(ANSI_RED + "Error: This field is required!" + ANSI_RESET);
                continue;
            }

            if (!required && input.isEmpty()) {
                return null;
            }

            return input;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(ANSI_CYAN + message + " (" + min + "-" + max + "): " + ANSI_RESET);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(ANSI_RED + "Error: Number must be between " + min + " and " + max + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Error: Please enter a valid number!" + ANSI_RESET);
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(ANSI_CYAN + message + " (y/n): " + ANSI_RESET);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y") || input.equals("yes") || input.equals("да")) {
                return true;
            } else if (input.equals("n") || input.equals("no") || input.equals("нет")) {
                return false;
            } else {
                System.out.println(ANSI_RED + "Error: Please enter 'y' or 'n'" + ANSI_RESET);
            }
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            System.out.println(ANSI_RED + "Error: No options available!" + ANSI_RESET);
            return null;
        }

        while (true) {
            System.out.println("\n" + ANSI_YELLOW + message + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "-".repeat(40) + ANSI_RESET);

            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d. %s\n", i + 1, options.get(i).toString());
            }
            System.out.println(ANSI_YELLOW + "-".repeat(40) + ANSI_RESET);

            System.out.print(ANSI_CYAN + "Enter your choice (1-" + options.size() + "): " + ANSI_RESET);
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                } else {
                    System.out.println(ANSI_RED + "Error: Please enter a number between 1 and " + options.size() + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Error: Please enter a valid number!" + ANSI_RESET);
            }
        }
    }

    public static void printHeader(String title) {
        System.out.println("\n" + ANSI_GREEN + "=".repeat(60) + ANSI_RESET);
        System.out.println(ANSI_GREEN + "  " + title + ANSI_RESET);
        System.out.println(ANSI_GREEN + "=".repeat(60) + ANSI_RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }

    public static void printError(String message) {
        System.out.println(ANSI_RED + "✗ " + message + ANSI_RESET);
    }

    public static void printInfo(String message) {
        System.out.println(ANSI_CYAN + "ℹ " + message + ANSI_RESET);
    }
}