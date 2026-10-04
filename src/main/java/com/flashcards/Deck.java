package com.flashcards;

import java.util.Objects;

public record Deck(String name, String subject, int cardCount) {
    public Deck {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Deck name cannot be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Deck subject cannot be blank");
        }
        if (cardCount < 1) {
            throw new IllegalArgumentException("A deck needs at least one card");
        }
        name = name.trim();
        subject = subject.trim();
    }

    public String cardSummary() {
        return subject + " · " + cardCount + (cardCount == 1 ? " card" : " cards");
    }
}