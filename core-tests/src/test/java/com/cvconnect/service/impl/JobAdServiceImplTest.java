package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho JobAdServiceImpl (Tuyển dụng)
 *
 * BAO PHỦ CÁC LUỒNG:
 *   - create (Validation lương, ngày hạn, lưu relationships)
 *   - update (Valid email auto send, update thong tin)
 *   - filterForOrg, detailOutside
 *   - getJobAdOrgDetail
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.common.NotificationDto;
import com.cvconnect.dto.internal.response.EmailConfigDto;
import com.cvconnect.dto.internal.response.EmailTemplateDto;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAd.*;
import com.cvconnect.dto.processType.ProcessTypeDto;
import com.cvconnect.entity.JobAd;
import com.cvconnect.enums.*;
import nmquan.commonlib.dto.request.FilterRequest;
import nmquan.commonlib.dto.response.FilterResponse;
import com.cvconnect.dto.jobAd.JobAdProjection;
import com.cvconnect.dto.jobAd.JobAdOrgFilterProjection;
import com.cvconnect.repository.JobAdRepository;
import nmquan.commonlib.utils.ObjectMapperUtils;
import com.cvconnect.service.*;
import com.cvconnect.service.impl.JobAdServiceImpl;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.utils.KafkaUtils;
import com.cvconnect.utils.CoreServiceUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdServiceImpl - Unit Tests")
public class JobAdServiceImplTest {

    @Mock private JobAdRepository jobAdRepository;
    @Mock private JobAdProcessService jobAdProcessService;
    @Mock private JobAdCareerService jobAdCareerService;
    @Mock private JobAdWorkLocationService jobAdWorkLocationService;
    @Mock private OrgAddressService orgAddressService;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private KafkaUtils kafkaUtils;
    @Mock private JobAdLevelService jobAdLevelService;
    @Mock private JobAdStatisticService jobAdStatisticService;
    @Mock private ProcessTypeService processTypeService;
    @Mock private LevelService levelService;
    @Mock private PositionService positionService;
    @Mock private DepartmentService departmentService;
    @Mock private CareerService careerService;
    @Mock private OrgService orgService;
    @Mock private SearchHistoryOutsideService searchHistoryOutsideService;

    @InjectMocks
    private JobAdServiceImpl jobAdService;

    private JobAdRequest genValidRequest() {
        JobAdRequest req = new JobAdRequest();
        req.setTitle("Test Title");
        req.setPositionId(10L);
        req.setJobType(JobType.FULL_TIME);
        req.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
        req.setQuantity(5);
        req.setSalaryType(SalaryType.NEGOTIABLE);
        req.setCurrencyType(CurrencyType.VND);
        req.setKeyword("Java, Spring");
        req.setHrContactId(99L);
        req.setJobAdStatus(JobAdStatus.OPEN);
        req.setAutoSendEmail(false);
        req.setPublic(true);
        req.setIsAllLevel(true);

        // Bat buoc phai co positionProcess (it nhat 2 phan tu: dau APPLY, cuoi ONBOARD)
        com.cvconnect.dto.positionProcess.PositionProcessRequest applyProc = new com.cvconnect.dto.positionProcess.PositionProcessRequest();
        applyProc.setProcessTypeId(1L);
        com.cvconnect.dto.positionProcess.PositionProcessRequest onboardProc = new com.cvconnect.dto.positionProcess.PositionProcessRequest();
        onboardProc.setProcessTypeId(2L);
        req.setPositionProcess(java.util.List.of(applyProc, onboardProc));
        return req;
    }

    @Nested
    @DisplayName("1. create() validations")
    class CreateJobAdTest {
        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            lenient().when(restTemplateClient.validOrgMember()).thenReturn(1L);
            lenient().when(restTemplateClient.checkOrgUserRole(anyLong(), anyString(), anyLong())).thenReturn(true);
            lenient().when(jobAdRepository.existsByOrgIdAndPositionId(anyLong(), anyLong())).thenReturn(true);
        }

        /**
         * Test Case ID: TC-JOBAD-001
         * Test Objective: Validate TC_JOBAD_001_createJobAd_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-001: Tạo Job thành công")
        void TC_JOBAD_001_createJobAd_Success() {
            JobAdRequest req = genValidRequest();
            Long userId = 2L;
            lenient().when(levelService.getByIds(anyList())).thenReturn(List.of(new com.cvconnect.dto.level.LevelDto()));
            
            // Mock validations
            when(processTypeService.detail(1L)).thenReturn(ProcessTypeDto.builder().code("APPLY").name("Apply").build());
            when(processTypeService.detail(2L)).thenReturn(ProcessTypeDto.builder().code("ONBOARD").name("Onboard").build());
            
            // Mock repository/service calls
            when(jobAdRepository.save(any(JobAd.class))).thenAnswer(i -> {
                JobAd ja = i.getArgument(0); ja.setId(100L); return ja;
            });

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(userId);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentFullName).thenReturn("HR Name");

                IDResponse<Long> resp = jobAdService.create(req);

                assertThat(resp.getId()).isEqualTo(100L);
                verify(jobAdProcessService).create(anyList());
                verify(kafkaUtils).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
            }
        }

        /**
         * Test Case ID: TC-JOBAD-002
         * Test Objective: Validate TC_JOBAD_009_createJob_FailApplyFirst behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-009: Lỗi tạo Job vì quy trình không bắt đầu bằng APPLY")
        void TC_JOBAD_009_createJob_FailApplyFirst() {
            JobAdRequest req = genValidRequest();
            req.getPositionProcess().get(0).setProcessTypeId(99L);
            when(processTypeService.detail(99L)).thenReturn(ProcessTypeDto.builder().code("INTERVIEW").name("Interview").build());

            assertThatThrownBy(() -> jobAdService.create(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.FIRST_PROCESS_MUST_BE_APPLY));
        }

        /**
         * Test Case ID: TC-JOBAD-003
         * Test Objective: Validate TC_JOBAD_010_createJob_FailOnboardLast behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-010: Lỗi tạo Job vì quy trình không kết thúc bằng ONBOARD")
        void TC_JOBAD_010_createJob_FailOnboardLast() {
            JobAdRequest req = genValidRequest();
            req.getPositionProcess().get(1).setProcessTypeId(88L);
            when(processTypeService.detail(1L)).thenReturn(ProcessTypeDto.builder().code("APPLY").name("Apply").build());
            when(processTypeService.detail(88L)).thenReturn(ProcessTypeDto.builder().code("EXAM").name("Exam").build());

            assertThatThrownBy(() -> jobAdService.create(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.LAST_PROCESS_MUST_BE_ONBOARD));
        }

        /**
         * Test Case ID: TC-JOBAD-004
         * Test Objective: Validate TC_JOBAD_011_createJob_FailLevelRequired behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-011: Lỗi tạo Job vì thiếu Level (khi không chọn All Level)")
        void TC_JOBAD_011_createJob_FailLevelRequired() {
            JobAdRequest req = genValidRequest();
            req.setIsAllLevel(false);
            req.setLevelIds(null); // List rỗng/null
            
            when(processTypeService.detail(1L)).thenReturn(ProcessTypeDto.builder().code("APPLY").name("Apply").build());
            when(processTypeService.detail(2L)).thenReturn(ProcessTypeDto.builder().code("ONBOARD").name("Onboard").build());

            assertThatThrownBy(() -> jobAdService.create(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.LEVEL_REQUIRED));
        }

        /**
         * Test Case ID: TC-JOBAD-005
         * Test Objective: Validate TC_JOBAD_002_createFail_OrgPositionNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-002: Thất bại do Org và Position không hợp lệ (Branch Missing Position)")
        void TC_JOBAD_002_createFail_OrgPositionNotFound() {
            when(jobAdRepository.existsByOrgIdAndPositionId(anyLong(), anyLong())).thenReturn(false);

            JobAdRequest req = genValidRequest();
            assertThatThrownBy(() -> jobAdService.create(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ORG_POSITION_LEVEL_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-JOBAD-006
         * Test Objective: Validate TC_JOBAD_003_createFail_DueDatePast behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-003: Lỗi do hạn chót đặt trong quá khứ (Branch DueDate Before Now)")
        void TC_JOBAD_003_createFail_DueDatePast() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdRepository.existsByOrgIdAndPositionId(1L, 10L)).thenReturn(true);

            JobAdRequest req = genValidRequest();
            req.setDueDate(Instant.now().minus(1, ChronoUnit.DAYS));

            assertThatThrownBy(() -> jobAdService.create(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DUE_DATE_MUST_BE_IN_FUTURE));
        }

        /**
         * Test Case ID: TC-JOBAD-007
         * Test Objective: Validate TC_JOBAD_004_createFail_RangeSalaryInvalid behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-004: Lỗi RANGE Salary nhập lộn From lớn hơn To (C2 Branch Check Salary)")
        void TC_JOBAD_004_createFail_RangeSalaryInvalid() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdRepository.existsByOrgIdAndPositionId(1L, 10L)).thenReturn(true);

            JobAdRequest req = genValidRequest();
            req.setSalaryType(SalaryType.RANGE);
            req.setSalaryFrom(500);
            req.setSalaryTo(100); // from > to => error

            assertThatThrownBy(() -> jobAdService.create(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.SALARY_FROM_TO_INVALID));
        }

        /**
         * Test Case ID: TC-JOBAD-008
         * Test Objective: Validate TC_JOBAD_005_createSuccess_AutoSendEmailAndPrivate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-005: Thành công tạo tin có Email Config & Private Code (Branch Email Valid & Private)")
        void TC_JOBAD_005_createSuccess_AutoSendEmailAndPrivate() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdRepository.existsByOrgIdAndPositionId(1L, 10L)).thenReturn(true);
            when(restTemplateClient.checkOrgUserRole(99L, Constants.RoleCode.HR, 1L)).thenReturn(true);
            when(jobAdRepository.getSuffixCodeMax(any(), anyString())).thenReturn(10L);
            // Mock processType validation
            com.cvconnect.dto.processType.ProcessTypeDto applyType = new com.cvconnect.dto.processType.ProcessTypeDto();
            applyType.setCode(com.cvconnect.enums.ProcessTypeEnum.APPLY.name());
            com.cvconnect.dto.processType.ProcessTypeDto onboardType = new com.cvconnect.dto.processType.ProcessTypeDto();
            onboardType.setCode(com.cvconnect.enums.ProcessTypeEnum.ONBOARD.name());
            when(processTypeService.detail(1L)).thenReturn(applyType);
            when(processTypeService.detail(2L)).thenReturn(onboardType);

            JobAdRequest req = genValidRequest();
            req.setPublic(false);
            req.setAutoSendEmail(true);
            req.setEmailTemplateId(55L);

            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new EmailConfigDto());
            EmailTemplateDto etd = new EmailTemplateDto(); etd.setId(55L); etd.setIsActive(true);
            when(restTemplateClient.getEmailTemplateByOrgId(1L)).thenReturn(List.of(etd));

            jobAdService.create(req);
            verify(jobAdRepository).save(argThat(ja -> ja.getKeyCodeInternal() != null)); // mã internal code phải được sinh ra
        }
    }

    @Nested
    @DisplayName("2. query() findById & getJobAdOrgDetail")
    class QueryJobAdTest {
        /**
         * Test Case ID: TC-JOBAD-009
         * Test Objective: Validate TC_JOBAD_006_findById behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_JOBAD_006_findById() {
            JobAd ja = new JobAd(); ja.setId(10L);
            when(jobAdRepository.findById(10L)).thenReturn(ja);
            JobAdDto dto = jobAdService.findById(10L);
            assertThat(dto.getId()).isEqualTo(10L);
        }

        /**
         * Test Case ID: TC-JOBAD-010
         * Test Objective: Validate TC_JOBAD_007_updateJobAdStatus_FailNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_JOBAD_007_updateJobAdStatus_FailNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdRepository.findById(1L)).thenReturn(null);

            JobAdStatusRequest req = new JobAdStatusRequest();
            req.setJobAdId(1L); req.setStatus(JobAdStatus.PAUSE);
            
            assertThatThrownBy(() -> jobAdService.updateJobAdStatus(req))
                .isInstanceOf(AppException.class);
        }

        /**
         * Test Case ID: TC-JOBAD-011
         * Test Objective: Validate TC_JOBAD_008_updateJobAdStatus_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-008: Thành công update status sang PAUSE (Lưu jobAd)")
        void TC_JOBAD_008_updateJobAdStatus_Success() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd(); ja.setId(10L); ja.setOrgId(1L); ja.setJobAdStatus("OPEN");
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdStatusRequest req = new JobAdStatusRequest();
            req.setJobAdId(10L); req.setStatus(JobAdStatus.PAUSE);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(1L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                jobAdService.updateJobAdStatus(req);
                verify(jobAdRepository).save(ja);
                assertThat(ja.getJobAdStatus()).isEqualTo("PAUSE");
            }
        }
    }

    @Nested
    @DisplayName("3. update() JobAd")
    class UpdateJobAdTest {

        /**
         * Test Case ID: TC-JOBAD-012
         * Test Objective: Validate TC_JOBAD_009_updateJobAdSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-009: Cập nhật thành công thông tin tin tuyển dụng")
        void TC_JOBAD_009_updateJobAdSuccess() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd(); ja.setId(10L); ja.setOrgId(1L); ja.setDueDate(Instant.now().minus(1, ChronoUnit.DAYS));
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdUpdateRequest req = new JobAdUpdateRequest();
            req.setId(10L);
            req.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS)); // Ngày mới
            req.setIsAutoSendEmail(false); 
            req.setTitle("Updated Title");

            jobAdService.update(req);
            verify(jobAdRepository).save(ja);
            assertThat(ja.getTitle()).isEqualTo("Updated Title");
        }

        /**
         * Test Case ID: TC-JOBAD-013
         * Test Objective: Validate TC_JOBAD_010_updateJobAdFail_EmailConfigNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-010: Báo lỗi khi cập nhật Email auto = true nhưng cấu hình Email rỗng")
        void TC_JOBAD_010_updateJobAdFail_EmailConfigNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd(); ja.setId(10L); ja.setOrgId(1L); ja.setDueDate(Instant.now().minus(1, ChronoUnit.DAYS));
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdUpdateRequest req = new JobAdUpdateRequest();
            req.setId(10L);
            req.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS)); 
            req.setIsAutoSendEmail(true); 
            req.setEmailTemplateId(99L);

            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(null);

            assertThatThrownBy(() -> jobAdService.update(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.EMAIL_CONFIG_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-JOBAD-014
         * Test Objective: Validate TC_JOBAD_010B_updateJobAdFail_IsAutoSendEmailRequired behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-010B: Báo lỗi khi thiếu cờ isAutoSendEmail")
        void TC_JOBAD_010B_updateJobAdFail_IsAutoSendEmailRequired() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS));
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdUpdateRequest req = new JobAdUpdateRequest();
            req.setId(10L);
            req.setDueDate(ja.getDueDate());
            req.setIsAutoSendEmail(null);

            assertThatThrownBy(() -> jobAdService.update(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.IS_AUTO_SEND_EMAIL_REQUIRED));
        }

        /**
         * Test Case ID: TC-JOBAD-015
         * Test Objective: Validate TC_JOBAD_010C_updateJobAdFail_EmailTemplateIdRequired behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-010C: Báo lỗi khi bật auto email nhưng thiếu emailTemplateId")
        void TC_JOBAD_010C_updateJobAdFail_EmailTemplateIdRequired() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS));
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdUpdateRequest req = new JobAdUpdateRequest();
            req.setId(10L);
            req.setDueDate(ja.getDueDate());
            req.setIsAutoSendEmail(true);
            req.setEmailTemplateId(null);

            assertThatThrownBy(() -> jobAdService.update(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.EMAIL_TEMPLATE_ID_REQUIRED));
        }

        /**
         * Test Case ID: TC-JOBAD-016
         * Test Objective: Validate TC_JOBAD_010D_updateJobAdFail_EmailTemplateNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-010D: Báo lỗi khi email template không tồn tại hoặc không active")
        void TC_JOBAD_010D_updateJobAdFail_EmailTemplateNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS));
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdUpdateRequest req = new JobAdUpdateRequest();
            req.setId(10L);
            req.setDueDate(ja.getDueDate());
            req.setIsAutoSendEmail(true);
            req.setEmailTemplateId(999L);

            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new EmailConfigDto());
            EmailTemplateDto inactiveTemplate = new EmailTemplateDto();
            inactiveTemplate.setId(999L);
            inactiveTemplate.setIsActive(false);
            when(restTemplateClient.getEmailTemplateByOrgId(1L)).thenReturn(List.of(inactiveTemplate));

            assertThatThrownBy(() -> jobAdService.update(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.EMAIL_TEMPLATE_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-JOBAD-017
         * Test Objective: Validate TC_JOBAD_012_updateStatus_SuccessAdmin behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-012: Cập nhật status thành công (Admin)")
        void TC_JOBAD_012_updateStatus_SuccessAdmin() {
            JobAd ja = new JobAd(); ja.setId(10L); ja.setOrgId(1L); ja.setJobAdStatus("OPEN");
            ja.setHrContactId(99L);
            when(jobAdRepository.findById(10L)).thenReturn(ja);
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(1L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                
                JobAdStatusRequest req = new JobAdStatusRequest(); req.setJobAdId(10L); req.setStatus(JobAdStatus.PAUSE); // OPEN -> PAUSE is level 1 -> 1
                jobAdService.updateJobAdStatus(req);
                
                assertThat(ja.getJobAdStatus()).isEqualTo("PAUSE");
                verify(jobAdRepository).save(ja);
            }
        }

        /**
         * Test Case ID: TC-JOBAD-018
         * Test Objective: Validate TC_JOBAD_011_updateJobAdStatus_FailRevert behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-011: Thất bại khi revert status thấp hơn")
        void TC_JOBAD_011_updateJobAdStatus_FailRevert() {
            JobAd ja = new JobAd(); ja.setId(10L); ja.setOrgId(1L); ja.setJobAdStatus("CLOSED"); // Level 2
            when(jobAdRepository.findById(10L)).thenReturn(ja);
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(1L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                
                JobAdStatusRequest req = new JobAdStatusRequest(); req.setJobAdId(10L); req.setStatus(JobAdStatus.OPEN); // Level 1 < 2
                assertThatThrownBy(() -> jobAdService.updateJobAdStatus(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.JOB_AD_STATUS_CANNOT_REVERT));
            }
        }
    }

    @Nested
    @DisplayName("4. Public Recruitment Logic (Outside)")
    class PublicRecruitmentTest {
        /**
         * Test Case ID: TC-JOBAD-019
         * Test Objective: Validate TC_JOBAD_012_filterOutside_LocationLogic behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-012: filterJobAdsForOutside() với xử lý Location (Remote vs Province)")
        void TC_JOBAD_012_filterOutside_LocationLogic() {
            JobAdOutsideFilterRequest req = new JobAdOutsideFilterRequest();
            req.setKeyword("Developer");
            
            // Mock Projections
            JobAdProjection p = mock(JobAdProjection.class);
            lenient().when(p.getId()).thenReturn(100L);
            lenient().when(jobAdRepository.filterJobAdsForOutsideFunction(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyLong(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(List.of(p));

            // Mock Working Location Statistics [jobId, isRemote, province]
            Object[] row1 = new Object[]{100L, true, "Hanoi"};
            Object[] row2 = new Object[]{101L, false, "Hanoi"};
            Object[] row3 = new Object[]{102L, false, "HCM "}; 
            lenient().when(jobAdRepository.getWorkingLocationByFilterFunction(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(List.of(row1, row2, row3));

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class);
                 org.mockito.MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> utils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(88L);
                
                com.cvconnect.dto.jobAd.JobAdDto d1 = new com.cvconnect.dto.jobAd.JobAdDto();
                d1.setId(100L); d1.setJobType(JobType.FULL_TIME.name()); d1.setSalaryType(SalaryType.RANGE.name());
                d1.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
                utils.when(() -> nmquan.commonlib.utils.ObjectMapperUtils.convertToList(any(), eq(com.cvconnect.dto.jobAd.JobAdDto.class)))
                     .thenReturn(List.of(d1));

                JobAdOutsideFilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterJobAdsForOutside(req);
                
                assertThat(resp.getLocations()).hasSize(3); // Hanoi, HCM, Remote
                verify(searchHistoryOutsideService).create(any());
            }
        }

        /**
         * Test Case ID: TC-JOBAD-020
         * Test Objective: Validate TC_JOBAD_013_detailOutside_AccessControl behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-013: detailOutside() logic cho Job nội bộ và công khai")
        void TC_JOBAD_013_detailOutside_AccessControl() {
            JobAd ja = new JobAd(); ja.setId(55L); ja.setIsPublic(false); ja.setKeyCodeInternal("SECRET");
            ja.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
            ja.setSalaryType(SalaryType.RANGE.name()); 
            ja.setCurrencyType(CurrencyType.VND.name());
            ja.setJobType(JobType.FULL_TIME.name());
            when(jobAdRepository.findById(55L)).thenReturn(ja);
            
            // Fail without key
            assertThatThrownBy(() -> jobAdService.detailOutside(55L, null))
                .isInstanceOf(AppException.class);
            
            // Success with key (Mock ObjectMapper conversion)
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> utils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                com.cvconnect.dto.jobAd.JobAdDto dto = new com.cvconnect.dto.jobAd.JobAdDto();
                dto.setId(55L); dto.setDueDate(ja.getDueDate()); dto.setSalaryType(ja.getSalaryType());
                dto.setJobType(ja.getJobType()); dto.setJobAdStatus("OPEN");
                dto.setPositionId(99L); dto.setOrgId(88L);
                
                // Match specifically the entity we have
                utils.when(() -> nmquan.commonlib.utils.ObjectMapperUtils.convertToObject(org.mockito.ArgumentMatchers.any(Object.class), eq(com.cvconnect.dto.jobAd.JobAdDto.class)))
                     .thenReturn(dto);

                // Correct way to mock multiple static classes
                try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                    webUtils.when(() -> nmquan.commonlib.utils.WebUtils.getCurrentUserId()).thenReturn(1L);

                    JobAdOutsideDetailResponse resp = jobAdService.detailOutside(55L, "SECRET");
                    assertThat(resp).isNotNull();
                    verify(jobAdStatisticService).addViewStatistic(55L);
                }
            }
        }

        /**
         * Test Case ID: TC-JOBAD-021
         * Test Objective: Validate TC_JOBAD_014_filterFeatured_Pagination behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-014: filterFeaturedOutside() phân trang & mapping")
        void TC_JOBAD_014_filterFeatured_Pagination() {
            JobAdProjection p = mock(JobAdProjection.class);
            when(p.getTotalElement()).thenReturn(100L);
            when(jobAdRepository.getFeaturedJobAds(anyInt(), anyLong())).thenReturn(List.of(p));
            
            // Mock ObjectMapperUtils to prevent NPE during mapping
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> utils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                com.cvconnect.dto.jobAd.JobAdDto dtoWithDate = new com.cvconnect.dto.jobAd.JobAdDto();
                dtoWithDate.setId(100L); dtoWithDate.setOrgId(1L); dtoWithDate.setPositionId(10L);
                dtoWithDate.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
                dtoWithDate.setSalaryType(SalaryType.RANGE.name());
                dtoWithDate.setJobType(JobType.FULL_TIME.name());
                dtoWithDate.setIsRemote(true);
                
                utils.when(() -> nmquan.commonlib.utils.ObjectMapperUtils.convertToList(any(), eq(com.cvconnect.dto.jobAd.JobAdDto.class)))
                     .thenReturn(List.of(dtoWithDate));

                FilterRequest req = new FilterRequest(); req.setPageIndex(0); req.setPageSize(10);
                FilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterFeaturedOutside(req);
                
                assertThat(resp.getPageInfo().getTotalElements()).isEqualTo(100L);
            }
        }
    }

    @Nested
    @DisplayName("5. Additional branch expansion for JobAdServiceImpl")
    class AdditionalCoverageTest {

        /**
         * Test Case ID: TC-JOBAD-022
         * Test Objective: Validate TC_JOBAD_015_updatePublicStatus_CreateKeyInternal behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-015: updatePublicStatus() tạo key internal khi chuyển private")
        void TC_JOBAD_015_updatePublicStatus_CreateKeyInternal() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setHrContactId(99L);
            ja.setIsPublic(true);
            ja.setKeyCodeInternal(null);
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdPublicStatusRequest req = new JobAdPublicStatusRequest();
            req.setJobAdId(10L);
            req.setIsPublic(false);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                jobAdService.updatePublicStatus(req);

                verify(jobAdRepository).save(ja);
                assertThat(ja.getIsPublic()).isFalse();
                assertThat(ja.getKeyCodeInternal()).isNotBlank();
            }
        }

        /**
         * Test Case ID: TC-JOBAD-023
         * Test Objective: Validate TC_JOBAD_016_updatePublicStatus_NoChange behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-016: updatePublicStatus() không đổi trạng thái thì không save")
        void TC_JOBAD_016_updatePublicStatus_NoChange() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setHrContactId(99L);
            ja.setIsPublic(true);
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdPublicStatusRequest req = new JobAdPublicStatusRequest();
            req.setJobAdId(10L);
            req.setIsPublic(true);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                jobAdService.updatePublicStatus(req);
                verify(jobAdRepository, never()).save(any(JobAd.class));
            }
        }

        /**
         * Test Case ID: TC-JOBAD-024
         * Test Objective: Validate TC_JOBAD_017_getJobAdOrgDetail_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-017: getJobAdOrgDetail() trả về đầy đủ dữ liệu")
        void TC_JOBAD_017_getJobAdOrgDetail_Success() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setCode("JOB-001");
            ja.setTitle("Backend Developer");
            ja.setPositionId(5L);
            ja.setDueDate(Instant.now().plus(3, ChronoUnit.DAYS));
            ja.setQuantity(3);
            ja.setHrContactId(99L);
            ja.setJobAdStatus(JobAdStatus.OPEN.name());
            ja.setIsPublic(true);
            ja.setJobType(JobType.FULL_TIME.name());
            ja.setSalaryType(SalaryType.NEGOTIABLE.name());
            ja.setCurrencyType(CurrencyType.VND.name());
            ja.setIsAllLevel(false);
            ja.setOrgId(1L);
            when(jobAdRepository.getJobAdOrgDetailById(10L, 1L, null)).thenReturn(ja);

            when(positionService.findById(5L)).thenReturn(com.cvconnect.dto.position.PositionDto.builder().id(5L).name("Java").departmentId(null).build());
            when(restTemplateClient.getUser(99L)).thenReturn(UserDto.builder().id(99L).fullName("HR Name").build());
            when(jobAdProcessService.getJobAdProcessByJobAdIds(List.of(10L))).thenReturn(java.util.Map.of(10L, List.of()));
            when(orgAddressService.getByJobAdId(10L)).thenReturn(List.of());
            when(levelService.getLevelsByJobAdId(10L)).thenReturn(List.of());

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                JobAdOrgDetailResponse response = jobAdService.getJobAdOrgDetail(10L);
                assertThat(response.getId()).isEqualTo(10L);
                assertThat(response.getPosition().getName()).isEqualTo("Java");
                assertThat(response.getHrContact().getFullName()).isEqualTo("HR Name");
                assertThat(response.getJobAdStatus().getName()).isEqualTo(JobAdStatus.OPEN.name());
            }
        }

        /**
         * Test Case ID: TC-JOBAD-025
         * Test Objective: Validate TC_JOBAD_018_getJobAdOrgDetail_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-018: getJobAdOrgDetail() fail khi không tìm thấy")
        void TC_JOBAD_018_getJobAdOrgDetail_NotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdRepository.getJobAdOrgDetailById(10L, 1L, null)).thenReturn(null);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                assertThatThrownBy(() -> jobAdService.getJobAdOrgDetail(10L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-JOBAD-026
         * Test Objective: Validate TC_JOBAD_019_filterJobAdsForOrg_HRRole behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-019: filterJobAdsForOrg() với role HR dùng participantId")
        void TC_JOBAD_019_filterJobAdsForOrg_HRRole() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdOrgFilterProjection p = mock(JobAdOrgFilterProjection.class);
            lenient().when(p.getId()).thenReturn(10L);
            lenient().when(p.getCode()).thenReturn("JOB-10");
            lenient().when(p.getTitle()).thenReturn("Title");
            lenient().when(p.getPositionId()).thenReturn(5L);
            lenient().when(p.getPositionName()).thenReturn("Java");
            lenient().when(p.getDepartmentId()).thenReturn(2L);
            lenient().when(p.getDepartmentName()).thenReturn("IT");
            lenient().when(p.getDueDate()).thenReturn(Instant.now().plus(2, ChronoUnit.DAYS));
            lenient().when(p.getQuantity()).thenReturn(2);
            lenient().when(p.getHrContactId()).thenReturn(99L);
            lenient().when(p.getJobAdStatus()).thenReturn(JobAdStatus.OPEN.name());
            lenient().when(p.getIsPublic()).thenReturn(true);

            org.springframework.data.domain.Page<JobAdOrgFilterProjection> page =
                new org.springframework.data.domain.PageImpl<>(List.of(p));
            when(jobAdRepository.filterJobAdsForOrg(any(), any(), eq(99L))).thenReturn(page);
            when(restTemplateClient.getUsersByIds(anyList())).thenReturn(java.util.Map.of(99L, UserDto.builder().id(99L).fullName("HR").build()));
            when(jobAdProcessService.getJobAdProcessByJobAdIds(List.of(10L))).thenReturn(java.util.Map.of(10L, List.of()));

            JobAdOrgFilterRequest req = new JobAdOrgFilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.HR));
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                FilterResponse<JobAdOrgDetailResponse> response = jobAdService.filterJobAdsForOrg(req);
                assertThat(response.getData()).hasSize(1);
                verify(jobAdRepository).filterJobAdsForOrg(any(), any(), eq(99L));
            }
        }

        /**
         * Test Case ID: TC-JOBAD-027
         * Test Objective: Validate TC_JOBAD_019B_filterJobAdsForOrg_AdminRole behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-019B: filterJobAdsForOrg() với role ORG_ADMIN dùng participantId null")
        void TC_JOBAD_019B_filterJobAdsForOrg_AdminRole() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdOrgFilterProjection p = mock(JobAdOrgFilterProjection.class);
            lenient().when(p.getId()).thenReturn(10L);
            lenient().when(p.getCode()).thenReturn("JOB-10");
            lenient().when(p.getTitle()).thenReturn("Title");
            lenient().when(p.getPositionId()).thenReturn(5L);
            lenient().when(p.getPositionName()).thenReturn("Java");
            lenient().when(p.getDepartmentId()).thenReturn(2L);
            lenient().when(p.getDepartmentName()).thenReturn("IT");
            lenient().when(p.getDueDate()).thenReturn(Instant.now().plus(2, ChronoUnit.DAYS));
            lenient().when(p.getQuantity()).thenReturn(2);
            lenient().when(p.getHrContactId()).thenReturn(99L);
            lenient().when(p.getJobAdStatus()).thenReturn(JobAdStatus.OPEN.name());
            lenient().when(p.getIsPublic()).thenReturn(true);

            org.springframework.data.domain.Page<JobAdOrgFilterProjection> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(p));
            when(jobAdRepository.filterJobAdsForOrg(any(), any(), isNull())).thenReturn(page);
            when(restTemplateClient.getUsersByIds(anyList())).thenReturn(java.util.Map.of(99L, UserDto.builder().id(99L).fullName("Admin HR").build()));
            when(jobAdProcessService.getJobAdProcessByJobAdIds(List.of(10L))).thenReturn(java.util.Map.of(10L, List.of()));

            JobAdOrgFilterRequest req = new JobAdOrgFilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                FilterResponse<JobAdOrgDetailResponse> response = jobAdService.filterJobAdsForOrg(req);
                assertThat(response.getData()).hasSize(1);
                verify(jobAdRepository).filterJobAdsForOrg(any(), any(), isNull());
            }
        }

        /**
         * Test Case ID: TC-JOBAD-028
         * Test Objective: Validate TC_JOBAD_020_findByProcessAndGetProcessList behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-020: findByJobAdProcessId() và getProcessByJobAdId()")
        void TC_JOBAD_020_findByProcessAndGetProcessList() {
            JobAd ja = new JobAd();
            ja.setId(100L);
            when(jobAdRepository.findByJobAdProcessId(200L)).thenReturn(ja);
            when(jobAdProcessService.getByJobAdId(100L)).thenReturn(List.of(JobAdProcessDto.builder().id(1L).build()));

            JobAdDto dto = jobAdService.findByJobAdProcessId(200L);
            List<JobAdProcessDto> processList = jobAdService.getProcessByJobAdId(100L);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(100L);
            assertThat(processList).hasSize(1);
        }

        /**
         * Test Case ID: TC-JOBAD-029
         * Test Objective: Validate TC_JOBAD_020B_findByProcess_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-020B: findByJobAdProcessId() trả null khi không tìm thấy")
        void TC_JOBAD_020B_findByProcess_NotFound() {
            when(jobAdRepository.findByJobAdProcessId(999L)).thenReturn(null);

            JobAdDto dto = jobAdService.findByJobAdProcessId(999L);

            assertThat(dto).isNull();
        }

        /**
         * Test Case ID: TC-JOBAD-030
         * Test Objective: Validate TC_JOBAD_021_getJobAdsByParticipantId_HRRole behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-021: getJobAdsByParticipantId() role HR")
        void TC_JOBAD_021_getJobAdsByParticipantId_HRRole() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setCode("JOB-10");
            ja.setTitle("Title");

            org.springframework.data.domain.Page<JobAd> page = new org.springframework.data.domain.PageImpl<>(List.of(ja));
            when(jobAdRepository.getJobAdsByParticipantId(eq(1L), eq(77L), any())).thenReturn(page);

            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.HR));
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(77L);

                FilterResponse<JobAdDto> response = jobAdService.getJobAdsByParticipantId(req);
                assertThat(response.getData()).hasSize(1);
                verify(jobAdRepository).getJobAdsByParticipantId(eq(1L), eq(77L), any());
            }
        }

        /**
         * Test Case ID: TC-JOBAD-031
         * Test Objective: Validate TC_JOBAD_021B_getJobAdsByParticipantId_AdminRole behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-021B: getJobAdsByParticipantId() role ORG_ADMIN dùng participantId null")
        void TC_JOBAD_021B_getJobAdsByParticipantId_AdminRole() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(20L);
            ja.setCode("JOB-20");
            ja.setTitle("Admin Title");

            org.springframework.data.domain.Page<JobAd> page = new org.springframework.data.domain.PageImpl<>(List.of(ja));
            when(jobAdRepository.getJobAdsByParticipantId(eq(1L), isNull(), any())).thenReturn(page);

            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                FilterResponse<JobAdDto> response = jobAdService.getJobAdsByParticipantId(req);
                assertThat(response.getData()).hasSize(1);
                verify(jobAdRepository).getJobAdsByParticipantId(eq(1L), isNull(), any());
            }
        }

        /**
         * Test Case ID: TC-JOBAD-032
         * Test Objective: Validate TC_JOBAD_022_outsideDataFilter_and_updateStatusByOrgIds behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-022: outsideDataFilter() và updateJobAdStatusByOrgIds()")
        void TC_JOBAD_022_outsideDataFilter_and_updateStatusByOrgIds() {
            FilterResponse<com.cvconnect.dto.career.CareerDto> careerResp = new FilterResponse<>();
            careerResp.setData(List.of(com.cvconnect.dto.career.CareerDto.builder().id(1L).name("IT").build()));
            when(careerService.filter(any())).thenReturn(careerResp);

            FilterResponse<com.cvconnect.dto.level.LevelDto> levelResp = new FilterResponse<>();
            levelResp.setData(List.of(com.cvconnect.dto.level.LevelDto.builder().id(1L).levelName("Junior").build()));
            when(levelService.filter(any())).thenReturn(levelResp);

            JobAdOutsideDataFilter dataFilter = jobAdService.outsideDataFilter();
            assertThat(dataFilter.getCareers()).hasSize(1);
            assertThat(dataFilter.getLevels()).hasSize(1);
            assertThat(dataFilter.getJobTypes()).isNotEmpty();

            jobAdService.updateJobAdStatusByOrgIds(List.of(1L, 2L), false);
            verify(jobAdRepository).updateJobAdStatusByOrgIds(List.of(1L, 2L), JobAdStatus.PAUSE.name());

            jobAdService.updateJobAdStatusByOrgIds(List.of(1L, 2L), true);
            verify(jobAdRepository, times(1)).updateJobAdStatusByOrgIds(List.of(1L, 2L), JobAdStatus.PAUSE.name());
        }

        /**
         * Test Case ID: TC-JOBAD-033
         * Test Objective: Validate TC_JOBAD_023_filterSuitableOutside_UserNullAndSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-023: filterSuitableOutside() xử lý nhánh user null và success")
        void TC_JOBAD_023_filterSuitableOutside_UserNullAndSuccess() {
            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(null);
                FilterResponse<JobAdOutsideDetailResponse> emptyResp = jobAdService.filterSuitableOutside(req);
                assertThat(emptyResp.getData()).isEmpty();
            }

            JobAdProjection p = mock(JobAdProjection.class);
            when(p.getTotalElement()).thenReturn(1L);
            when(jobAdRepository.getSuitableJobAds(eq("java"), eq(10), eq(0L))).thenReturn(List.of(p));

            when(searchHistoryOutsideService.getMySearchHistoryOutside()).thenReturn(List.of(
                com.cvconnect.dto.searchHistoryOutside.SearchHistoryOutsideDto.builder().keyword("java").build()
            ));

            com.cvconnect.dto.position.PositionDto pos = com.cvconnect.dto.position.PositionDto.builder().id(5L).name("Java").build();
            com.cvconnect.dto.org.OrgDto org = com.cvconnect.dto.org.OrgDto.builder().id(1L).name("Org A").build();
            when(positionService.getPositionMapByIds(List.of(5L))).thenReturn(java.util.Map.of(5L, pos));
            when(orgAddressService.getOrgAddressByJobAdIds(List.of(100L))).thenReturn(java.util.Map.of(100L, List.of()));
            when(levelService.getLevelsMapByJobAdIds(List.of(100L))).thenReturn(java.util.Map.of(100L, List.of()));
            when(orgService.getOrgMapByIds(List.of(1L))).thenReturn(java.util.Map.of(1L, org));

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class);
                 MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> utils = mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(88L);

                JobAdDto dto = new JobAdDto();
                dto.setId(100L);
                dto.setOrgId(1L);
                dto.setPositionId(5L);
                dto.setDueDate(Instant.now().plus(3, ChronoUnit.DAYS));
                dto.setJobType(JobType.FULL_TIME.name());
                dto.setSalaryType(SalaryType.NEGOTIABLE.name());
                dto.setIsRemote(false);
                dto.setKeyword("java;spring");
                dto.setIsAllLevel(true);
                utils.when(() -> ObjectMapperUtils.convertToList(any(), eq(JobAdDto.class))).thenReturn(List.of(dto));

                FilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterSuitableOutside(req);
                assertThat(resp.getData()).hasSize(1);
                assertThat(resp.getPageInfo().getTotalElements()).isEqualTo(1L);
            }
        }

        /**
         * Test Case ID: TC-JOBAD-034
         * Test Objective: Validate TC_JOBAD_023B_filterSuitableOutside_KeywordEmpty behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-023B: filterSuitableOutside() trả rỗng khi keyword lịch sử trống")
        void TC_JOBAD_023B_filterSuitableOutside_KeywordEmpty() {
            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            when(searchHistoryOutsideService.getMySearchHistoryOutside()).thenReturn(java.util.Collections.emptyList());

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(88L);

                FilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterSuitableOutside(req);
                assertThat(resp.getData()).isEmpty();
                assertThat(resp.getPageInfo().getTotalElements()).isEqualTo(0L);
            }
        }

        /**
         * Test Case ID: TC-JOBAD-035
         * Test Objective: Validate TC_JOBAD_023C_filterSuitableOutside_ProjectionEmpty behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-023C: filterSuitableOutside() trả rỗng khi suitable projections rỗng")
        void TC_JOBAD_023C_filterSuitableOutside_ProjectionEmpty() {
            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            when(searchHistoryOutsideService.getMySearchHistoryOutside()).thenReturn(List.of(
                    com.cvconnect.dto.searchHistoryOutside.SearchHistoryOutsideDto.builder().keyword("java").build()
            ));
            when(jobAdRepository.getSuitableJobAds(eq("java"), eq(10), eq(0L))).thenReturn(java.util.Collections.emptyList());

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(88L);

                FilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterSuitableOutside(req);
                assertThat(resp.getData()).isEmpty();
                assertThat(resp.getPageInfo().getTotalElements()).isEqualTo(0L);
            }
        }

        /**
         * Test Case ID: TC-JOBAD-036
         * Test Objective: Validate TC_JOBAD_023D_filterFeaturedOutside_EmptyProjection behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-023D: filterFeaturedOutside() trả rỗng khi không có featured jobs")
        void TC_JOBAD_023D_filterFeaturedOutside_EmptyProjection() {
            when(jobAdRepository.getFeaturedJobAds(eq(10), eq(0L))).thenReturn(java.util.Collections.emptyList());

            FilterRequest req = new FilterRequest();
            req.setPageIndex(0);
            req.setPageSize(10);

            FilterResponse<JobAdOutsideDetailResponse> resp = jobAdService.filterFeaturedOutside(req);

            assertThat(resp.getData()).isEmpty();
            assertThat(resp.getPageInfo().getTotalElements()).isEqualTo(0L);
        }

        /**
         * Test Case ID: TC-JOBAD-037
         * Test Objective: Validate TC_JOBAD_023E_updatePublicStatus_FailNotOwner behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-023E: updatePublicStatus() lỗi với non-admin không phải HR contact")
        void TC_JOBAD_023E_updatePublicStatus_FailNotOwner() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAd ja = new JobAd();
            ja.setId(10L);
            ja.setOrgId(1L);
            ja.setHrContactId(99L);
            ja.setIsPublic(true);
            when(jobAdRepository.findById(10L)).thenReturn(ja);

            JobAdPublicStatusRequest req = new JobAdPublicStatusRequest();
            req.setJobAdId(10L);
            req.setIsPublic(false);

            try (MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(55L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.HR));

                assertThatThrownBy(() -> jobAdService.updatePublicStatus(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-JOBAD-038
         * Test Objective: Validate TC_JOBAD_024_listRelateOutside_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-JOBAD-024: listRelateOutside() fail khi jobAd không tồn tại")
        void TC_JOBAD_024_listRelateOutside_NotFound() {
            when(jobAdRepository.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> jobAdService.listRelateOutside(999L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
        }
    }
}


