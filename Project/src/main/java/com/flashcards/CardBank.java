package com.flashcards;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supplies the question/answer cards studied for each deck. Seeded decks ship
 * with real content; decks created later get placeholder cards generated on
 * first study session so "start studying" always has something to show.
 */
public final class CardBank {
    private final Map<String, List<Card>> cardsByDeckName = new ConcurrentHashMap<>();

    public CardBank() {
        cardsByDeckName.put("cell biology", List.of(
                new Card("What is the powerhouse of the cell?", "The mitochondrion"),
                new Card("What molecule carries genetic information?", "DNA"),
                new Card("What organelle synthesizes proteins?", "The ribosome"),
                new Card("What process do plants use to capture light energy?", "Photosynthesis"),
                new Card("What is the basic unit of life?", "The cell"),
                new Card("What structure controls what enters and exits the cell?", "The cell membrane")));

        cardsByDeckName.put("20th century history", List.of(
                new Card("In what year did World War I begin?", "1914"),
                new Card("What event started the Great Depression?", "The 1929 stock market crash"),
                new Card("In what year did World War II end?", "1945"),
                new Card("What wall fell in 1989, symbolizing the end of the Cold War?", "The Berlin Wall"),
                new Card("What treaty ended World War I?", "The Treaty of Versailles")));

        cardsByDeckName.put("calculus", List.of(
                new Card("What is the derivative of x^2?", "2x"),
                new Card("What does the integral symbol represent?", "The area under a curve"),
                new Card("What is the derivative of a constant?", "0"),
                new Card("What rule finds the derivative of a product of two functions?", "The product rule"),
                new Card("What is the limit definition of a derivative?", "lim h->0 (f(x+h) - f(x)) / h"),
                new Card("What is the derivative of sin(x)?", "cos(x)"),
                new Card("What theorem connects derivatives and integrals?", "The Fundamental Theorem of Calculus")));

        cardsByDeckName.put("literary devices", List.of(
                new Card("What device compares two things using 'like' or 'as'?", "Simile"),
                new Card("What device gives human traits to non-human things?", "Personification"),
                new Card("What device repeats the same consonant sound?", "Alliteration"),
                new Card("What device foreshadows later events in a story?", "Foreshadowing"),
                new Card("What device directly compares two unlike things without 'like' or 'as'?", "Metaphor")));
    }

    /** Returns the deck's cards, generating placeholder content on first request if none exists yet. */
    public List<Card> cardsFor(Deck deck) {
        String key = deck.name().toLowerCase();
        return cardsByDeckName.computeIfAbsent(key, name -> generatePlaceholders(deck));
    }

    private List<Card> generatePlaceholders(Deck deck) {
        List<Card> placeholders = new java.util.ArrayList<>();
        for (int i = 1; i <= deck.cardCount(); i++) {
            placeholders.add(new Card(
                    deck.subject() + " term " + i,
                    deck.subject() + " definition " + i));
        }
        return List.copyOf(placeholders);
    }
}
