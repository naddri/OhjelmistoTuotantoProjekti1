package com.flashcards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LocalizationTest {
    @Test
    void loadsTranslationsAndAppliesRightToLeftOrientationForArabic() {
        Localization localization = new Localization();

        localization.setLanguage("ja");
        assertEquals("言語", localization.text("language.label"));
        assertFalse(localization.isRightToLeft());

        localization.setLanguage("ar");
        assertEquals("اللغة", localization.text("language.label"));
        assertTrue(localization.isRightToLeft());
    }

    @Test
    void formatsNumbersAndDatesUsingSelectedLocale() {
        Localization localization = new Localization();
        String englishNumber = localization.number(12345);
        String englishDate = localization.dateTime(Instant.parse("2024-01-02T03:04:05Z"));
        assertEquals("12,345", englishNumber);

        localization.setLanguage("ja");
        assertNotEquals(englishDate, localization.dateTime(Instant.parse("2024-01-02T03:04:05Z")));
    }
}
