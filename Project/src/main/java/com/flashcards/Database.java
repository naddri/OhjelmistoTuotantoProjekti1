package com.flashcards;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC connection settings for the quizcard_app MariaDB/MySQL database.
 * Configured through DB_URL, DB_USER and DB_PASSWORD environment variables.
 */
public final class Database {
    private static final String DEFAULT_URL =
            "jdbc:mariadb://localhost:3306/quizcard_app?useUnicode=true&characterEncoding=utf8";

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database fromEnvironment() {
        return new Database(
                env("DB_URL", DEFAULT_URL),
                env("DB_USER", "root"),
                env("DB_PASSWORD", ""));
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean isAvailable() {
        try (Connection ignored = connect()) {
            return true;
        } catch (SQLException e) {
            System.err.println("Database unavailable: " + e.getMessage());
            return false;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
