package com.flashcards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the deck collection and provides validated inserts plus search,
 * filtering and sorting used by the deck list screen.
 */
public final class DeckRepository {
    private final List<Deck> decks = new ArrayList<>();
    private final Database database;
    private int version;

    public DeckRepository(List<Deck> initialDecks) {
        this.database = null;
        decks.addAll(initialDecks);
    }

    /** Database-backed repository; seed decks are inserted (without cards) when missing by title. */
    public DeckRepository(Database database, List<Deck> seedDecks) {
        this.database = database;
        try (Connection conn = database.connect()) {
            for (Deck seed : seedDecks) {
                if (findDeckId(conn, seed.name()) < 0) {
                    insertDeck(conn, seed.name(), seed.subject());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to seed decks", e);
        }
        reload();
    }

    /** Re-reads decks from the database; decks without cards are not listed. */
    public void reload() {
        if (database == null) {
            return;
        }
        String sql = "SELECT d.title, COALESCE(c.name, 'General') AS subject, COUNT(k.id) AS n "
                + "FROM decks d LEFT JOIN categories c ON c.id = d.category_id "
                + "JOIN cards k ON k.deck_id = d.id "
                + "GROUP BY d.id, d.title, c.name ORDER BY d.id";
        List<Deck> loaded = new ArrayList<>();
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                loaded.add(new Deck(rs.getString("title"), rs.getString("subject"), rs.getInt("n")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load decks", e);
        }
        decks.clear();
        decks.addAll(loaded);
        version++;
    }

    static long findDeckId(Connection conn, String title) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM decks WHERE LOWER(title) = LOWER(?) ORDER BY id LIMIT 1")) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    private long insertDeck(Connection conn, String title, String subject) throws SQLException {
        long categoryId = categoryId(conn, subject);
        long ownerId;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id FROM users ORDER BY FIELD(role, 'admin', 'teacher', 'student'), id LIMIT 1")) {
            if (!rs.next()) {
                throw new IllegalStateException("No user exists to own the deck");
            }
            ownerId = rs.getLong(1);
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO decks (owner_id, category_id, title) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, ownerId);
            stmt.setLong(2, categoryId);
            stmt.setString(3, title);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long categoryId(Connection conn, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM categories WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO categories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void persistNewDeck(Deck deck) {
        try (Connection conn = database.connect()) {
            conn.setAutoCommit(false);
            try {
                long deckId = insertDeck(conn, deck.name(), deck.subject());
                List<Card> placeholders = CardBank.placeholders(deck.subject(), deck.cardCount());
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO cards (deck_id, question, answer, position) VALUES (?, ?, ?, ?)")) {
                    for (int i = 0; i < placeholders.size(); i++) {
                        stmt.setLong(1, deckId);
                        stmt.setString(2, placeholders.get(i).front());
                        stmt.setString(3, placeholders.get(i).back());
                        stmt.setInt(4, i);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save deck", e);
        }
    }

    public Deck addDeck(String name, String subject, int cardCount) {
        String cleanName = InputSanitizer.sanitize(name);
        String cleanSubject = InputSanitizer.sanitize(subject);
        Deck deck = new Deck(cleanName, cleanSubject, cardCount);
        if (database != null) {
            persistNewDeck(deck);
        }
        decks.add(deck);
        version++;
        return deck;
    }

    /** Replaces the deck's stored card count, e.g. after a card is added to it, so summaries stay accurate. */
    public Deck updateCardCount(Deck deck, int newCardCount) {
        int index = decks.indexOf(deck);
        if (index < 0) {
            throw new IllegalArgumentException("Deck not found: " + deck.name());
        }
        Deck updated = new Deck(deck.name(), deck.subject(), newCardCount);
        decks.set(index, updated);
        version++;
        return updated;
    }

    /** Increments whenever the deck collection changes, used to invalidate downstream caches. */
    public int version() {
        return version;
    }

    public List<Deck> all() {
        return List.copyOf(decks);
    }

    public Set<String> subjects() {
        return decks.stream()
                .map(Deck::subject)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    /** Returns decks matching the (case-insensitive) search text and subject filter, sorted per option. */
    public List<Deck> search(String query, String subjectFilter, DeckSortOption sortOption) {
        String needle = query == null ? "" : query.strip().toLowerCase();
        List<Deck> results = new ArrayList<>();
        for (Deck deck : decks) {
            boolean matchesText = needle.isEmpty()
                    || deck.name().toLowerCase().contains(needle)
                    || deck.subject().toLowerCase().contains(needle);
            boolean matchesSubject = subjectFilter == null
                    || subjectFilter.isBlank()
                    || subjectFilter.equalsIgnoreCase("All")
                    || deck.subject().equalsIgnoreCase(subjectFilter);
            if (matchesText && matchesSubject) {
                results.add(deck);
            }
        }
        results.sort(comparatorFor(sortOption));
        return results;
    }

    private Comparator<Deck> comparatorFor(DeckSortOption sortOption) {
        DeckSortOption effective = sortOption == null ? DeckSortOption.NAME_ASC : sortOption;
        return switch (effective) {
            case NAME_ASC -> Comparator.comparing(Deck::name, String.CASE_INSENSITIVE_ORDER);
            case SUBJECT_ASC -> Comparator.comparing(Deck::subject, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Deck::name, String.CASE_INSENSITIVE_ORDER);
            case CARD_COUNT_DESC -> Comparator.comparingInt(Deck::cardCount).reversed();
        };
    }
}
