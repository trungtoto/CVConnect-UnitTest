package com.cvconnect.job;

/**
 * ============================================================
 * FILE: JobSchedulerTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.job.JobScheduler}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - Đọc cấu hình job từ DB
 *   - Schedule theo CRON / FIXED_RATE / FIXED_DELAY
 *   - Bỏ qua job không đăng ký hoặc config không hợp lệ
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
public class JobSchedulerTest {

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
     * Test Case ID: TC-SCHED-001
     * Test Objective: Không schedule khi DB không có cấu hình job.
     * Input: getAllJobs() trả danh sách rỗng.
     * Expected Output: taskScheduler không bị gọi.
     * Notes: Empty config branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: no config from database")
    void scheduleJobsFromDb_noConfig() {
        when(jobConfigService.getAllJobs()).thenReturn(List.of());

        jobScheduler.scheduleJobsFromDb();

        verifyNoInteractions(taskScheduler);
    }

    /**
     * Test Case ID: TC-SCHED-002
     * Test Objective: Bỏ qua config khi jobName không tồn tại trong registry.
     * Input: Một JobConfigDto với jobName không đăng ký.
     * Expected Output: Không schedule và không chạy cronJob.
     * Notes: Unknown job branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: skip config when job is not registered")
    void scheduleJobsFromDb_skipUnknownJob() {
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
     * Test Case ID: TC-SCHED-003
     * Test Objective: Schedule thành công job kiểu CRON.
     * Input: JobConfigDto scheduleType=CRON và expression hợp lệ.
     * Expected Output: taskScheduler.schedule(Runnable, CronTrigger) được gọi.
     * Notes: Cron scheduling branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule cron job")
    void scheduleJobsFromDb_scheduleCron() {
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
     * Test Case ID: TC-SCHED-004
     * Test Objective: Schedule thành công job kiểu FIXED_RATE.
     * Input: scheduleType=FIXED_RATE, expression số giây.
     * Expected Output: scheduleAtFixedRate được gọi với chu kỳ đúng.
     * Notes: Seconds-to-duration branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule fixed-rate job in seconds")
    void scheduleJobsFromDb_scheduleFixedRate() {
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
     * Test Case ID: TC-SCHED-005
     * Test Objective: Schedule thành công job kiểu FIXED_DELAY.
     * Input: scheduleType=FIXED_DELAY, expression số giây.
     * Expected Output: scheduleWithFixedDelay được gá»i với delay đúng.
     * Notes: Fixed-delay scheduling branch.
     */
    @Test
    @DisplayName("scheduleJobsFromDb: schedule fixed-delay job in seconds")
    void scheduleJobsFromDb_scheduleFixedDelay() {
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

