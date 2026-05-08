package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testGetCurrentDate() {
        String date = DateUtils.getCurrentDate();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void testGetCurrentDateTime() {
        String datetime = DateUtils.getCurrentDateTime();
        assertTrue(datetime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testIsBefore() {
        assertTrue(DateUtils.isBefore("2024-01-01", "2024-01-15"));
        assertFalse(DateUtils.isBefore("2024-01-15", "2024-01-01"));
        assertFalse(DateUtils.isBefore(null, "2024-01-01"));
        assertFalse(DateUtils.isBefore("2024-01-01", null));
    }

    @Test
    void testIsAfter() {
        assertTrue(DateUtils.isAfter("2024-01-15", "2024-01-01"));
        assertFalse(DateUtils.isAfter("2024-01-01", "2024-01-15"));
        assertFalse(DateUtils.isAfter(null, "2024-01-01"));
        assertFalse(DateUtils.isAfter("2024-01-01", null));
    }

    @Test
    void testIsEqual() {
        assertTrue(DateUtils.isEqual("2024-01-01", "2024-01-01"));
        assertFalse(DateUtils.isEqual("2024-01-01", "2024-01-02"));
        assertFalse(DateUtils.isEqual(null, "2024-01-01"));
    }

    @Test
    void testAddDays() {
        assertEquals("2024-01-06", DateUtils.addDays("2024-01-01", 5));
        assertEquals("2023-12-27", DateUtils.addDays("2024-01-01", -5));
        assertNotNull(DateUtils.addDays(null, 5));
        assertNotNull(DateUtils.addDays("invalid", 5));
    }

    @Test
    void testFormatRelativeTimeToday() {
        String today = DateUtils.getCurrentDate();
        assertEquals("today", DateUtils.formatRelativeTime(today));
    }

    @Test
    void testFormatRelativeTimeYesterday() {
        String yesterday = DateUtils.addDays(DateUtils.getCurrentDate(), -1);
        assertEquals("yesterday", DateUtils.formatRelativeTime(yesterday));
    }

    @Test
    void testFormatRelativeTomorrow() {
        String tomorrow = DateUtils.addDays(DateUtils.getCurrentDate(), 1);
        assertEquals("tomorrow", DateUtils.formatRelativeTime(tomorrow));
    }

    @Test
    void testFormatRelativeDaysAgo() {
        String date = DateUtils.addDays(DateUtils.getCurrentDate(), -5);
        assertEquals("5 days ago", DateUtils.formatRelativeTime(date));
    }

    @Test
    void testFormatRelativeInDays() {
        String date = DateUtils.addDays(DateUtils.getCurrentDate(), 5);
        assertEquals("in 5 days", DateUtils.formatRelativeTime(date));
    }

    @Test
    void testFormatRelativeWeeksAgo() {
        String date = DateUtils.addDays(DateUtils.getCurrentDate(), -14);
        assertTrue(DateUtils.formatRelativeTime(date).contains("weeks ago"));
    }

    @Test
    void testFormatRelativeNull() {
        assertEquals("unknown date", DateUtils.formatRelativeTime(null));
        assertEquals("invalid date", DateUtils.formatRelativeTime("invalid"));
    }

    @Test
    void testDaysBetween() {
        long days = DateUtils.daysBetween("2024-01-01", "2024-01-15");
        assertEquals(14, days);

        assertEquals(0, DateUtils.daysBetween(null, "2024-01-15"));
        assertEquals(0, DateUtils.daysBetween("invalid", "2024-01-15"));
    }

    @Test
    void testIsValidDateFormat() {
        assertTrue(DateUtils.isValidDateFormat("2024-01-01"));
        assertTrue(DateUtils.isValidDateFormat("2024-12-31"));

        assertFalse(DateUtils.isValidDateFormat("2024-13-01"));
        assertFalse(DateUtils.isValidDateFormat("2024-01-32"));
        assertFalse(DateUtils.isValidDateFormat("01-01-2024"));
        assertFalse(DateUtils.isValidDateFormat(null));
        assertFalse(DateUtils.isValidDateFormat(""));
    }
}