package com.flashcards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CardBankTest {
    private final CardBank cardBank = new CardBank();

    @Test
    void returnsSeededCardsForBuiltInDecks() {
        Deck deck = new Deck("Cell Biology", "Biology", 10);

        List<Card> cards = cardBank.cardsFor(deck);

        assertEquals(10, cards.size());
        assertEquals("What is the powerhouse of the cell?", cards.get(0).front());
    }

    @Test
    void seededLookupIsCaseInsensitive() {
        Deck deck = new Deck("cell biology", "Biology", 10);

        assertEquals(10, cardBank.cardsFor(deck).size());
    }

    @Test
    void generatesPlaceholderCardsMatchingCountForUnknownDecks() {
        Deck deck = new Deck("Organic Chemistry", "Chemistry", 4);

        List<Card> cards = cardBank.cardsFor(deck);

        assertEquals(4, cards.size());
        assertEquals("Chemistry term 1", cards.get(0).front());
        assertEquals("Chemistry definition 1", cards.get(0).back());
    }

    @Test
    void generatedPlaceholdersAreCachedAndStable() {
        Deck deck = new Deck("Organic Chemistry", "Chemistry", 4);

        List<Card> first = cardBank.cardsFor(deck);
        List<Card> second = cardBank.cardsFor(deck);

        assertSame(first, second);
    }
}
