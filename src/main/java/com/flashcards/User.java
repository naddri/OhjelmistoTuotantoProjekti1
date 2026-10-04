package com.flashcards;

import java.util.Objects;

/** An authenticated account. The password is never stored, only its salted hash. */
public record User(String username, String passwordHash, String salt, Role role) {
    public User {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        Objects.requireNonNull(salt, "salt cannot be null");
        Objects.requireNonNull(role, "role cannot be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean canManageContent() {
        return role == Role.TEACHER || role == Role.ADMIN;
    }
}
