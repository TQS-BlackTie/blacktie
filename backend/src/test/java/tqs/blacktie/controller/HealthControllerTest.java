package tqs.blacktie.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HealthController Tests")
class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    @DisplayName("Should return health status")
    void whenGetHealth_thenReturnStatus() {
        Map<String, String> result = healthController.health();

        assertNotNull(result);
        assertEquals("Up", result.get("status"));
        assertEquals("Backend is running", result.get("message"));
    }

    @Test
    @DisplayName("Should return exactly two fields")
    void whenGetHealth_thenReturnTwoFields() {
        Map<String, String> result = healthController.health();

        assertEquals(2, result.size());
        assertTrue(result.containsKey("status"));
        assertTrue(result.containsKey("message"));
    }
}
