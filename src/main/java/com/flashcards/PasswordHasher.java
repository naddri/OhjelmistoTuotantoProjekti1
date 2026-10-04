package com.flashcards;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Salted PBKDF2 password hashing. Avoids storing or comparing plaintext
 * passwords, mitigating credential exposure (OWASP A02: Cryptographic Failures).
 */
public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(char[] password, String salt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password,
                    Base64.getDecoder().decode(salt),
                    ITERATIONS,
                    KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    public static boolean matches(char[] password, String salt, String expectedHash) {
        String actualHash = hash(password, salt);
        return constantTimeEquals(actualHash, expectedHash);
    }

    /** Constant-time comparison to prevent timing attacks on hash comparison. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
