package com.flashcards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

    private final Database database;

    public CardBank() {
        this(null);
    }

    /** With a database, built-in cards are inserted for seed decks that have none, then served from the cards table. */
    public CardBank(Database database) {
        this.database = database;
        cardsByDeckName.put("cell biology", new ArrayList<>(List.of(
                new Card("What is the powerhouse of the cell?", "The mitochondrion"),
                new Card("What molecule carries genetic information?", "DNA"),
                new Card("What organelle synthesizes proteins?", "The ribosome"),
                new Card("What process do plants use to capture light energy?", "Photosynthesis"),
                new Card("What is the basic unit of life?", "The cell"),
                new Card("What structure controls what enters and exits the cell?", "The cell membrane"),
                new Card("What structure contains the cell's genetic material?", "The nucleus"),
                new Card("What organelle breaks down waste materials in the cell?", "The lysosome"),
                new Card("What process do cells use to divide into two identical daughter cells?", "Mitosis"),
                new Card("What network of tubes transports proteins within the cell?", "The endoplasmic reticulum"))));

        cardsByDeckName.put("20th century history", new ArrayList<>(List.of(
                new Card("In what year did World War I begin?", "1914"),
                new Card("What event started the Great Depression?", "The 1929 stock market crash"),
                new Card("In what year did World War II end?", "1945"),
                new Card("What wall fell in 1989, symbolizing the end of the Cold War?", "The Berlin Wall"),
                new Card("What treaty ended World War I?", "The Treaty of Versailles"),
                new Card("What international organization was founded in 1945 to promote peace?", "The United Nations"),
                new Card("What attack on December 7, 1941 brought the US into World War II?", "The attack on Pearl Harbor"),
                new Card("What 1969 event was a major milestone of the space race?", "The Moon landing"))));

        cardsByDeckName.put("calculus", new ArrayList<>(List.of(
                new Card("What is the derivative of x^2?", "2x"),
                new Card("What does the integral symbol represent?", "The area under a curve"),
                new Card("What is the derivative of a constant?", "0"),
                new Card("What rule finds the derivative of a product of two functions?", "The product rule"),
                new Card("What is the limit definition of a derivative?", "lim h->0 (f(x+h) - f(x)) / h"),
                new Card("What is the derivative of sin(x)?", "cos(x)"),
                new Card("What theorem connects derivatives and integrals?", "The Fundamental Theorem of Calculus"),
                new Card("What is the derivative of cos(x)?", "-sin(x)"),
                new Card("What rule differentiates a composition of functions?", "The chain rule"),
                new Card("What does a definite integral calculate between two bounds?", "The net area under the curve between those bounds"))));

        cardsByDeckName.put("literary devices", new ArrayList<>(List.of(
                new Card("What device compares two things using 'like' or 'as'?", "Simile"),
                new Card("What device gives human traits to non-human things?", "Personification"),
                new Card("What device repeats the same consonant sound?", "Alliteration"),
                new Card("What device foreshadows later events in a story?", "Foreshadowing"),
                new Card("What device directly compares two unlike things without 'like' or 'as'?", "Metaphor"),
                new Card("What device is a deliberate exaggeration for effect?", "Hyperbole"),
                new Card("What device uses words that imitate sounds?", "Onomatopoeia"),
                new Card("What device places contrasting ideas side by side?", "Juxtaposition"))));

        if (database != null) {
            persistSeedCards();
            cardsByDeckName.clear();
        }
    }

    private void persistSeedCards() {
        try (Connection conn = database.connect()) {
            for (var entry : cardsByDeckName.entrySet()) {
                long deckId = DeckRepository.findDeckId(conn, entry.getKey());
                if (deckId >= 0 && loadCards(conn, deckId).isEmpty()) {
                    for (Card card : entry.getValue()) {
                        insertCard(conn, deckId, card);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to seed cards", e);
        }
    }

    private static List<Card> loadCards(Connection conn, long deckId) throws SQLException {
        List<Card> cards = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT question, answer FROM cards WHERE deck_id = ? ORDER BY position, id")) {
            stmt.setLong(1, deckId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cards.add(new Card(rs.getString(1), rs.getString(2)));
                }
            }
        }
        return cards;
    }

    private static void insertCard(Connection conn, long deckId, Card card) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO cards (deck_id, question, answer, position) "
                        + "SELECT ?, ?, ?, COALESCE(MAX(position) + 1, 0) FROM cards WHERE deck_id = ?")) {
            stmt.setLong(1, deckId);
            stmt.setString(2, card.front());
            stmt.setString(3, card.back());
            stmt.setLong(4, deckId);
            stmt.executeUpdate();
        }
    }

    /** Returns the deck's cards, generating placeholder content on first request if none exists yet. */
    public List<Card> cardsFor(Deck deck) {
        String key = deck.name().toLowerCase();
        return cardsByDeckName.computeIfAbsent(key, name -> {
            if (database != null) {
                List<Card> stored = loadStored(deck);
                if (!stored.isEmpty()) {
                    return stored;
                }
            }
            return new ArrayList<>(placeholders(deck.subject(), deck.cardCount()));
        });
    }

    private List<Card> loadStored(Deck deck) {
        try (Connection conn = database.connect()) {
            long deckId = DeckRepository.findDeckId(conn, deck.name());
            return deckId < 0 ? new ArrayList<>() : loadCards(conn, deckId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load cards", e);
        }
    }

    /** Appends a real card to the deck, creating its card list if none exists yet. */
    public Card addCard(Deck deck, String front, String back) {
        Card card = new Card(front, back);
        List<Card> cards = cardsFor(deck);
        if (database != null) {
            try (Connection conn = database.connect()) {
                long deckId = DeckRepository.findDeckId(conn, deck.name());
                if (deckId < 0) {
                    throw new IllegalArgumentException("Deck not found: " + deck.name());
                }
                insertCard(conn, deckId, card);
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to save card", e);
            }
        }
        cards.add(card);
        return card;
    }

    static List<Card> placeholders(String subject, int count) {
        List<Card> placeholders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            placeholders.add(new Card(subject + " term " + i, subject + " definition " + i));
        }
        return placeholders;
    }
}
