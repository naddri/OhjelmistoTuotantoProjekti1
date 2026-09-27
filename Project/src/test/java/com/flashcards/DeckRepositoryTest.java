package com.flashcards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckRepositoryTest {
    private DeckRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DeckRepository(List.of(
                new Deck("Cell Biology", "Biology", 6),
                new Deck("Genetics", "Biology", 3),
                new Deck("Calculus", "Mathematics", 7)));
    }

    @Test
    void searchFiltersByNameOrSubjectCaseInsensitively() {
        List<Deck> results = repository.search("bio", "All", DeckSortOption.NAME_ASC);

        assertEquals(2, results.size());
    }

    @Test
    void searchFiltersBySubject() {
        List<Deck> results = repository.search("", "Mathematics", DeckSortOption.NAME_ASC);

        assertEquals(1, results.size());
        assertEquals("Calculus", results.get(0).name());
    }

    @Test
    void sortsByNameAscending() {
        List<Deck> results = repository.search("", "All", DeckSortOption.NAME_ASC);

        assertEquals(List.of("Calculus", "Cell Biology", "Genetics"),
                results.stream().map(Deck::name).toList());
    }

    @Test
    void sortsByCardCountDescending() {
        List<Deck> results = repository.search("", "All", DeckSortOption.CARD_COUNT_DESC);

        assertEquals(List.of("Calculus", "Cell Biology", "Genetics"),
                results.stream().map(Deck::name).toList());
    }

    @Test
    void addDeckSanitizesInputAndIncrementsVersion() {
        int versionBefore = repository.version();

        Deck deck = repository.addDeck("  <b>New Deck</b>  ", "History", 4);

        assertEquals("bNew Deck/b", deck.name());
        assertTrue(repository.version() > versionBefore);
        assertEquals(4, repository.all().size());
    }

    @Test
    void subjectsReturnsUniqueSubjectsInInsertionOrder() {
        assertEquals(List.of("Biology", "Mathematics"), List.copyOf(repository.subjects()));
    }
}
