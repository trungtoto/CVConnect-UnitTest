package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobConfigServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Job Config Mapping
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - getAllJobs: boolean IsActive filter + mapping to DTO logic.
 * ============================================================
 */

import com.cvconnect.dto.common.JobConfigDto;
import com.cvconnect.entity.JobConfig;
import com.cvconnect.repository.JobConfigRepository;
import com.cvconnect.service.impl.JobConfigServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobConfigServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobConfigServiceImplTest {

    @Mock
    private JobConfigRepository jobConfigRepository;

    @InjectMocks
    private JobConfigServiceImpl jobConfigService;

    /**
     * Test Case ID: TC-JC-001
     * Test Objective: Validate TC_JC_001_getAllActiveJobsMap behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-JC-001: Lọc isActive == true và map ra JobConfigDto")
    void TC_JC_001_getAllActiveJobsMap() {
        // Active
        JobConfig c1 = new JobConfig(); 
        c1.setId(1L); 
        c1.setIsActive(true);
        c1.setJobName("Job 1");
        
        // Inactive -> Should filter out branch
        JobConfig c2 = new JobConfig(); 
        c2.setId(2L); 
        c2.setIsActive(false);

        // Null Active -> Should filter out branch via Boolean.TRUE.equals
        JobConfig c3 = new JobConfig();
        c3.setId(3L);
        c3.setIsActive(null);

        when(jobConfigRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        List<JobConfigDto> result = jobConfigService.getAllJobs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getJobName()).isEqualTo("Job 1");
        assertThat(result.get(0).getIsActive()).isTrue();
    }
}


