package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdWorkLocationServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Địa điểm làm việc Quảng cáo Việc làm
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: Rỗng vs có dữ liệu Array lưu DB.
 * ============================================================
 */

import com.cvconnect.dto.jobAd.JobAdWorkLocationDto;
import com.cvconnect.repository.JobAdWorkLocationRepository;
import com.cvconnect.service.impl.JobAdWorkLocationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdWorkLocationServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobAdWorkLocationServiceImplTest {

    @Mock
    private JobAdWorkLocationRepository jobAdWorkLocationRepository;

    @InjectMocks
    private JobAdWorkLocationServiceImpl jobAdWorkLocationService;

    /**
     * Test Case ID: TC-WL-001
     * Test Objective: Validate TC_WL_001_createEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-WL-001: Mảng dto Dto rỗng thì không saveAll để bảo vệ DB")
    void TC_WL_001_createEmpty() {
        jobAdWorkLocationService.create(Collections.emptyList());
        verify(jobAdWorkLocationRepository, never()).saveAll(any());
    }

    /**
     * Test Case ID: TC-WL-002
     * Test Objective: Validate TC_WL_002_createSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-WL-002: Có phần tử thì map rồi saveAll")
    void TC_WL_002_createSuccess() {
        JobAdWorkLocationDto dto = new JobAdWorkLocationDto();
        dto.setJobAdId(1L);

        jobAdWorkLocationService.create(List.of(dto));

        verify(jobAdWorkLocationRepository).saveAll(any());
    }
}


