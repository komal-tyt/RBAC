package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void testIsValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("john123"));
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertTrue(ValidationUtils.isValidUsername("admin"));

        assertFalse(ValidationUtils.isValidUsername("jo"));
        assertFalse(ValidationUtils.isValidUsername("abcdefghijklmnopqrstuvwxyz123"));
        assertFalse(ValidationUtils.isValidUsername("john doe"));
        assertFalse(ValidationUtils.isValidUsername("john@doe"));
        assertFalse(ValidationUtils.isValidUsername(null));
    }

    @Test
    void testIsValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("john@example.com"));
        assertTrue(ValidationUtils.isValidEmail("john.doe@example.co.uk"));

        assertFalse(ValidationUtils.isValidEmail("john@example"));
        assertFalse(ValidationUtils.isValidEmail("johnexample.com"));
        assertFalse(ValidationUtils.isValidEmail("john@.com"));
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    void testIsValidDate() {
        assertTrue(ValidationUtils.isValidDate("2024-01-01"));
        assertTrue(ValidationUtils.isValidDate("2024-12-31"));

        assertFalse(ValidationUtils.isValidDate("2024-13-01"));
        assertFalse(ValidationUtils.isValidDate("2024-01-32"));
        assertFalse(ValidationUtils.isValidDate("01-01-2024"));
        assertFalse(ValidationUtils.isValidDate(null));
    }

    @Test
    void testNormalizeString() {
        assertEquals("hello world", ValidationUtils.normalizeString("  Hello World  "));
        assertEquals("test", ValidationUtils.normalizeString("  TEST  "));
        assertEquals("", ValidationUtils.normalizeString(null));
        assertEquals("", ValidationUtils.normalizeString("   "));
    }

    @Test
    void testRequireNonEmpty() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("test", "Field"));

        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.requireNonEmpty(null, "Field"));
        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.requireNonEmpty("", "Field"));
        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.requireNonEmpty("   ", "Field"));
    }
}