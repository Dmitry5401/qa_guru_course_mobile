package guru.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test that verifies the JUnit 5 toolchain is wired up correctly.
 * Serves as the representative "hello world" flow for the project setup.
 */
class SmokeTest {

    @Test
    @DisplayName("JUnit 5 executes and assertions pass")
    void junitToolchainWorks() {
        assertTrue(true, "JUnit 5 is running");
        assertEquals(4, 2 + 2, "Basic arithmetic works");
    }
}
