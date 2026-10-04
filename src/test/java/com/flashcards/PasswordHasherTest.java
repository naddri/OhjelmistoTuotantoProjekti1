package com.flashcards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void sameSaltAndPasswordProduceSameHash() {
        String salt = PasswordHasher.generateSalt();
        String hash1 = PasswordHasher.hash("correct horse".toCharArray(), salt);
        String hash2 = PasswordHasher.hash("correct horse".toCharArray(), salt);

        assertEquals(hash1, hash2);
    }

    @Test
    void differentSaltsProduceDifferentHashesForSamePassword() {
        String hash1 = PasswordHasher.hash("password123".toCharArray(), PasswordHasher.generateSalt());
        String hash2 = PasswordHasher.hash("password123".toCharArray(), PasswordHasher.generateSalt());

        assertNotEquals(hash1, hash2);
    }

    @Test
    void matchesReturnsTrueForCorrectPasswordAndFalseOtherwise() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("s3cret!!".toCharArray(), salt);

        assertTrue(PasswordHasher.matches("s3cret!!".toCharArray(), salt, hash));
        assertFalse(PasswordHasher.matches("wrong-password".toCharArray(), salt, hash));
    }

    @Test
    void rejectsEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(new char[0], PasswordHasher.generateSalt()));
    }
}
