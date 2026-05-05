package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdCareerServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Ngành nghề Quảng cáo Việc làm
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: Danh sách empty thì bỏ qua.
 *   - create: Danh sách không rỗng thì lưu.
 * ============================================================
 */

import com.cvconnect.dto.jobAd.JobAdCareerDto;
import com.cvconnect.entity.JobAdCareer;
import com.cvconnect.repository.JobAdCareerRepository;
import com.cvconnect.service.impl.JobAdCareerServiceImpl;
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
@DisplayName("JobAdCareerServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobAdCareerServiceImplTest {

    @Mock
    private JobAdCareerRepository jobAdCareerRepository;

    @InjectMocks
    private JobAdCareerServiceImpl jobAdCareerService;

    /**
     * Test Case ID: TC-JAC-001
     * Test Objective: Validate TC_JAC_001_createEmpty behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAC-001: Mảng dto Dto rỗng thì không saveAll để bảo vệ DB")
    void TC_JAC_001_createEmpty() {
        jobAdCareerService.create(Collections.emptyList());

        verify(jobAdCareerRepository, never()).saveAll(any());
    }

    /**
     * Test Case ID: TC-JAC-002
     * Test Objective: Validate TC_JAC_002_createSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JAC-002: Có phần tử thì map rồi saveAll")
    void TC_JAC_002_createSuccess() {
        JobAdCareerDto dto = new JobAdCareerDto();
        dto.setCareerId(1L);

        jobAdCareerService.create(List.of(dto));

        verify(jobAdCareerRepository).saveAll(any());
    }
}


