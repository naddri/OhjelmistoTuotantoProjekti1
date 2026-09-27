package com.flashcards;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes analytics for the administrator reporting dashboard.
 * Results are cached and only recomputed when the deck repository changes,
 * reducing repeated aggregation work on every render.
 */
public final class ReportService {
    private final DeckRepository deckRepository;
    private final AuthService authService;

    private DashboardReport cachedReport;
    private int cachedVersion = -1;

    public ReportService(DeckRepository deckRepository, AuthService authService) {
        this.deckRepository = deckRepository;
        this.authService = authService;
    }

    public DashboardReport generate() {
        int currentVersion = deckRepository.version();
        if (cachedReport != null && cachedVersion == currentVersion) {
            return cachedReport;
        }

        List<Deck> decks = deckRepository.all();
        Map<String, Integer> decksPerSubject = new LinkedHashMap<>();
        Map<String, Integer> cardsPerSubject = new LinkedHashMap<>();
        int totalCards = 0;

        for (Deck deck : decks) {
            decksPerSubject.merge(deck.subject(), 1, Integer::sum);
            cardsPerSubject.merge(deck.subject(), deck.cardCount(), Integer::sum);
            totalCards += deck.cardCount();
        }

        double averageCardsPerDeck = decks.isEmpty() ? 0.0 : (double) totalCards / decks.size();

        cachedReport = new DashboardReport(
                decks.size(),
                totalCards,
                averageCardsPerDeck,
                Map.copyOf(decksPerSubject),
                Map.copyOf(cardsPerSubject),
                authService.userCount());
        cachedVersion = currentVersion;
        return cachedReport;
    }

    public record DashboardReport(
            int totalDecks,
            int totalCards,
            double averageCardsPerDeck,
            Map<String, Integer> decksPerSubject,
            Map<String, Integer> cardsPerSubject,
            int registeredUsers) {
    }
}
