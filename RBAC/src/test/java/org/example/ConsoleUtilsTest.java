package org.example;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    private Scanner createMockScanner(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void testPromptStringRequired() {
        Scanner scanner = createMockScanner("test input\n");
        String result = ConsoleUtils.promptString(scanner, "Enter text: ", true);
        assertEquals("test input", result);
    }

    @Test
    void testPromptStringRequiredEmptyThenValid() {
        Scanner scanner = createMockScanner("\ntest input\n");
        String result = ConsoleUtils.promptString(scanner, "Enter text: ", true);
        assertEquals("test input", result);
    }

    @Test
    void testPromptStringNotRequired() {
        Scanner scanner = createMockScanner("\n");
        String result = ConsoleUtils.promptString(scanner, "Enter text: ", false);
        assertNull(result);
    }

    @Test
    void testPromptIntValid() {
        Scanner scanner = createMockScanner("5\n");
        int result = ConsoleUtils.promptInt(scanner, "Enter number", 1, 10);
        assertEquals(5, result);
    }

    @Test
    void testPromptIntInvalidThenValid() {
        Scanner scanner = createMockScanner("20\n5\n");
        int result = ConsoleUtils.promptInt(scanner, "Enter number", 1, 10);
        assertEquals(5, result);
    }

    @Test
    void testPromptIntNonNumberThenValid() {
        Scanner scanner = createMockScanner("abc\n5\n");
        int result = ConsoleUtils.promptInt(scanner, "Enter number", 1, 10);
        assertEquals(5, result);
    }

    @Test
    void testPromptYesNoYes() {
        Scanner scanner = createMockScanner("y\n");
        boolean result = ConsoleUtils.promptYesNo(scanner, "Confirm?");
        assertTrue(result);
    }

    @Test
    void testPromptYesNoYesFull() {
        Scanner scanner = createMockScanner("yes\n");
        boolean result = ConsoleUtils.promptYesNo(scanner, "Confirm?");
        assertTrue(result);
    }

    @Test
    void testPromptYesNoNo() {
        Scanner scanner = createMockScanner("n\n");
        boolean result = ConsoleUtils.promptYesNo(scanner, "Confirm?");
        assertFalse(result);
    }

    @Test
    void testPromptYesNoInvalidThenYes() {
        Scanner scanner = createMockScanner("invalid\ny\n");
        boolean result = ConsoleUtils.promptYesNo(scanner, "Confirm?");
        assertTrue(result);
    }

    @Test
    void testPromptChoice() {
        List<String> options = List.of("Option A", "Option B", "Option C");
        Scanner scanner = createMockScanner("2\n");

        String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
        assertEquals("Option B", result);
    }

    @Test
    void testPromptChoiceInvalidThenValid() {
        List<String> options = List.of("Option A", "Option B");
        Scanner scanner = createMockScanner("5\n1\n");

        String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
        assertEquals("Option A", result);
    }

    @Test
    void testPromptChoiceEmptyOptions() {
        List<String> options = List.of();
        Scanner scanner = createMockScanner("1\n");

        String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
        assertNull(result);
    }

    @Test
    void testPrintHeader() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ConsoleUtils.printHeader("TEST HEADER");
            String output = outputStream.toString();
            assertTrue(output.contains("TEST HEADER"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintSuccess() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ConsoleUtils.printSuccess("Success message");
            String output = outputStream.toString();
            assertTrue(output.contains("Success message"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintError() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ConsoleUtils.printError("Error message");
            String output = outputStream.toString();
            assertTrue(output.contains("Error message"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintInfo() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ConsoleUtils.printInfo("Info message");
            String output = outputStream.toString();
            assertTrue(output.contains("Info message"));
        } finally {
            System.setOut(originalOut);
        }
    }
}