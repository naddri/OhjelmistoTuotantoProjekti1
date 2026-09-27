package com.flashcards;

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
    private int version;

    public DeckRepository(List<Deck> initialDecks) {
        decks.addAll(initialDecks);
    }

    public Deck addDeck(String name, String subject, int cardCount) {
        String cleanName = InputSanitizer.sanitize(name);
        String cleanSubject = InputSanitizer.sanitize(subject);
        Deck deck = new Deck(cleanName, cleanSubject, cardCount);
        decks.add(deck);
        version++;
        return deck;
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
