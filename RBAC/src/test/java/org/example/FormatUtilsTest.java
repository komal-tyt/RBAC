package org.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void testFormatTable() {
        String[] headers = {"NAME", "AGE", "CITY"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"John", "25", "New York"});
        rows.add(new String[]{"Anna", "30", "London"});

        String table = FormatUtils.formatTable(headers, rows);

        assertTrue(table.contains("NAME"));
        assertTrue(table.contains("AGE"));
        assertTrue(table.contains("CITY"));
        assertTrue(table.contains("John"));
        assertTrue(table.contains("Anna"));
        assertTrue(table.contains("+"));
    }

    @Test
    void testFormatTableEmptyHeaders() {
        String table = FormatUtils.formatTable(null, null);
        assertEquals("", table);
    }

    @Test
    void testFormatTableEmptyRows() {
        String[] headers = {"NAME", "AGE"};
        String table = FormatUtils.formatTable(headers, null);

        assertTrue(table.contains("NAME"));
        assertTrue(table.contains("AGE"));
        assertTrue(table.contains("+"));
    }

    @Test
    void testFormatBox() {
        String box = FormatUtils.formatBox("Hello");

        assertTrue(box.contains("┌"));
        assertTrue(box.contains("┐"));
        assertTrue(box.contains("│"));
        assertTrue(box.contains("└"));
        assertTrue(box.contains("┘"));
        assertTrue(box.contains("Hello"));
    }

    @Test
    void testFormatBoxNull() {
        String box = FormatUtils.formatBox(null);
        assertTrue(box.contains("┌"));
        assertTrue(box.contains("┐"));
    }

    @Test
    void testFormatHeader() {
        String header = FormatUtils.formatHeader("Test Header");

        assertTrue(header.contains("Test Header"));
        assertTrue(header.contains("═"));
    }

    @Test
    void testFormatHeaderNull() {
        String header = FormatUtils.formatHeader(null);
        assertTrue(header.contains("═"));
    }

    @Test
    void testTruncate() {
        assertEquals("Hello", FormatUtils.truncate("Hello", 10));
        assertEquals("Hel...", FormatUtils.truncate("Hello World", 6));
        assertEquals("", FormatUtils.truncate(null, 5));
        assertEquals("abc", FormatUtils.truncate("abcdef", 3));
    }

    @Test
    void testPadRight() {
        assertEquals("Hello   ", FormatUtils.padRight("Hello", 8));
        assertEquals("Hello", FormatUtils.padRight("Hello", 3));
        assertEquals("", FormatUtils.padRight(null, 5));
    }

    @Test
    void testPadLeft() {
        assertEquals("   Hello", FormatUtils.padLeft("Hello", 8));
        assertEquals("Hello", FormatUtils.padLeft("Hello", 3));
        assertEquals("", FormatUtils.padLeft(null, 5));
    }

    @Test
    void testPadCenter() {
        assertEquals(" Hello ", FormatUtils.padCenter("Hello", 7));
        assertEquals("Hello", FormatUtils.padCenter("Hello", 3));
        assertEquals("", FormatUtils.padCenter(null, 5));
    }

    @Test
    void testPrintTable() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            String[] headers = {"TEST"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Value"});
            FormatUtils.printTable(headers, rows);

            assertTrue(outputStream.toString().contains("TEST"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintBox() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            FormatUtils.printBox("Test");
            assertTrue(outputStream.toString().contains("Test"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintHeader() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            FormatUtils.printHeader("Header");
            assertTrue(outputStream.toString().contains("Header"));
        } finally {
            System.setOut(originalOut);
        }
    }
}