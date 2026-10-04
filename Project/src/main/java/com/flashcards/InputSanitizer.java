package com.flashcards;

import java.util.regex.Pattern;

/**
 * Validates and sanitizes untrusted, user-supplied text before it is stored or
 * displayed, mitigating injection and markup-based issues (OWASP A03).
 */
public final class InputSanitizer {
    private static final int MAX_LENGTH = 120;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,32}$");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]");
    private static final Pattern MARKUP = Pattern.compile("[<>]");

    private InputSanitizer() {
    }

    /** Strips control characters and markup delimiters, then trims and caps length. */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String noControls = CONTROL_CHARS.matcher(raw).replaceAll("");
        String noMarkup = MARKUP.matcher(noControls).replaceAll("");
        String trimmed = noMarkup.strip();
        return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(char[] password) {
        return password != null && password.length >= 8;
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
    }
}
