package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdStatisticServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Thống kê JobAd
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - addViewStatistic: Chưa có record -> Tạo mới (count = 1).
 *   - addViewStatistic: Đã có record -> Update count + 1.
 * ============================================================
 */

import com.cvconnect.entity.JobAdStatistic;
import com.cvconnect.repository.JobAdStatisticRepository;
import com.cvconnect.service.impl.JobAdStatisticServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdStatisticServiceImpl - Unit Tests (C2 Branch Coverage)")
public class JobAdStatisticServiceImplTest {

    @Mock
    private JobAdStatisticRepository jobAdStatisticRepository;

    @InjectMocks
    private JobAdStatisticServiceImpl jobAdStatisticService;

    /**
     * Test Case ID: TC-STAT-001
     * Test Objective: Validate TC_STAT_001_createNewStatistic behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-STAT-001: Chưa có Statistic, tạo mới với viewCount = 1")
    void TC_STAT_001_createNewStatistic() {
        when(jobAdStatisticRepository.findByJobAdId(50L)).thenReturn(null);

        jobAdStatisticService.addViewStatistic(50L);

        ArgumentCaptor<JobAdStatistic> captor = ArgumentCaptor.forClass(JobAdStatistic.class);
        verify(jobAdStatisticRepository).save(captor.capture());

        JobAdStatistic savedObj = captor.getValue();
        assertThat(savedObj.getJobAdId()).isEqualTo(50L);
        assertThat(savedObj.getViewCount()).isEqualTo(1L);
    }

    /**
     * Test Case ID: TC-STAT-002
     * Test Objective: Validate TC_STAT_002_updateExistingStatistic behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-STAT-002: Đã có Statistic, tăng branch viewCount + 1")
    void TC_STAT_002_updateExistingStatistic() {
        JobAdStatistic existObj = new JobAdStatistic();
        existObj.setJobAdId(50L);
        existObj.setViewCount(10L); // Current 10 views

        when(jobAdStatisticRepository.findByJobAdId(50L)).thenReturn(existObj);

        jobAdStatisticService.addViewStatistic(50L);

        ArgumentCaptor<JobAdStatistic> captor = ArgumentCaptor.forClass(JobAdStatistic.class);
        verify(jobAdStatisticRepository).save(captor.capture());

        JobAdStatistic savedObj = captor.getValue();
        assertThat(savedObj.getViewCount()).isEqualTo(11L); // Branch Update Count
    }
}


