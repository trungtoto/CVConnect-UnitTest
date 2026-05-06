package com.cvconnect.userservice.job;

/**
 * ============================================================
 * FILE: JobConfigServiceImplTest.java
 * MODULE: user-service
 * PURPOSE: Unit test for JobConfigServiceImpl mapping and filtering.
 * ============================================================
 */

import com.cvconnect.dto.common.JobConfigDto;
import com.cvconnect.entity.JobConfig;
import com.cvconnect.enums.ScheduleType;
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
@DisplayName("JobConfigServiceImpl - Unit Tests")
class JobConfigServiceImplTest {

    @Mock
    private JobConfigRepository jobConfigRepository;

    @InjectMocks
    private JobConfigServiceImpl jobConfigService;

    /**
     * Test Case ID: TC-US-JC-001
     * Test Objective: Filter active jobs and map to JobConfigDto.
     * Input: active + inactive + null-active job configs.
     * Expected Output: only active item is returned in DTO list.
     * Notes: Branch coverage for Boolean.TRUE.equals filter.
     */
    @Test
    @DisplayName("getAllJobs: filter active configs and map fields")
    void getAllJobs_shouldFilterActiveAndMapFields() {
        JobConfig activeJob = new JobConfig();
        activeJob.setId(1L);
        activeJob.setJobName("invite-reminder");
        activeJob.setDescription("Invite reminder job");
        activeJob.setScheduleType(ScheduleType.CRON);
        activeJob.setExpression("0 0/5 * * * *");
        activeJob.setIsActive(true);

        JobConfig inactiveJob = new JobConfig();
        inactiveJob.setId(2L);
        inactiveJob.setJobName("disabled-job");
        inactiveJob.setIsActive(false);

        JobConfig nullActiveJob = new JobConfig();
        nullActiveJob.setId(3L);
        nullActiveJob.setJobName("null-active-job");
        nullActiveJob.setIsActive(null);

        when(jobConfigRepository.findAll()).thenReturn(List.of(activeJob, inactiveJob, nullActiveJob));

        List<JobConfigDto> result = jobConfigService.getAllJobs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getJobName()).isEqualTo("invite-reminder");
        assertThat(result.get(0).getDescription()).isEqualTo("Invite reminder job");
        assertThat(result.get(0).getScheduleType()).isEqualTo(ScheduleType.CRON);
        assertThat(result.get(0).getExpression()).isEqualTo("0 0/5 * * * *");
    }
}
