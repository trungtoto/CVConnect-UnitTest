package com.cvconnect.job.failedRollback;

/**
 * ============================================================
 * FILE: FailedRollbackHandlerRegistryTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.job.failedRollback.FailedRollbackHandlerRegistry}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - Lấy handler theo type khi tồn tại
 *   - Trả về null khi không có handler phù hợp
 * ============================================================
 */

import com.cvconnect.enums.FailedRollbackType;
import com.cvconnect.job.failedRollback.FailedRollbackHandler;
import com.cvconnect.job.failedRollback.FailedRollbackHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FailedRollbackHandlerRegistry - Unit Tests")
public class FailedRollbackHandlerRegistryTest {

    /**
     * Test Case ID: TC-FRREG-001
     * Test Objective: Trả đúng handler khi registry có type tương ứng.
     * Input: Registry chứa handler UPDATE_ACCOUNT_STATUS.
     * Expected Output: getHandler(type) trả đúng instance handler.
     * Notes: Happy path.
     */
    @Test
    @DisplayName("getHandler: return handler when type exists")
    void getHandler_existingType_returnsHandler() {
        FailedRollbackHandler handler = mock(FailedRollbackHandler.class);
        when(handler.getType()).thenReturn(FailedRollbackType.UPDATE_ACCOUNT_STATUS);
        FailedRollbackHandlerRegistry registry = new FailedRollbackHandlerRegistry(List.of(handler));

        FailedRollbackHandler result = registry.getHandler(FailedRollbackType.UPDATE_ACCOUNT_STATUS);

        assertThat(result).isSameAs(handler);
    }

    /**
     * Test Case ID: TC-FRREG-002
     * Test Objective: Trả null khi không tìm thấy handler theo type.
     * Input: Registry chỉ có 1 handler, gọi getHandler(null).
     * Expected Output: Kết quả null.
     * Notes: Negative path.
     */
    @Test
    @DisplayName("getHandler: return null when type does not exist")
    void getHandler_missingType_returnsNull() {
        FailedRollbackHandler handler = mock(FailedRollbackHandler.class);
        when(handler.getType()).thenReturn(FailedRollbackType.UPDATE_ACCOUNT_STATUS);
        FailedRollbackHandlerRegistry registry = new FailedRollbackHandlerRegistry(List.of(handler));

        FailedRollbackHandler result = registry.getHandler(null);

        assertThat(result).isNull();
    }
}

