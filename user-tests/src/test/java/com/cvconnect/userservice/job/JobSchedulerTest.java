package com.cvconnect.userservice.job;

/**
 * ============================================================
 * FILE: JobSchedulerTest.java
 * MODULE: user-service
 * PURPOSE: Unit tests for dynamic scheduler behavior in JobScheduler.
 * ============================================================
 */

import com.cvconnect.dto.common.JobConfigDto;
import com.cvconnect.enums.ScheduleType;
import com.cvconnect.job.JobScheduler;
import com.cvconnect.service.JobConfigService;
import nmquan.commonlib.job.RunningJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobScheduler - Unit Tests")
class JobSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private JobConfigService jobConfigService;

    @Mock
    private RunningJob cronJob;

    @Mock
    private RunningJob fixedRateJob;

    @Mock
    private RunningJob fixedDelayJob;

    private JobScheduler jobScheduler;

    @BeforeEach
    void setUp() {
        when(cronJob.getJobName()).thenReturn("cron-job");
        when(fixedRateJob.getJobName()).thenReturn("fixed-rate-job");
        when(fixedDelayJob.getJobName()).thenReturn("fixed-delay-job");

        jobScheduler = new JobScheduler(List.of(cronJob, fixedRateJob, fixedDelayJob));
        ReflectionTestUtils.setField(jobScheduler, "taskScheduler", taskScheduler);
        ReflectionTestUtils.setField(jobScheduler, "jobConfigService", jobConfigService);
    }

    /**
     * Test Case ID: TC-US-JS-001
     * Test Objective: Skip scheduling when DB job config is empty.
     * Input: getAllJobs returns empty list.
     * Expected Output: no interaction with taskScheduler.
     * Notes: DB read branch with zero configs.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: no config")
    void scheduleJobsFromDb_noConfig_shouldNotScheduleAnything() {
        when(jobConfigService.getAllJobs()).thenReturn(List.of());

        jobScheduler.scheduleJobsFromDb();

        verifyNoInteractions(taskScheduler);
    }

    /**
     * Test Case ID: TC-US-JS-002
     * Test Objective: Skip unknown job names not registered in scheduler.
     * Input: one CRON config with unregistered jobName.
     * Expected Output: no schedule call.
     * Notes: Registry miss branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: skip unknown job")
    void scheduleJobsFromDb_unknownJob_shouldSkip() {
        JobConfigDto config = JobConfigDto.builder()
                .jobName("missing-job")
                .scheduleType(ScheduleType.CRON)
                .expression("0 0/5 * * * *")
                .build();

        when(jobConfigService.getAllJobs()).thenReturn(List.of(config));

        jobScheduler.scheduleJobsFromDb();

        verifyNoInteractions(taskScheduler);
        verify(cronJob, never()).runJob();
    }

    /**
     * Test Case ID: TC-US-JS-003
     * Test Objective: Schedule CRON job correctly.
     * Input: CRON config with valid expression.
     * Expected Output: taskScheduler.schedule called with CronTrigger.
     * Notes: CRON scheduling path.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule CRON")
    void scheduleJobsFromDb_cron_shouldSchedule() {
        JobConfigDto config = JobConfigDto.builder()
                .jobName("cron-job")
                .scheduleType(ScheduleType.CRON)
                .expression("0 0/5 * * * *")
                .build();

        when(jobConfigService.getAllJobs()).thenReturn(List.of(config));

        jobScheduler.scheduleJobsFromDb();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    /**
     * Test Case ID: TC-US-JS-004
     * Test Objective: Schedule FIXED_RATE in milliseconds from seconds expression.
     * Input: FIXED_RATE expression="7".
     * Expected Output: scheduleAtFixedRate called with 7000 ms.
     * Notes: Conversion branch seconds -> milliseconds.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule FIXED_RATE")
    void scheduleJobsFromDb_fixedRate_shouldSchedule() {
        JobConfigDto config = JobConfigDto.builder()
                .jobName("fixed-rate-job")
                .scheduleType(ScheduleType.FIXED_RATE)
                .expression("7")
                .build();

        when(jobConfigService.getAllJobs()).thenReturn(List.of(config));

        jobScheduler.scheduleJobsFromDb();

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(7000L));
    }

    /**
     * Test Case ID: TC-US-JS-005
     * Test Objective: Schedule FIXED_DELAY in milliseconds from seconds expression.
     * Input: FIXED_DELAY expression="9".
     * Expected Output: scheduleWithFixedDelay called with 9000 ms.
     * Notes: Conversion branch seconds -> milliseconds.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule FIXED_DELAY")
    void scheduleJobsFromDb_fixedDelay_shouldSchedule() {
        JobConfigDto config = JobConfigDto.builder()
                .jobName("fixed-delay-job")
                .scheduleType(ScheduleType.FIXED_DELAY)
                .expression("9")
                .build();

        when(jobConfigService.getAllJobs()).thenReturn(List.of(config));

        jobScheduler.scheduleJobsFromDb();

        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(9000L));
    }
}
