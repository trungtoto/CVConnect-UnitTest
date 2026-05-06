package com.cvconnect.userservice.failedrollback;

/**
 * ============================================================
 * FILE: FailedRollbackHandlerRegistryTest.java
 * MODULE: user-service
 * PURPOSE: Unit tests for FailedRollbackHandlerRegistry.
 * ============================================================
 */

import com.cvconnect.enums.FailedRollbackType;
import com.cvconnect.job.failedRollback.FailedRollbackHandler;
import com.cvconnect.job.failedRollback.FailedRollbackHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedRollbackHandlerRegistry - Unit Tests")
class FailedRollbackHandlerRegistryTest {

    @Mock
    private FailedRollbackHandler orgCreationHandler;

    @Mock
    private FailedRollbackHandler uploadFileHandler;

    /**
     * Test Case ID: TC-US-FRREG-001
     * Test Objective: Return correct handler by rollback type.
     * Input: registry built with ORG_CREATION and UPLOAD_FILE handlers.
     * Expected Output: getHandler(type) returns matching handler instance.
     * Notes: Registry lookup success branch.
     */
    @Test
    @DisplayName("getHandler: return matched handler")
    void getHandler_existingType_shouldReturnMatchedHandler() {
        when(orgCreationHandler.getType()).thenReturn(FailedRollbackType.ORG_CREATION);
        when(uploadFileHandler.getType()).thenReturn(FailedRollbackType.UPLOAD_FILE);

        FailedRollbackHandlerRegistry registry = new FailedRollbackHandlerRegistry(
                List.of(orgCreationHandler, uploadFileHandler)
        );

        assertThat(registry.getHandler(FailedRollbackType.ORG_CREATION)).isSameAs(orgCreationHandler);
        assertThat(registry.getHandler(FailedRollbackType.UPLOAD_FILE)).isSameAs(uploadFileHandler);
    }

    /**
     * Test Case ID: TC-US-FRREG-002
     * Test Objective: Return null for type without configured handler.
     * Input: registry built with ORG_CREATION only.
     * Expected Output: getHandler(UPLOAD_FILE) returns null.
     * Notes: Registry lookup miss branch.
     */
    @Test
    @DisplayName("getHandler: return null when handler not registered")
    void getHandler_missingType_shouldReturnNull() {
        when(orgCreationHandler.getType()).thenReturn(FailedRollbackType.ORG_CREATION);

        FailedRollbackHandlerRegistry registry = new FailedRollbackHandlerRegistry(
                List.of(orgCreationHandler)
        );

        assertThat(registry.getHandler(FailedRollbackType.UPLOAD_FILE)).isNull();
    }
}
