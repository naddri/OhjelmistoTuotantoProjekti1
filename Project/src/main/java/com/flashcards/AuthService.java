package com.flashcards;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory authentication and authorization module.
 * Validates and sanitizes credentials and never stores plaintext passwords.
 */
public final class AuthService {
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();

    public User register(String username, char[] password, Role role) {
        String cleanUsername = InputSanitizer.sanitize(username);
        if (!InputSanitizer.isValidUsername(cleanUsername)) {
            throw new IllegalArgumentException(
                    "Username must be 3-32 characters (letters, digits, '.', '_' or '-')");
        }
        if (!InputSanitizer.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (usersByUsername.containsKey(cleanUsername.toLowerCase())) {
            throw new IllegalStateException("Username is already taken");
        }
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);
        User user = new User(cleanUsername, hash, salt, role);
        usersByUsername.put(cleanUsername.toLowerCase(), user);
        return user;
    }

    public User login(String username, char[] password) {
        String cleanUsername = InputSanitizer.sanitize(username);
        User user = usersByUsername.get(cleanUsername.toLowerCase());
        if (user == null || !PasswordHasher.matches(password, user.salt(), user.passwordHash())) {
            throw new SecurityException("Invalid username or password");
        }
        return user;
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(InputSanitizer.sanitize(username).toLowerCase()));
    }

    public boolean isAuthorized(User user, Role required) {
        if (user == null) {
            return false;
        }
        return switch (required) {
            case STUDENT -> true;
            case TEACHER -> user.role() == Role.TEACHER || user.role() == Role.ADMIN;
            case ADMIN -> user.role() == Role.ADMIN;
        };
    }

    public int userCount() {
        return usersByUsername.size();
    }
}
