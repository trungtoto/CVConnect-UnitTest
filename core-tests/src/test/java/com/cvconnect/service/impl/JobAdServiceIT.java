package com.cvconnect.service.impl;

import com.cvconnect.dto.jobAd.JobAdRequest;
import com.cvconnect.entity.JobAd;
import com.cvconnect.entity.Organization;
import com.cvconnect.entity.Position;
import com.cvconnect.entity.Department;
import com.cvconnect.enums.CurrencyType;
import com.cvconnect.enums.JobAdStatus;
import com.cvconnect.enums.JobType;
import com.cvconnect.enums.SalaryType;
import com.cvconnect.repository.JobAdRepository;
import com.cvconnect.repository.OrgRepository;
import com.cvconnect.repository.PositionRepository;
import com.cvconnect.repository.DepartmentRepository;
import com.cvconnect.service.JobAdService;
import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.positionProcess.PositionProcessRequest;
import nmquan.commonlib.dto.response.IDResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ============================================================
 * FILE: JobAdServiceIT.java
 * MODULE: core-service
 * MỤC ĐÍCH: Integration Test cho JobAdService
 * ĐIỀU KIỆN: CheckDB (PostgreSQL) & Tự động Rollback
 * ============================================================
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class JobAdServiceIT {

    @Autowired
    private JobAdService jobAdService;

    @Autowired
    private JobAdRepository jobAdRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private com.cvconnect.repository.ProcessTypeRepository processTypeRepository;

    @MockBean
    private RestTemplateClient restTemplateClient;

    /**
     * Test Case ID: IT-JOB-001
     * Test Objective: Tạo tin tuyển dụng mới và xác nhận lưu DB thành công
     * Input: JobAdRequest với đầy đủ thông tin bắt buộc
     */
    @Test
    @DisplayName("CheckDB: Tạo tin tuyển dụng mới thành công")
    void IT_JOB_001_createJobAd_ShouldSaveToDB() {
        // 1. Seed Data
        Organization org = orgRepository.findAll().stream().findFirst().orElseGet(() -> {
            Organization newOrg = new Organization();
            newOrg.setName("Test Org for JobAd");
            return orgRepository.save(newOrg);
        });

        Department dept = departmentRepository.findAll().stream()
                .filter(d -> d.getOrgId().equals(org.getId()))
                .findFirst().orElseGet(() -> {
                    Department newDept = new Department();
                    newDept.setName("Test Dept");
                    newDept.setOrgId(org.getId());
                    newDept.setCode("D" + System.currentTimeMillis() % 1000);
                    return departmentRepository.save(newDept);
                });

        Position pos = positionRepository.findAll().stream()
                .filter(p -> p.getDepartmentId().equals(dept.getId()))
                .findFirst().orElseGet(() -> {
                    Position newPos = new Position();
                    newPos.setName("Test Pos");
                    newPos.setCode("P" + System.currentTimeMillis() % 1000);
                    newPos.setDepartmentId(dept.getId());
                    return positionRepository.save(newPos);
                });

        JobAdRequest request = JobAdRequest.builder()
                .title("Integration Test Job")
                .orgId(org.getId())
                .positionId(Long.valueOf(pos.getId()))
                .jobType(JobType.FULL_TIME)
                .dueDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .salaryType(SalaryType.RANGE)
                .salaryFrom(2000)
                .salaryTo(4000)
                .currencyType(CurrencyType.VND)
                .hrContactId(5L) // Dùng ID hr1
                .jobAdStatus(JobAdStatus.OPEN)
                .quantity(1)
                .description("Job description")
                .workLocationIds(List.of(1L))
                .positionProcess(List.of(
                        PositionProcessRequest.builder()
                                .processTypeId(1L)
                                .name("Interview Step")
                                .sortOrder(1)
                                .build(),
                        PositionProcessRequest.builder()
                                .processTypeId(processTypeRepository.findAll().stream()
                                        .filter(pt -> pt.getName().equalsIgnoreCase("Onboard"))
                                        .findFirst()
                                        .map(pt -> pt.getId())
                                        .orElse(5L)) // Fallback nếu không tìm thấy
                                .name("Onboard Step")
                                .sortOrder(2)
                                .build()
                ))
                .isAllLevel(true)
                .build();

        // MOCK ĐỂ VƯỢT QUA SECURITY:
        when(restTemplateClient.validOrgMember()).thenReturn(org.getId());
        when(restTemplateClient.checkOrgUserRole(any(), any(), any())).thenReturn(true);
        when(restTemplateClient.getUser(any())).thenReturn(new com.cvconnect.dto.internal.response.UserDto());

        // 2. Act
        IDResponse<Long> response = jobAdService.create(request);

        // 3. Assert
        assertThat(response.getId()).isNotNull();
        Optional<JobAd> dbRecord = Optional.ofNullable(jobAdRepository.findById(response.getId()));
        assertThat(dbRecord).isPresent();
        assertThat(dbRecord.get().getTitle()).isEqualTo("Integration Test Job");
    }

    /**
     * Test Case ID: IT-JOB-002
     * Test Objective: Xóa tin tuyển dụng và kiểm tra DB
     */
    @Test
    @DisplayName("CheckDB: Xóa tin tuyển dụng")
    void IT_JOB_002_deleteJobAd_ShouldRemoveFromDB() {
        JobAd job = new JobAd();
        job.setCode("JOB_DEL_" + System.currentTimeMillis() % 1000);
        job.setTitle("Delete Me");
        job.setOrgId(1L);
        job.setPositionId(1L);
        job.setJobType(JobType.PART_TIME.name());
        job.setDueDate(Instant.now());
        job.setSalaryType(SalaryType.NEGOTIABLE.name());
        job.setCurrencyType(CurrencyType.VND.name());
        job.setHrContactId(5L);
        job.setJobAdStatus(JobAdStatus.OPEN.name());
        job = jobAdRepository.save(job);

        Integer jobId = Math.toIntExact(job.getId());
        jobAdRepository.deleteById(jobId);

        Optional<JobAd> deletedJob = Optional.ofNullable(jobAdRepository.findById(Long.valueOf(jobId)));
        assertThat(deletedJob).isNotPresent();
    }
}
