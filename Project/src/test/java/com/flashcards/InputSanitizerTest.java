package com.flashcards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputSanitizerTest {
    @Test
    void stripsMarkupAndControlCharacters() {
        assertEquals("scriptalert('xss')/script", InputSanitizer.sanitize("<script>alert('xss')</script>"));
        assertEquals("Hello", InputSanitizer.sanitize("Hel\u0007lo"));
    }

    @Test
    void trimsWhitespaceAndCapsLength() {
        String tooLong = "a".repeat(200);
        assertEquals(120, InputSanitizer.sanitize(tooLong).length());
        assertEquals("Biology", InputSanitizer.sanitize("  Biology  "));
    }

    @Test
    void nullInputSanitizesToEmptyString() {
        assertEquals("", InputSanitizer.sanitize(null));
    }

    @Test
    void validatesUsernameFormat() {
        assertTrue(InputSanitizer.isValidUsername("teacher_1"));
        assertFalse(InputSanitizer.isValidUsername("ab"));
        assertFalse(InputSanitizer.isValidUsername("has spaces"));
        assertFalse(InputSanitizer.isValidUsername(null));
    }

    @Test
    void validatesPasswordLength() {
        assertTrue(InputSanitizer.isValidPassword("longenough".toCharArray()));
        assertFalse(InputSanitizer.isValidPassword("short".toCharArray()));
        assertFalse(InputSanitizer.isValidPassword(null));
    }
}
