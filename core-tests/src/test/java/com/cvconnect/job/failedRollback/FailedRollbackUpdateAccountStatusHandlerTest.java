package com.cvconnect.job.failedRollback;

/**
 * ============================================================
 * FILE: FailedRollbackUpdateAccountStatusHandlerTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.job.failedRollback.FailedRollbackUpdateAccountStatusHandler}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - getType()
 *   - rollback() deserialize payload và gọi delegate RestTemplateClient
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.failedRollback.FailedRollbackDto;
import com.cvconnect.dto.failedRollback.FailedRollbackUpdateAccountStatus;
import com.cvconnect.enums.FailedRollbackType;
import com.cvconnect.job.failedRollback.FailedRollbackUpdateAccountStatusHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedRollbackUpdateAccountStatusHandler - Unit Tests")
public class FailedRollbackUpdateAccountStatusHandlerTest {

    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private FailedRollbackUpdateAccountStatusHandler handler;

    /**
     * Test Case ID: TC-FRH-001
     * Test Objective: Kiểm tra handler trả đúng FailedRollbackType.
     * Input: Gọi getType().
     * Expected Output: UPDATE_ACCOUNT_STATUS.
     * Notes: Enum mapping.
     */
    @Test
    @DisplayName("getType: should return UPDATE_ACCOUNT_STATUS")
    void getType_shouldReturnEnum() {
        assertThat(handler.getType()).isEqualTo(FailedRollbackType.UPDATE_ACCOUNT_STATUS);
    }

    /**
     * Test Case ID: TC-FRH-002
     * Test Objective: Deserialize payload và delegate rollback sang RestTemplateClient.
     * Input: FailedRollbackDto chứa payload JSON orgIds/active.
     * Expected Output: Gọi rollbackUpdateAccountStatusByOrgIds với object parse đúng.
     * Notes: Verify qua ArgumentCaptor.
     */
    @Test
    @DisplayName("rollback: should deserialize payload and delegate to restTemplateClient")
    void rollback_shouldDeserializePayloadAndDelegate() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .payload("{\"orgIds\":[1,2],\"active\":true}")
                .build();

        handler.rollback(dto);

        ArgumentCaptor<FailedRollbackUpdateAccountStatus> captor =
                ArgumentCaptor.forClass(FailedRollbackUpdateAccountStatus.class);
        verify(restTemplateClient).rollbackUpdateAccountStatusByOrgIds(captor.capture());

        FailedRollbackUpdateAccountStatus payload = captor.getValue();
        assertThat(payload.getOrgIds()).isEqualTo(List.of(1L, 2L));
        assertThat(payload.getActive()).isTrue();
    }
}

