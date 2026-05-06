package com.cvconnect.userservice.failedrollback;

/**
 * ============================================================
 * FILE: FailedRollbackRetryJobTest.java
 * MODULE: user-service
 * PURPOSE: Unit tests for FailedRollbackRetryJob execution logic.
 * ============================================================
 */

import com.cvconnect.constant.Constants;
import com.cvconnect.dto.failedRollback.FailedRollbackDto;
import com.cvconnect.enums.FailedRollbackType;
import com.cvconnect.job.failedRollback.FailedRollbackHandler;
import com.cvconnect.job.failedRollback.FailedRollbackHandlerRegistry;
import com.cvconnect.job.failedRollback.FailedRollbackRetryJob;
import com.cvconnect.service.FailedRollbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedRollbackRetryJob - Unit Tests")
class FailedRollbackRetryJobTest {

    @Mock
    private FailedRollbackService failedRollbackService;

    @Mock
    private FailedRollbackHandlerRegistry handlerRegistry;

    @Mock
    private FailedRollbackHandler handler;

    @InjectMocks
    private FailedRollbackRetryJob job;

    /**
     * Test Case ID: TC-US-FRJOB-001
     * Test Objective: Validate static metadata of retry job.
     * Input: call metadata methods.
     * Expected Output: expected job name and blank schedule metadata.
     * Notes: Configuration contract assertion.
     */
    @Test
    @DisplayName("metadata: return expected values")
    void metadata_shouldReturnExpectedValues() {
        assertThat(job.getJobName()).isEqualTo(Constants.JobName.FAILED_ROLLBACK_RETRY);
        assertThat(job.getScheduleType()).isEmpty();
        assertThat(job.getExpression()).isEmpty();
    }

    /**
     * Test Case ID: TC-US-FRJOB-002
     * Test Objective: Do nothing when there is no pending failed rollback.
     * Input: service returns empty list.
     * Expected Output: no handler lookup and no save.
     * Notes: DB read empty branch.
     */
    @Test
    @DisplayName("runJob: no pending failed rollback")
    void runJob_noPending_shouldDoNothing() {
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of());

        job.runJob();

        verifyNoInteractions(handlerRegistry);
        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-US-FRJOB-003
     * Test Objective: Skip item when rollback type string is invalid.
     * Input: DTO type=UNKNOWN_TYPE.
     * Expected Output: no handler lookup and no save.
     * Notes: Invalid enum branch.
     */
    @Test
    @DisplayName("runJob: skip invalid rollback type")
    void runJob_invalidType_shouldSkip() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(1L)
                .type("UNKNOWN_TYPE")
                .retryCount(0)
                .build();

        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));

        job.runJob();

        verify(handlerRegistry, never()).getHandler(any());
        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-US-FRJOB-004
     * Test Objective: Skip item when no handler is registered for valid type.
     * Input: DTO type=ORG_CREATION, registry returns null.
     * Expected Output: no save.
     * Notes: Missing handler branch.
     */
    @Test
    @DisplayName("runJob: skip when handler is missing")
    void runJob_missingHandler_shouldSkip() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(2L)
                .type(FailedRollbackType.ORG_CREATION.name())
                .retryCount(2)
                .build();

        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.ORG_CREATION)).thenReturn(null);

        job.runJob();

        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-US-FRJOB-005
     * Test Objective: Mark rollback as success when handler runs without exception.
     * Input: one valid DTO and handler performs rollback.
     * Expected Output: dto.status=true and dto saved.
     * Notes: CheckDB behavior for success update.
     */
    @Test
    @DisplayName("runJob: successful rollback sets status and saves")
    void runJob_success_shouldSetStatusAndSave() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(3L)
                .type(FailedRollbackType.UPLOAD_FILE.name())
                .status(false)
                .retryCount(0)
                .build();

        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.UPLOAD_FILE)).thenReturn(handler);

        job.runJob();

        verify(handler).rollback(dto);
        verify(failedRollbackService).save(dto);
        assertThat(dto.getStatus()).isTrue();
        assertThat(dto.getRetryCount()).isEqualTo(0);
    }

    /**
     * Test Case ID: TC-US-FRJOB-006
     * Test Objective: Increase retryCount when rollback fails.
     * Input: one valid DTO and handler throws exception.
     * Expected Output: retryCount +1 and dto saved.
     * Notes: Rollback retry branch, representing compensation path.
     */
    @Test
    @DisplayName("runJob: failed rollback increments retryCount and saves")
    void runJob_failure_shouldIncreaseRetryAndSave() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(4L)
                .type(FailedRollbackType.ORG_CREATION.name())
                .retryCount(1)
                .build();

        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.ORG_CREATION)).thenReturn(handler);
        doThrow(new RuntimeException("rollback failed")).when(handler).rollback(dto);

        job.runJob();

        verify(handler).rollback(dto);
        verify(failedRollbackService).save(dto);
        assertThat(dto.getRetryCount()).isEqualTo(2);
    }
}
