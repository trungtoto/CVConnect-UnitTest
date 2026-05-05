package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: FailedRollbackServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Hỗ trợ Rollback Lỗi
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - save: Chuyển đổi và lưu Repository.
 *   - getPendingFailedRollbacks: Empty list / Null trả về rỗng.
 *   - getPendingFailedRollbacks: Có dữ liệu trả về danh sách Dto.
 * ============================================================
 */

import com.cvconnect.dto.failedRollback.FailedRollbackDto;
import com.cvconnect.entity.FailedRollback;
import com.cvconnect.repository.FailedRollbackRepository;
import com.cvconnect.service.impl.FailedRollbackServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedRollbackServiceImpl - Unit Tests (C2 Branch Coverage)")
public class FailedRollbackServiceImplTest {

    @Mock
    private FailedRollbackRepository failedRollbackRepository;

    @InjectMocks
    private FailedRollbackServiceImpl failedRollbackService;

    /**
     * Test Case ID: TC-FRS-001
     * Test Objective: Validate testSave behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-FR-001: Lưu rollback thành công")
    void testSave() {
        FailedRollbackDto dto = new FailedRollbackDto();
        dto.setId(10L);

        failedRollbackService.save(dto);

        verify(failedRollbackRepository).save(any(FailedRollback.class));
    }

    /**
     * Test Case ID: TC-FRS-002
     * Test Objective: Validate testGetPendingEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-FR-002: Lấy danh sách nhưng trả về mảng rỗng (Nhánh Empty)")
    void testGetPendingEmpty() {
        when(failedRollbackRepository.getPendingFailedRollbacks()).thenReturn(null);
        
        List<FailedRollbackDto> result = failedRollbackService.getPendingFailedRollbacks();
        
        assertThat(result).isEmpty();
    }

    /**
     * Test Case ID: TC-FRS-003
     * Test Objective: Validate testGetPendingSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-FR-003: Lấy danh sách có dữ liệu")
    void testGetPendingSuccess() {
        FailedRollback rb = new FailedRollback();
        rb.setId(5L);
        when(failedRollbackRepository.getPendingFailedRollbacks()).thenReturn(List.of(rb));
        
        List<FailedRollbackDto> result = failedRollbackService.getPendingFailedRollbacks();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
    }
}


