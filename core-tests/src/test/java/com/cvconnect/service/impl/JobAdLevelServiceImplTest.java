package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdLevelServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Cấp bậc Quảng cáo Việc làm
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: SaveAll trực tiếp.
 * ============================================================
 */

import com.cvconnect.dto.jobAdLevel.JobAdLevelDto;
import com.cvconnect.repository.JobAdLevelRepository;
import com.cvconnect.service.impl.JobAdLevelServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdLevelServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobAdLevelServiceImplTest {

    @Mock
    private JobAdLevelRepository jobAdLevelRepository;

    @InjectMocks
    private JobAdLevelServiceImpl jobAdLevelService;

    /**
     * Test Case ID: TC-JAL-001
     * Test Objective: Validate TC_JAL_001_create behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAL-001: Lưu danh sách list level thành công")
    void TC_JAL_001_create() {
        JobAdLevelDto dto = new JobAdLevelDto();
        dto.setLevelId(1L);

        jobAdLevelService.create(List.of(dto));

        verify(jobAdLevelRepository).saveAll(any());
    }
}


