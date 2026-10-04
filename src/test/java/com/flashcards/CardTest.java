package com.flashcards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardTest {
    @Test
    void trimsFrontAndBackText() {
        Card card = new Card(" What is 2+2? ", " 4 ");

        assertEquals("What is 2+2?", card.front());
        assertEquals("4", card.back());
    }

    @Test
    void rejectsBlankSides() {
        assertThrows(IllegalArgumentException.class, () -> new Card("", "answer"));
        assertThrows(IllegalArgumentException.class, () -> new Card("question", ""));
        assertThrows(IllegalArgumentException.class, () -> new Card(null, "answer"));
    }
}
