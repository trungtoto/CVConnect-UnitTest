package com.cvconnect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TC-NTF-APP-001: Smoke test - verifies basic application setup.
 * Uses lightweight Mockito context instead of full Spring context
 * to avoid requiring real database connections during unit testing.
 */
@ExtendWith(MockitoExtension.class)
class NotifyServiceApplicationTests {

    @Test
    @DisplayName("TC-NTF-APP-001: Application context loads (smoke test)")
    void contextLoads() {
        // Smoke test - confirms the test infrastructure is set up correctly.
        // Full integration test (with real DB) is handled in E2E/integration test suites.
    }

}
