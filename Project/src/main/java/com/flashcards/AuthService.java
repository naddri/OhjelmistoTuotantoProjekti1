package com.flashcards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication and authorization module, optionally persisted in the users table.
 * Validates and sanitizes credentials and never stores plaintext passwords.
 */
public final class AuthService {
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();
    private final Database database;

    public AuthService() {
        this(null);
    }

    public AuthService(Database database) {
        this.database = database;
        if (database != null) {
            loadUsers();
        }
    }

    /** Rows are stored as password_hash = "salt:hash"; rows in any other format are skipped. */
    private void loadUsers() {
        String sql = "SELECT username, password_hash, role FROM users ORDER BY id";
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String[] parts = rs.getString("password_hash").split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                Role role = Role.valueOf(rs.getString("role").toUpperCase());
                String username = rs.getString("username");
                usersByUsername.put(username.toLowerCase(), new User(username, parts[1], parts[0], role));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load users", e);
        }
    }

    private void insertUser(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name, role) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.username().toLowerCase() + "@studycard.local");
            stmt.setString(3, user.salt() + ":" + user.passwordHash());
            stmt.setString(4, user.username());
            stmt.setString(5, user.username());
            stmt.setString(6, user.role().name().toLowerCase());
            stmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException("Username is already taken", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save user", e);
        }
    }

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
        if (database != null) {
            insertUser(user);
        }
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
