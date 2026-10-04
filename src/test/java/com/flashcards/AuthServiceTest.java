package com.flashcards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void registersAndLogsInWithMatchingCredentials() {
        authService.register("student1", "password123".toCharArray(), Role.STUDENT);

        User user = authService.login("student1", "password123".toCharArray());

        assertEquals("student1", user.username());
        assertEquals(Role.STUDENT, user.role());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        authService.register("student1", "password123".toCharArray(), Role.STUDENT);

        assertThrows(SecurityException.class, () -> authService.login("student1", "wrong-password".toCharArray()));
    }

    @Test
    void rejectsLoginForUnknownUser() {
        assertThrows(SecurityException.class, () -> authService.login("ghost", "password123".toCharArray()));
    }

    @Test
    void rejectsDuplicateUsernameRegardlessOfCase() {
        authService.register("student1", "password123".toCharArray(), Role.STUDENT);

        assertThrows(IllegalStateException.class,
                () -> authService.register("Student1", "another-pass".toCharArray(), Role.STUDENT));
    }

    @Test
    void rejectsWeakPasswordsAndInvalidUsernames() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("ab", "password123".toCharArray(), Role.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> authService.register("validname", "short".toCharArray(), Role.STUDENT));
    }

    @Test
    void authorizationRespectsRoleHierarchy() {
        User admin = authService.register("admin1", "password123".toCharArray(), Role.ADMIN);
        User teacher = authService.register("teacher1", "password123".toCharArray(), Role.TEACHER);
        User student = authService.register("student2", "password123".toCharArray(), Role.STUDENT);

        assertTrue(authService.isAuthorized(admin, Role.ADMIN));
        assertTrue(authService.isAuthorized(teacher, Role.TEACHER));
        assertTrue(authService.isAuthorized(student, Role.STUDENT));
        assertEquals(false, authService.isAuthorized(student, Role.ADMIN));
        assertEquals(false, authService.isAuthorized(teacher, Role.ADMIN));
    }
}
