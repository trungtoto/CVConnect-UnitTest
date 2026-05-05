package com.cvconnect.job.failedRollback;

/**
 * ============================================================
 * FILE: FailedRollbackRetryJobTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.job.failedRollback.FailedRollbackRetryJob}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - Metadata của job
 *   - Branch không có pending/không có handler/type không hợp lệ
 *   - Branch rollback thành công/thất bại và cập nhật trạng thái retry
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
public class FailedRollbackRetryJobTest {

    @Mock
    private FailedRollbackService failedRollbackService;

    @Mock
    private FailedRollbackHandlerRegistry handlerRegistry;

    @Mock
    private FailedRollbackHandler handler;

    @InjectMocks
    private FailedRollbackRetryJob job;

    /**
     * Test Case ID: TC-FRJOB-001
     * Test Objective: Kiểm tra metadata của job.
     * Input: Gọi các hàm metadata của job.
     * Expected Output: Trả về jobName/scheduleType/expression đúng.
     * Notes: Static metadata assertions.
     */
    @Test
    @DisplayName("metadata methods should return expected values")
    void metadata_shouldReturnExpectedValues() {
        assertThat(job.getJobName()).isEqualTo(Constants.JobName.FAILED_ROLLBACK_RETRY);
        assertThat(job.getScheduleType()).isEmpty();
        assertThat(job.getExpression()).isEmpty();
    }

    /**
     * Test Case ID: TC-FRJOB-002
     * Test Objective: Không xử lý khi không có failed rollback pending.
     * Input: Service trả danh sách rỗng.
     * Expected Output: Không gọi handler registry và không save.
     * Notes: Empty queue branch.
     */
    @Test
    @DisplayName("runJob: no pending rollbacks")
    void runJob_noPendingRollbacks() {
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of());

        job.runJob();

        verifyNoInteractions(handlerRegistry);
        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-FRJOB-003
     * Test Objective: Bỏ qua bản ghi có type không xác định.
     * Input: DTO type = UNKNOWN_TYPE.
     * Expected Output: Không resolve handler, không save.
     * Notes: Invalid type branch.
     */
    @Test
    @DisplayName("runJob: unknown type should be skipped")
    void runJob_unknownType_shouldSkip() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(11L)
                .type("UNKNOWN_TYPE")
                .retryCount(1)
                .build();
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));

        job.runJob();

        verify(handlerRegistry, never()).getHandler(any());
        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-FRJOB-004
     * Test Objective: Tiếp tục xử lý khi không tìm thấy handler.
     * Input: DTO type hợp lệ nhưng registry trả null handler.
     * Expected Output: Không save.
     * Notes: Missing handler branch.
     */
    @Test
    @DisplayName("runJob: missing handler should continue")
    void runJob_missingHandler_shouldContinue() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(12L)
                .type(FailedRollbackType.UPDATE_ACCOUNT_STATUS.name())
                .retryCount(2)
                .build();
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.UPDATE_ACCOUNT_STATUS)).thenReturn(null);

        job.runJob();

        verify(failedRollbackService, never()).save(any());
    }

    /**
     * Test Case ID: TC-FRJOB-005
     * Test Objective: Đánh dấu thành công khi rollback chạy thành công.
     * Input: Handler rollback không ném lỗi.
     * Expected Output: status=true, retryCount không tăng, save được gọi.
     * Notes: Success branch.
     */
    @Test
    @DisplayName("runJob: successful rollback sets status true and saves")
    void runJob_successfulRollback_shouldSetStatusAndSave() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(13L)
                .type(FailedRollbackType.UPDATE_ACCOUNT_STATUS.name())
                .retryCount(0)
                .status(false)
                .build();
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.UPDATE_ACCOUNT_STATUS)).thenReturn(handler);

        job.runJob();

        verify(handler).rollback(dto);
        verify(failedRollbackService).save(dto);
        assertThat(dto.getStatus()).isTrue();
        assertThat(dto.getRetryCount()).isEqualTo(0);
    }

    /**
     * Test Case ID: TC-FRJOB-006
     * Test Objective: Tăng retryCount khi rollback thất bại.
     * Input: Handler rollback ném RuntimeException.
     * Expected Output: retryCount tăng 1 và save được gọi.
     * Notes: Failure/retry branch.
     */
    @Test
    @DisplayName("runJob: rollback failure increments retryCount and saves")
    void runJob_rollbackFailure_shouldIncreaseRetryAndSave() {
        FailedRollbackDto dto = FailedRollbackDto.builder()
                .id(14L)
                .type(FailedRollbackType.UPDATE_ACCOUNT_STATUS.name())
                .retryCount(3)
                .build();
        when(failedRollbackService.getPendingFailedRollbacks()).thenReturn(List.of(dto));
        when(handlerRegistry.getHandler(FailedRollbackType.UPDATE_ACCOUNT_STATUS)).thenReturn(handler);
        doThrow(new RuntimeException("rollback error")).when(handler).rollback(dto);

        job.runJob();

        verify(handler).rollback(dto);
        verify(failedRollbackService).save(dto);
        assertThat(dto.getRetryCount()).isEqualTo(4);
    }
}

