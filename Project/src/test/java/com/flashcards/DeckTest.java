package com.flashcards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeckTest {
    @Test
    void createsReadableCardSummary() {
        Deck deck = new Deck(" Cell Biology ", "Biology", 6);

        assertEquals("Biology · 6 cards", deck.cardSummary());
    }

    @Test
    void usesSingularCardForOneCardDeck() {
        Deck deck = new Deck("Basics", "Math", 1);

        assertEquals("Math · 1 card", deck.cardSummary());
    }

    @Test
    void rejectsEmptyDeckDetails() {
        assertThrows(IllegalArgumentException.class, () -> new Deck("", "Biology", 3));
        assertThrows(IllegalArgumentException.class, () -> new Deck("Biology", "", 3));
        assertThrows(IllegalArgumentException.class, () -> new Deck("Biology", "Biology", 0));
    }
}