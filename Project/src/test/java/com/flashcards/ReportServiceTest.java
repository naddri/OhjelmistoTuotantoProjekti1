package com.flashcards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReportServiceTest {
    private DeckRepository deckRepository;
    private AuthService authService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        deckRepository = new DeckRepository(List.of(
                new Deck("Cell Biology", "Biology", 6),
                new Deck("Genetics", "Biology", 3),
                new Deck("Calculus", "Mathematics", 7)));
        authService = new AuthService();
        reportService = new ReportService(deckRepository, authService);
    }

    @Test
    void aggregatesTotalsAndPerSubjectBreakdown() {
        ReportService.DashboardReport report = reportService.generate();

        assertEquals(3, report.totalDecks());
        assertEquals(16, report.totalCards());
        assertEquals(9, report.decksPerSubject().get("Biology") + report.cardsPerSubject().get("Mathematics"));
        assertEquals(2, report.decksPerSubject().get("Biology"));
        assertEquals(1, report.decksPerSubject().get("Mathematics"));
    }

    @Test
    void cachesReportUntilRepositoryChanges() {
        ReportService.DashboardReport first = reportService.generate();
        ReportService.DashboardReport second = reportService.generate();
        assertSame(first, second);

        deckRepository.addDeck("New Deck", "History", 4);
        ReportService.DashboardReport third = reportService.generate();

        assertEquals(4, third.totalDecks());
    }

    @Test
    void includesRegisteredUserCount() {
        authService.register("student1", "password123".toCharArray(), Role.STUDENT);
        authService.register("student2", "password123".toCharArray(), Role.STUDENT);

        assertEquals(2, reportService.generate().registeredUsers());
    }
}
