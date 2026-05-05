package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: PositionProcessServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Config Quy Trình Tuyển Dụng chuẩn
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - getByPositionId: Data projection rỗng vs có data. Map builder ProcessType check.
 *   - deleteByIds: mảng null vs có dữ liệu để thực hiện thao tác delete.
 * ============================================================
 */

import com.cvconnect.dto.positionProcess.PositionProcessDto;
import com.cvconnect.dto.positionProcess.PositionProcessProjection;
import com.cvconnect.repository.PositionProcessRepository;
import com.cvconnect.service.impl.PositionProcessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionProcessServiceImpl - Unit Tests (C2 Branch Coverage)")
public class PositionProcessServiceImplTest {

    @Mock
    private PositionProcessRepository positionProcessRepository;

    @InjectMocks
    private PositionProcessServiceImpl positionProcessService;

    /**
     * Test Case ID: TC-PP-001
     * Test Objective: Validate TC_PP_001_getByPositionIdNull behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-PP-001: Trả về ArrayList trống khi null")
    void TC_PP_001_getByPositionIdNull() {
        when(positionProcessRepository.findByPositionId(10L)).thenReturn(null);
        assertThat(positionProcessService.getByPositionId(10L)).isEmpty();
    }

    /**
     * Test Case ID: TC-PP-002
     * Test Objective: Validate TC_PP_002_getByPositionIdMapping behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-PP-002: Ánh xạ PositionProcessProjection")
    void TC_PP_002_getByPositionIdMapping() {
        PositionProcessProjection projection = Mockito.mock(PositionProcessProjection.class);
        when(projection.getId()).thenReturn(5L);
        when(projection.getName()).thenReturn("Process N");
        when(projection.getProcessId()).thenReturn(9L);
        when(projection.getProcessName()).thenReturn("Pro Name");

        when(positionProcessRepository.findByPositionId(10L)).thenReturn(List.of(projection));

        List<PositionProcessDto> result = positionProcessService.getByPositionId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
        assertThat(result.get(0).getProcessType().getName()).isEqualTo("Pro Name"); // ensure wrapper runs
    }

    /**
     * Test Case ID: TC-PP-003
     * Test Objective: Validate TC_PP_003_deleteSafe behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-PP-003: Xoá danh sách bảo vệ mảng null/rỗng")
    void TC_PP_003_deleteSafe() {
        positionProcessService.deleteByIds(null);
        positionProcessService.deleteByIds(Collections.emptyList());

        verify(positionProcessRepository, never()).deleteAllById(any());

        positionProcessService.deleteByIds(List.of(1L, 2L));
        verify(positionProcessRepository).deleteAllById(List.of(1L, 2L));
    }
}


