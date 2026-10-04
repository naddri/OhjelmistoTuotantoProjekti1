package com.flashcards;

/** A single question/answer pair belonging to a {@link Deck}. */
public record Card(String front, String back) {
    public Card {
        if (front == null || front.isBlank()) {
            throw new IllegalArgumentException("Card front cannot be blank");
        }
        if (back == null || back.isBlank()) {
            throw new IllegalArgumentException("Card back cannot be blank");
        }
        front = front.trim();
        back = back.trim();
    }
}
