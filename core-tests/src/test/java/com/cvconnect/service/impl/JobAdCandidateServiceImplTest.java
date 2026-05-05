package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: JobAdCandidateServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho quy trình xử lý ứng viên (Apply, Eliminate, Onboard)
 *
 * BAO PHỦ CÁC LUỒNG:
 *   - apply (Có file đính kèm, Gửi thông báo Kafka)
 *   - changeCandidateProcess
 *   - eliminateCandidate
 *   - markOnboard
 * ============================================================
 */

import com.cvconnect.common.ReplacePlaceholder;
import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto;
import com.cvconnect.dto.common.NotificationDto;
import com.cvconnect.dto.internal.response.EmailTemplateDto;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAd.JobAdDto;
import com.cvconnect.dto.jobAd.JobAdProcessDto;
import com.cvconnect.dto.jobAdCandidate.ApplyRequest;
import com.cvconnect.dto.jobAdCandidate.EliminateCandidateRequest;
import com.cvconnect.dto.processType.ProcessTypeDto;
import com.cvconnect.entity.JobAdCandidate;
import com.cvconnect.enums.CandidateStatus;
import com.cvconnect.enums.EliminateReasonEnum;
import com.cvconnect.enums.ProcessTypeEnum;
import com.cvconnect.repository.JobAdCandidateRepository;
import com.cvconnect.service.*;
import com.cvconnect.service.impl.JobAdCandidateServiceImpl;
import nmquan.commonlib.service.SendEmailService;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyProjection;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoDetail;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import nmquan.commonlib.utils.KafkaUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAdCandidateServiceImpl - Unit Tests")
public class JobAdCandidateServiceImplTest {

    @Mock private JobAdCandidateRepository jobAdCandidateRepository;
    @Mock private AttachFileService attachFileService;
    @Mock private CandidateInfoApplyService candidateInfoApplyService;
    @Mock private JobAdProcessService jobAdProcessService;
    @Mock private JobAdProcessCandidateService jobAdProcessCandidateService;
    @Mock private JobAdService jobAdService;
    @Mock private SendEmailService sendEmailService;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private ReplacePlaceholder replacePlaceholder;
    @Mock private KafkaUtils kafkaUtils;
    @Mock private OrgService orgService;

    @InjectMocks
    private JobAdCandidateServiceImpl candidateService;

    @Nested
    @DisplayName("1. apply() Nộp hồ sơ ứng tuyển")
    class ApplyTest {

        /**
         * Test Case ID: TC-CANDIDATE-001
         * Test Objective: Validate TC_CANDIDATE_001_applySuccess_uploadFile behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001: Nộp CV thành công (Tự động tải CV lên hệ thống & gửi Notify)")
        void TC_CANDIDATE_001_applySuccess_uploadFile() {
            // Mock JobAd (phai co orgId va dueDate trong tuong lai)
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L); mockJobAd.setOrgId(1L); mockJobAd.setTitle("Software Engineer");
            mockJobAd.setIsAutoSendEmail(false); mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            // Mock Upload File (File PDF giả)
            MultipartFile mockFile = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "CV Content".getBytes());
            when(attachFileService.uploadFile(any())).thenReturn(List.of(100L));
            
            // Mock Candidate Info
            when(candidateInfoApplyService.create(anyList())).thenReturn(List.of(200L));
            CandidateInfoApplyDto infoApplyDto = new CandidateInfoApplyDto();
            infoApplyDto.setId(200L); infoApplyDto.setFullName("Nguyen Van A");
            when(candidateInfoApplyService.getById(200L)).thenReturn(infoApplyDto);

            // Mock Workflow Process (Khởi chạy bước APPLY)
            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setFullName("Nguyen Van A");
            req.setEmail("test@gmail.com");
            req.setPhone("0123456789");

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                // candidateId khác orgId của job (1L) để không bị bắt lỗi "cannot apply own org"
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                IDResponse<Long> resp = candidateService.apply(req, mockFile);
                
                // Verify
                verify(jobAdCandidateRepository).save(any(JobAdCandidate.class));
                verify(jobAdProcessCandidateService).create(anyList());
                verify(kafkaUtils).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-002
         * Test Objective: Validate TC_CANDIDATE_001B_applySuccess_UseExistingCandidateInfo behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001B: Nộp thành công với candidateInfoApplyId có sẵn (không upload lại CV)")
        void TC_CANDIDATE_001B_applySuccess_UseExistingCandidateInfo() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L);
            mockJobAd.setOrgId(1L);
            mockJobAd.setTitle("Software Engineer");
            mockJobAd.setIsAutoSendEmail(false);
            mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            CandidateInfoApplyDto existingInfo = new CandidateInfoApplyDto();
            existingInfo.setId(300L);
            existingInfo.setCandidateId(99L);
            existingInfo.setFullName("Candidate Existing");
            existingInfo.setEmail("existing@test.com");
            when(candidateInfoApplyService.getById(300L)).thenReturn(existingInfo);

            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setCandidateInfoApplyId(300L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                IDResponse<Long> resp = candidateService.apply(req, null);

                assertThat(resp).isNotNull();
                verify(attachFileService, never()).uploadFile(any());
                verify(candidateInfoApplyService, never()).create(anyList());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-003
         * Test Objective: Validate TC_CANDIDATE_001C_applySuccess_AutoEmailTemplateNull behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001C: Auto email bật nhưng email template null thì bỏ qua gửi mail")
        void TC_CANDIDATE_001C_applySuccess_AutoEmailTemplateNull() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L);
            mockJobAd.setOrgId(1L);
            mockJobAd.setTitle("Software Engineer");
            mockJobAd.setIsAutoSendEmail(true);
            mockJobAd.setEmailTemplateId(5L);
            mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            MultipartFile mockFile = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "CV Content".getBytes());
            when(attachFileService.uploadFile(any())).thenReturn(List.of(100L));
            when(candidateInfoApplyService.create(anyList())).thenReturn(List.of(200L));

            CandidateInfoApplyDto infoApplyDto = new CandidateInfoApplyDto();
            infoApplyDto.setId(200L);
            infoApplyDto.setFullName("Nguyen Van A");
            infoApplyDto.setEmail("candidate@test.com");
            when(candidateInfoApplyService.getById(200L)).thenReturn(infoApplyDto);

            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(null);

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setFullName("Nguyen Van A");
            req.setEmail("candidate@test.com");
            req.setPhone("0123456789");

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                IDResponse<Long> resp = candidateService.apply(req, mockFile);

                assertThat(resp).isNotNull();
                verify(sendEmailService, never()).sendEmailWithBody(any());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-004
         * Test Objective: Validate TC_CANDIDATE_001D_applyFail_AutoEmailMissingHrUser behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001D: Auto email bật nhưng thiếu HR user thì ném CommonErrorCode.ERROR")
        void TC_CANDIDATE_001D_applyFail_AutoEmailMissingHrUser() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L);
            mockJobAd.setOrgId(1L);
            mockJobAd.setTitle("Software Engineer");
            mockJobAd.setIsAutoSendEmail(true);
            mockJobAd.setEmailTemplateId(5L);
            mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            MultipartFile mockFile = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "CV Content".getBytes());
            when(attachFileService.uploadFile(any())).thenReturn(List.of(100L));
            when(candidateInfoApplyService.create(anyList())).thenReturn(List.of(200L));

            CandidateInfoApplyDto infoApplyDto = new CandidateInfoApplyDto();
            infoApplyDto.setId(200L);
            infoApplyDto.setFullName("Nguyen Van A");
            infoApplyDto.setEmail("candidate@test.com");
            when(candidateInfoApplyService.getById(200L)).thenReturn(infoApplyDto);

            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
            emailTemplateDto.setId(5L);
            emailTemplateDto.setSubject("Apply");
            emailTemplateDto.setBody("Hello");
            emailTemplateDto.setPlaceholderCodes(List.of());
            when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(emailTemplateDto);
            when(restTemplateClient.getUser(99L)).thenReturn(null);

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setFullName("Nguyen Van A");
            req.setEmail("candidate@test.com");
            req.setPhone("0123456789");

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                assertThatThrownBy(() -> candidateService.apply(req, mockFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ERROR));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-005
         * Test Objective: Validate TC_CANDIDATE_001E_applySuccess_AutoEmailSendSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001E: Auto email bật và gửi mail thành công")
        void TC_CANDIDATE_001E_applySuccess_AutoEmailSendSuccess() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L);
            mockJobAd.setOrgId(1L);
            mockJobAd.setTitle("Software Engineer");
            mockJobAd.setPositionId(3L);
            mockJobAd.setIsAutoSendEmail(true);
            mockJobAd.setEmailTemplateId(5L);
            mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            MultipartFile mockFile = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "CV Content".getBytes());
            when(attachFileService.uploadFile(any())).thenReturn(List.of(100L));
            when(candidateInfoApplyService.create(anyList())).thenReturn(List.of(200L));

            CandidateInfoApplyDto infoApplyDto = new CandidateInfoApplyDto();
            infoApplyDto.setId(200L);
            infoApplyDto.setFullName("Nguyen Van A");
            infoApplyDto.setEmail("candidate@test.com");
            when(candidateInfoApplyService.getById(200L)).thenReturn(infoApplyDto);

            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
            emailTemplateDto.setId(5L);
            emailTemplateDto.setSubject("Apply");
            emailTemplateDto.setBody("Hello {{candidate_name}}");
            emailTemplateDto.setPlaceholderCodes(List.of("candidate_name"));
            when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(emailTemplateDto);

            UserDto hr = new UserDto();
            hr.setId(99L);
            hr.setFullName("HR A");
            hr.setEmail("hr@test.com");
            hr.setPhoneNumber("0900000000");
            when(restTemplateClient.getUser(99L)).thenReturn(hr);
            when(replacePlaceholder.replacePlaceholder(anyString(), anyList(), any())).thenReturn("Hello candidate");

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setFullName("Nguyen Van A");
            req.setEmail("candidate@test.com");
            req.setPhone("0123456789");

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                IDResponse<Long> resp = candidateService.apply(req, mockFile);

                assertThat(resp).isNotNull();
                verify(sendEmailService).sendEmailWithBody(any());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-006
         * Test Objective: Validate TC_CANDIDATE_001F_applyFail_AutoEmailMissingCandidateInfo behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-001F: Auto email bật nhưng candidateInfoApply null thì ném CommonErrorCode.ERROR")
        void TC_CANDIDATE_001F_applyFail_AutoEmailMissingCandidateInfo() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L);
            mockJobAd.setOrgId(1L);
            mockJobAd.setTitle("Software Engineer");
            mockJobAd.setIsAutoSendEmail(true);
            mockJobAd.setEmailTemplateId(5L);
            mockJobAd.setHrContactId(99L);
            mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            MultipartFile mockFile = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "CV Content".getBytes());
            when(attachFileService.uploadFile(any())).thenReturn(List.of(100L));
            when(candidateInfoApplyService.create(anyList())).thenReturn(List.of(200L));
            when(candidateInfoApplyService.getById(200L)).thenReturn(null);

            JobAdProcessDto processDto = new JobAdProcessDto();
            processDto.setId(5L);
            ProcessTypeDto processType = new ProcessTypeDto();
            processType.setCode(ProcessTypeEnum.APPLY.name());
            processDto.setProcessType(processType);
            when(jobAdProcessService.getByJobAdId(10L)).thenReturn(List.of(processDto));

            EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
            emailTemplateDto.setId(5L);
            emailTemplateDto.setSubject("Apply");
            emailTemplateDto.setBody("Hello");
            emailTemplateDto.setPlaceholderCodes(List.of());
            when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(emailTemplateDto);

            UserDto hr = new UserDto();
            hr.setId(99L);
            hr.setFullName("HR A");
            hr.setEmail("hr@test.com");
            when(restTemplateClient.getUser(99L)).thenReturn(hr);

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setFullName("Nguyen Van A");
            req.setEmail("candidate@test.com");
            req.setPhone("0123456789");

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(999L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of());

                assertThatThrownBy(() -> candidateService.apply(req, mockFile))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ERROR));

                verify(sendEmailService, never()).sendEmailWithBody(any());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-007
         * Test Objective: Validate TC_CANDIDATE_002_applyFail_InvalidFile behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-002: Lỗi nộp CV vì file rỗng/sai định dạng")
        void TC_CANDIDATE_002_applyFail_InvalidFile() {
            ApplyRequest req = new ApplyRequest(); req.setJobAdId(10L);
            MultipartFile emptyFile = new MockMultipartFile("cv", new byte[0]);

            assertThatThrownBy(() -> candidateService.apply(req, emptyFile))
                .isInstanceOf(AppException.class);
        }

        /**
         * Test Case ID: TC-CANDIDATE-008
         * Test Objective: Validate TC_CANDIDATE_005_applyFail_OwnOrg behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-005: Lỗi nộp CV cho chính công ty của mình (Own Org)")
        void TC_CANDIDATE_005_applyFail_OwnOrg() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L); mockJobAd.setOrgId(1L);
            lenient().when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            ApplyRequest req = new ApplyRequest(); req.setJobAdId(10L);
            req.setFullName("Candidate A"); req.setEmail("a@gmail.com"); req.setPhone("0987654321");

            MultipartFile validFile = new org.springframework.mock.web.MockMultipartFile("cv", "cv.pdf", "application/pdf", "dummy content".getBytes());
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(1L); // Trùng cty
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                assertThatThrownBy(() -> candidateService.apply(req, validFile))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.CANNOT_APPLY_OWN_ORG_JOB_AD));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-009
         * Test Objective: Validate TC_CANDIDATE_006_applyFail_Expired behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-006: Lỗi nộp CV vì Job đã hết hạn")
        void TC_CANDIDATE_006_applyFail_Expired() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L); mockJobAd.setOrgId(1L);
            mockJobAd.setDueDate(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
            lenient().when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            ApplyRequest req = new ApplyRequest(); req.setJobAdId(10L);
            req.setFullName("A"); req.setEmail("a@g.com"); req.setPhone("1");
            MultipartFile validFile = new org.springframework.mock.web.MockMultipartFile("cv", "cv.pdf", "application/pdf", "dummy content".getBytes());
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(2L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                assertThatThrownBy(() -> candidateService.apply(req, validFile))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.JOB_AD_EXPIRED));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-010
         * Test Objective: Validate TC_CANDIDATE_007_applyFail_NotOpen behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-007: Lỗi nộp CV vì tin đã dừng tuyển dụng (PAUSED)")
        void TC_CANDIDATE_007_applyFail_NotOpen() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L); mockJobAd.setOrgId(1L); mockJobAd.setJobAdStatus("PAUSE");
            mockJobAd.setDueDate(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            lenient().when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            ApplyRequest req = new ApplyRequest(); req.setJobAdId(10L);
            req.setFullName("A"); req.setEmail("a@g.com"); req.setPhone("1");
            MultipartFile validFile = new org.springframework.mock.web.MockMultipartFile("cv", "cv.pdf", "application/pdf", "dummy content".getBytes());
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(2L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                assertThatThrownBy(() -> candidateService.apply(req, validFile))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.JOB_AD_STOP_RECRUITMENT));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-011
         * Test Objective: Validate TC_CANDIDATE_008_applyFail_Duplicate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-008: Lỗi nộp CV vì đã nộp trước đó rồi (Duplicate)")
        void TC_CANDIDATE_008_applyFail_Duplicate() {
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setId(10L); mockJobAd.setOrgId(1L); mockJobAd.setJobAdStatus("OPEN");
            mockJobAd.setDueDate(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            lenient().when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            ApplyRequest req = new ApplyRequest(); req.setJobAdId(10L);
            req.setFullName("Candidate A"); req.setEmail("a@gmail.com"); req.setPhone("0987654321");
            
            lenient().when(jobAdCandidateRepository.existsByJobAdIdAndCandidateId(anyLong(), anyLong())).thenReturn(true);

            MultipartFile validFile = new org.springframework.mock.web.MockMultipartFile("cv", "cv.pdf", "application/pdf", "dummy content".getBytes());
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(2L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                assertThatThrownBy(() -> candidateService.apply(req, validFile))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_DUPLICATE_APPLY));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-012
         * Test Objective: Validate TC_CANDIDATE_008B_applyFail_CandidateInfoNotBelongToCurrentCandidate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-008B: Lỗi nộp CV khi candidateInfoApplyId không thuộc candidate hiện tại")
        void TC_CANDIDATE_008B_applyFail_CandidateInfoNotBelongToCurrentCandidate() {
            CandidateInfoApplyDto existingInfo = new CandidateInfoApplyDto();
            existingInfo.setId(300L);
            existingInfo.setCandidateId(100L);
            when(candidateInfoApplyService.getById(300L)).thenReturn(existingInfo);

            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setCandidateInfoApplyId(300L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(2L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                assertThatThrownBy(() -> candidateService.apply(req, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                        .isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_INFO_APPLY_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-013
         * Test Objective: Validate TC_CANDIDATE_008C_applyFail_CurrentUserNullWhenUsingExistingCandidateInfo behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-008C: Lỗi nộp CV khi current user null với candidateInfoApplyId có sẵn")
        void TC_CANDIDATE_008C_applyFail_CurrentUserNullWhenUsingExistingCandidateInfo() {
            ApplyRequest req = new ApplyRequest();
            req.setJobAdId(10L);
            req.setCandidateInfoApplyId(300L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentOrgId).thenReturn(2L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(null);

                assertThatThrownBy(() -> candidateService.apply(req, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                        .isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("2. eliminateCandidate() Thất bại/Loại ứng viên")
    class EliminateTest {

        /**
         * Test Case ID: TC-CANDIDATE-014
         * Test Objective: Validate TC_CANDIDATE_003_eliminateSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-003: Đánh rớt ứng viên thành công")
        void TC_CANDIDATE_003_eliminateSuccess() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L); jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            jac.setJobAdId(10L); jac.setCandidateInfoId(20L);
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));

            // Thong tin jobAd can co title
            JobAdDto mockJobAd = new JobAdDto();
            mockJobAd.setTitle("Software Engineer");
            when(jobAdService.findById(10L)).thenReturn(mockJobAd);

            // candidateId phai co gia tri de List.of() khong NPE
            CandidateInfoApplyDto candidateInfo = new CandidateInfoApplyDto();
            candidateInfo.setId(20L); candidateInfo.setFullName("Nguyen Van A"); candidateInfo.setCandidateId(99L);
            when(candidateInfoApplyService.getById(anyLong())).thenReturn(candidateInfo);

            // Notify org admin
            UserDto hrUser = new UserDto(); hrUser.setId(1L); hrUser.setFullName("HR Manager");
            when(restTemplateClient.getUser(anyLong())).thenReturn(hrUser);
            when(restTemplateClient.getUserByRoleCodeOrg(eq(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN), eq(1L))).thenReturn(List.of());

            EliminateCandidateRequest request = new EliminateCandidateRequest();
            request.setJobAdCandidateId(55L);
            request.setReason(EliminateReasonEnum.SKILL_MISMATCH);
            request.setSendEmail(false);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(1L);
                // checkAuthorizedChangeProcess: ORG_ADMIN branch
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                candidateService.eliminateCandidate(request);

                verify(jobAdCandidateRepository).save(jac);
                assertThat(jac.getCandidateStatus()).isEqualTo("REJECTED");
                assertThat(jac.getEliminateReasonType()).isEqualTo("SKILL_MISMATCH");
                verify(kafkaUtils, times(2)).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
            }
        }
    }

    @Nested
    @DisplayName("3. changeCandidateProcess() & markOnboard()")
    class WorkflowTest {

        /**
         * Test Case ID: TC-CANDIDATE-015
         * Test Objective: Validate TC_CANDIDATE_004_markOnboardSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-004: Đánh dấu Onboard thành công")
        void TC_CANDIDATE_004_markOnboardSuccess() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            
            JobAdCandidate jac = new JobAdCandidate(); jac.setId(77L); jac.setCandidateStatus(CandidateStatus.WAITING_ONBOARDING.name());
            when(jobAdCandidateRepository.findById(77L)).thenReturn(Optional.of(jac));
            when(jobAdProcessCandidateService.validateCurrentProcessTypeIs(77L, ProcessTypeEnum.ONBOARD.name())).thenReturn(true);
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrgAndNotJobAdCandidate(any(), eq(1L), eq(77L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);

            // Action
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                // checkAuthorizedChangeProcess: ORG_ADMIN branch
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(77L, 1L)).thenReturn(true);
                
                com.cvconnect.dto.jobAdCandidate.MarkOnboardRequest req = new com.cvconnect.dto.jobAdCandidate.MarkOnboardRequest();
                req.setJobAdCandidateId(77L); 
                req.setIsOnboarded(true);

                candidateService.markOnboard(req);
                
                verify(jobAdCandidateRepository).save(jac);
                assertThat(jac.getCandidateStatus()).isEqualTo("ONBOARDED");
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-016
         * Test Objective: Validate TC_CANDIDATE_004B_markNotOnboardSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-004B: Đánh dấu Not Onboarded thành công")
        void TC_CANDIDATE_004B_markNotOnboardSuccess() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(78L);
            jac.setCandidateStatus(CandidateStatus.WAITING_ONBOARDING.name());
            jac.setCandidateInfoId(20L);
            when(jobAdCandidateRepository.findById(78L)).thenReturn(Optional.of(jac));
            when(jobAdProcessCandidateService.validateCurrentProcessTypeIs(78L, ProcessTypeEnum.ONBOARD.name())).thenReturn(true);
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrgAndNotJobAdCandidate(any(), eq(1L), eq(78L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(78L, 1L)).thenReturn(true);

                com.cvconnect.dto.jobAdCandidate.MarkOnboardRequest req = new com.cvconnect.dto.jobAdCandidate.MarkOnboardRequest();
                req.setJobAdCandidateId(78L);
                req.setIsOnboarded(false);

                candidateService.markOnboard(req);

                verify(jobAdCandidateRepository).save(jac);
                assertThat(jac.getCandidateStatus()).isEqualTo("NOT_ONBOARDED");
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-017
         * Test Objective: Validate TC_CANDIDATE_009_changeCandidateProcess_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-009: Chuyển bước quy trình thành công (Phát email manual)")
        void TC_CANDIDATE_009_changeCandidateProcess_Success() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);
            req.setSendEmail(true);
            req.setSubject("Manual Subject"); req.setTemplate("Hello {{candidate_name}}");
            
            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L); toProc.setJobAdCandidateId(55L); toProc.setJobAdProcessId(200L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            JobAdCandidate jac = new JobAdCandidate(); jac.setId(55L); jac.setJobAdId(10L); jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(true);
            
            JobAdProcessDto jap = new JobAdProcessDto(); jap.setId(200L); jap.setName("Interview 1");
            jap.setProcessType(ProcessTypeDto.builder().code("INTERVIEW").name("Interview").build());
            when(jobAdProcessService.getById(200L)).thenReturn(jap);
            
            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto currentProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            currentProc.setProcessName("Apply");
            when(jobAdProcessCandidateService.getCurrentProcess(10L, 20L)).thenReturn(currentProc);

            // Notify/Email mocks
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            when(candidateInfoApplyService.getById(20L)).thenReturn(CandidateInfoApplyDto.builder().id(20L).fullName("Van A").email("a@g.com").candidateId(99L).build());
            when(restTemplateClient.getUser(99L)).thenReturn(UserDto.builder().id(99L).fullName("HR").email("hr@c.com").build());
            when(jobAdService.findById(10L)).thenReturn(new JobAdDto());
            
            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                candidateService.changeCandidateProcess(req);
                
                verify(jobAdCandidateRepository).save(jac);
                verify(sendEmailService).sendEmailWithBody(any());
                verify(kafkaUtils).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any());
            }
        }
        
        /**
         * Test Case ID: TC-CANDIDATE-018
         * Test Objective: Validate TC_CANDIDATE_010_changeProcess_FailAlreadyOnboarded behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-010: Chuyển bước quy trình thất bại do đã Onboarded")
        void TC_CANDIDATE_010_changeProcess_FailAlreadyOnboarded() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(101L);
            
            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(101L); toProc.setJobAdCandidateId(55L);
            when(jobAdProcessCandidateService.findById(101L)).thenReturn(toProc);

            JobAdCandidate jac = new JobAdCandidate(); jac.setId(55L); jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(eq(20L), eq(1L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(true);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_ALREADY_ONBOARDED));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-019
         * Test Objective: Validate TC_CANDIDATE_011_changeProcess_FailMissingOnboardDate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-011: Chuyển bước quy trình thất bại do thiếu ngày Onboard")
        void TC_CANDIDATE_011_changeProcess_FailMissingOnboardDate() {
             com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);
            req.setOnboardDate(null); // Missing
            
            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setJobAdCandidateId(55L); toProc.setJobAdProcessId(200L);
            when(jobAdProcessCandidateService.findById(anyLong())).thenReturn(toProc);

            JobAdCandidate jac = new JobAdCandidate(); jac.setId(55L); jac.setJobAdId(1L); jac.setCandidateInfoId(2L); jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdProcessCandidateService.validateProcessOrderChange(anyLong(), anyLong())).thenReturn(true);
            lenient().when(jobAdProcessCandidateService.getCurrentProcess(anyLong(), anyLong())).thenReturn(new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto());
            
            JobAdProcessDto jap = new JobAdProcessDto(); jap.setProcessType(ProcessTypeDto.builder().code(ProcessTypeEnum.ONBOARD.name()).build());
            when(jobAdProcessService.getById(200L)).thenReturn(jap);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(anyLong(), anyLong())).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.ONBOARD_DATE_REQUIRED));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-020
         * Test Objective: Validate TC_CANDIDATE_014_changeProcess_FailProcessNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-014: Chuyển bước thất bại khi process đích không tồn tại")
        void TC_CANDIDATE_014_changeProcess_FailProcessNotFound() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(999L);

            when(jobAdProcessCandidateService.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.PROCESS_TYPE_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-021
         * Test Objective: Validate TC_CANDIDATE_015_changeProcess_FailAlreadyEliminated behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-015: Chuyển bước thất bại khi ứng viên đã bị loại")
        void TC_CANDIDATE_015_changeProcess_FailAlreadyEliminated() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setCandidateStatus(CandidateStatus.REJECTED.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_ALREADY_ELIMINATED));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-022
         * Test Objective: Validate TC_CANDIDATE_016_changeProcess_FailInvalidOrder behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-016: Chuyển bước thất bại khi thứ tự quy trình không hợp lệ")
        void TC_CANDIDATE_016_changeProcess_FailInvalidOrder() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(eq(20L), eq(1L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(false);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.INVALID_PROCESS_TYPE_CHANGE));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-023
         * Test Objective: Validate TC_CANDIDATE_017_changeProcess_FailCurrentProcessMissing behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-017: Chuyển bước thất bại khi không tìm thấy current process")
        void TC_CANDIDATE_017_changeProcess_FailCurrentProcessMissing() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setJobAdId(10L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(eq(20L), eq(1L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(true);
            when(jobAdProcessCandidateService.getCurrentProcess(10L, 20L)).thenReturn(null);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ERROR));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-024
         * Test Objective: Validate TC_CANDIDATE_017B_changeProcess_FailAccessDeniedForNonAdmin behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-017B: Chuyển bước thất bại khi non-admin không có quyá»Ân")
        void TC_CANDIDATE_017B_changeProcess_FailAccessDeniedForNonAdmin() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndHrContactId(55L, 99L)).thenReturn(false);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));

                verify(jobAdCandidateRepository, never()).findById(anyLong());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-025
         * Test Objective: Validate TC_CANDIDATE_017C_changeProcess_FailTargetJobAdProcessMissing behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-017C: Chuyển bước thất bại khi không tìm thấy jobAdProcess đích")
        void TC_CANDIDATE_017C_changeProcess_FailTargetJobAdProcessMissing() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            toProc.setJobAdProcessId(200L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setJobAdId(10L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(20L, 1L, CandidateStatus.ONBOARDED.name())).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(true);
            when(jobAdProcessCandidateService.getCurrentProcess(10L, 20L)).thenReturn(
                com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto.builder().processName("Apply").build()
            );
            when(jobAdProcessService.getById(200L)).thenReturn(null);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                        .isEqualTo(com.cvconnect.enums.CoreErrorCode.PROCESS_TYPE_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-026
         * Test Objective: Validate TC_CANDIDATE_018_changeProcess_FailOnboardDateInvalid behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-018: Chuyển sang ONBOARD thất bại do onboard date trong quá khứ")
        void TC_CANDIDATE_018_changeProcess_FailOnboardDateInvalid() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);
            req.setOnboardDate(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            toProc.setJobAdProcessId(200L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setJobAdId(10L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(eq(20L), eq(1L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(true);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto currentProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            currentProc.setProcessName("Interview");
            when(jobAdProcessCandidateService.getCurrentProcess(10L, 20L)).thenReturn(currentProc);

            JobAdProcessDto onboardProcess = new JobAdProcessDto();
            onboardProcess.setId(200L);
            onboardProcess.setProcessType(ProcessTypeDto.builder().code(ProcessTypeEnum.ONBOARD.name()).build());
            when(jobAdProcessService.getById(200L)).thenReturn(onboardProcess);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.ONBOARD_DATE_INVALID));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-027
         * Test Objective: Validate TC_CANDIDATE_019_changeProcess_FailEmailConfigNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-019: Chuyển bước thất bại khi bật gửi email nhưng chưa cấu hình email")
        void TC_CANDIDATE_019_changeProcess_FailEmailConfigNotFound() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(100L);
            req.setSendEmail(true);
            req.setSubject("Subject");
            req.setTemplate("Body");

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(100L);
            toProc.setJobAdCandidateId(55L);
            toProc.setJobAdProcessId(200L);
            when(jobAdProcessCandidateService.findById(100L)).thenReturn(toProc);

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(55L);
            jac.setJobAdId(10L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.APPLIED.name());
            when(jobAdCandidateRepository.findById(55L)).thenReturn(Optional.of(jac));
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(eq(20L), eq(1L), eq(CandidateStatus.ONBOARDED.name()))).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(100L, 55L)).thenReturn(true);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto currentProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            currentProc.setProcessName("Apply");
            when(jobAdProcessCandidateService.getCurrentProcess(10L, 20L)).thenReturn(currentProc);

            JobAdProcessDto interviewProcess = new JobAdProcessDto();
            interviewProcess.setId(200L);
            interviewProcess.setName("Interview");
            interviewProcess.setProcessType(ProcessTypeDto.builder().code(ProcessTypeEnum.INTERVIEW.name()).build());
            when(jobAdProcessService.getById(200L)).thenReturn(interviewProcess);

            when(jobAdProcessCandidateService.findByJobAdCandidateId(55L)).thenReturn(List.of(
                com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto.builder().id(100L).build()
            ));
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(null);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(55L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.EMAIL_CONFIG_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-028
         * Test Objective: Validate TC_CANDIDATE_019B_changeProcess_FailEmailTemplateNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-019B: Chuyển bước thất bại khi email template không tồn tại")
        void TC_CANDIDATE_019B_changeProcess_FailEmailTemplateNotFound() {
            com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest req = new com.cvconnect.dto.jobAdCandidate.ChangeCandidateProcessRequest();
            req.setToJobAdProcessCandidateId(102L);
            req.setSendEmail(true);
            req.setEmailTemplateId(999L);

            com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto toProc = new com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto();
            toProc.setId(102L);
            toProc.setJobAdCandidateId(56L);
            toProc.setJobAdProcessId(201L);
            when(jobAdProcessCandidateService.findById(102L)).thenReturn(toProc);

            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(56L);
            jac.setJobAdId(11L);
            jac.setCandidateInfoId(21L);
            jac.setCandidateStatus(CandidateStatus.APPLIED.name());
            when(jobAdCandidateRepository.findById(56L)).thenReturn(Optional.of(jac));

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(21L, 1L, CandidateStatus.ONBOARDED.name())).thenReturn(false);
            when(jobAdProcessCandidateService.validateProcessOrderChange(102L, 56L)).thenReturn(true);
            when(jobAdProcessCandidateService.getCurrentProcess(11L, 21L)).thenReturn(
                com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto.builder().processName("Apply").build()
            );

            JobAdProcessDto interviewProcess = new JobAdProcessDto();
            interviewProcess.setId(201L);
            interviewProcess.setName("Interview");
            interviewProcess.setProcessType(ProcessTypeDto.builder().code(ProcessTypeEnum.INTERVIEW.name()).build());
            when(jobAdProcessService.getById(201L)).thenReturn(interviewProcess);

            when(jobAdProcessCandidateService.findByJobAdCandidateId(56L)).thenReturn(List.of(
                com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto.builder().id(102L).build()
            ));
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            when(restTemplateClient.getEmailTemplateById(999L)).thenReturn(null);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(56L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeCandidateProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(com.cvconnect.enums.CoreErrorCode.EMAIL_TEMPLATE_NOT_FOUND));

                verify(sendEmailService, never()).sendEmailWithBody(any());
            }
        }
    }

    @Nested
    @DisplayName("4. filter() & detail() & Projection logic")
    class QueryCandidateTest {
        /**
         * Test Case ID: TC-CANDIDATE-029
         * Test Objective: Validate TC_CANDIDATE_012_filter_OrgAdmin behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-012: filter() cho ORG_ADMIN (Không filtered theo participant)")
        void TC_CANDIDATE_012_filter_OrgAdmin() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            
            CandidateInfoApplyProjection p = mock(CandidateInfoApplyProjection.class);
            when(p.getId()).thenReturn(20L);
            org.springframework.data.domain.Page<CandidateInfoApplyProjection> page = new org.springframework.data.domain.PageImpl<>(List.of(p));
            when(jobAdCandidateRepository.filter(any(), anyLong(), any(), any())).thenReturn(page);
            
            com.cvconnect.dto.jobAdCandidate.CandidateFilterRequest request = new com.cvconnect.dto.jobAdCandidate.CandidateFilterRequest();

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                
                candidateService.filter(request);
                verify(jobAdCandidateRepository).filter(any(), eq(1L), isNull(), any());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-030
         * Test Objective: Validate TC_CANDIDATE_013_detailSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-013: detail() cho ứng viên")
        void TC_CANDIDATE_013_detailSuccess() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            CandidateInfoApplyProjection p = mock(CandidateInfoApplyProjection.class);
            when(p.getId()).thenReturn(20L);
            when(p.getCandidateId()).thenReturn(99L);
            when(jobAdCandidateRepository.getCandidateInfoDetailProjection(anyLong(), anyLong(), any())).thenReturn(p);
            
            when(restTemplateClient.getUser(99L)).thenReturn(new UserDto());

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                
                CandidateInfoDetail resp = candidateService.candidateDetail(20L);
                assertThat(resp).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("5. Additional branch expansion")
    class AdditionalCoverageTest {

        /**
         * Test Case ID: TC-CANDIDATE-031
         * Test Objective: Validate TC_CANDIDATE_020_existsByJobAdCandidateIdAndHrContactId_delegate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-020: existsByJobAdCandidateIdAndHrContactId delegate")
        void TC_CANDIDATE_020_existsByJobAdCandidateIdAndHrContactId_delegate() {
            when(jobAdCandidateRepository.existsByJobAdCandidateIdAndHrContactId(1L, 2L)).thenReturn(true);

            Boolean result = candidateService.existsByJobAdCandidateIdAndHrContactId(1L, 2L);

            assertThat(result).isTrue();
            verify(jobAdCandidateRepository).existsByJobAdCandidateIdAndHrContactId(1L, 2L);
        }

        /**
         * Test Case ID: TC-CANDIDATE-032
         * Test Objective: Validate TC_CANDIDATE_021_existsByJobAdCandidateIdAndOrgId_delegate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-021: existsByJobAdCandidateIdAndOrgId delegate")
        void TC_CANDIDATE_021_existsByJobAdCandidateIdAndOrgId_delegate() {
            when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(1L, 3L)).thenReturn(false);

            Boolean result = candidateService.existsByJobAdCandidateIdAndOrgId(1L, 3L);

            assertThat(result).isFalse();
            verify(jobAdCandidateRepository).existsByJobAdCandidateIdAndOrgId(1L, 3L);
        }

        /**
         * Test Case ID: TC-CANDIDATE-033
         * Test Objective: Validate TC_CANDIDATE_022_findById_notFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-022: findById trả null khi không tồn tại")
        void TC_CANDIDATE_022_findById_notFound() {
            when(jobAdCandidateRepository.findById(99L)).thenReturn(Optional.empty());

            com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto result = candidateService.findById(99L);

            assertThat(result).isNull();
        }

        /**
         * Test Case ID: TC-CANDIDATE-034
         * Test Objective: Validate TC_CANDIDATE_023_findById_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-023: findById trả dto khi tồn tại")
        void TC_CANDIDATE_023_findById_success() {
            JobAdCandidate entity = new JobAdCandidate();
            entity.setId(99L);
            entity.setJobAdId(10L);
            entity.setCandidateInfoId(20L);
            when(jobAdCandidateRepository.findById(99L)).thenReturn(Optional.of(entity));

            com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto result = candidateService.findById(99L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
        }

        /**
         * Test Case ID: TC-CANDIDATE-035
         * Test Objective: Validate TC_CANDIDATE_024_validateAndGetHrContactId_candidateMissing behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-024: validateAndGetHrContactId lỗi khi không có candidate apply")
        void TC_CANDIDATE_024_validateAndGetHrContactId_candidateMissing() {
            when(jobAdCandidateRepository.findByJobAdIdAndCandidateId(10L, 20L)).thenReturn(null);

            assertThatThrownBy(() -> candidateService.validateAndGetHrContactId(10L, 20L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_INFO_APPLY_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-036
         * Test Objective: Validate TC_CANDIDATE_025_validateAndGetHrContactId_jobMissing behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-025: validateAndGetHrContactId lỗi access denied khi job không tồn tại")
        void TC_CANDIDATE_025_validateAndGetHrContactId_jobMissing() {
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(1L);
            when(jobAdCandidateRepository.findByJobAdIdAndCandidateId(10L, 20L)).thenReturn(jac);
            when(jobAdService.findById(10L)).thenReturn(null);

            assertThatThrownBy(() -> candidateService.validateAndGetHrContactId(10L, 20L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CANDIDATE-037
         * Test Objective: Validate TC_CANDIDATE_026_validateAndGetHrContactId_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-026: validateAndGetHrContactId thành công")
        void TC_CANDIDATE_026_validateAndGetHrContactId_success() {
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(1L);
            when(jobAdCandidateRepository.findByJobAdIdAndCandidateId(10L, 20L)).thenReturn(jac);
            JobAdDto jobAdDto = new JobAdDto();
            jobAdDto.setHrContactId(777L);
            when(jobAdService.findById(10L)).thenReturn(jobAdDto);

            Long hrContactId = candidateService.validateAndGetHrContactId(10L, 20L);

            assertThat(hrContactId).isEqualTo(777L);
        }

        /**
         * Test Case ID: TC-CANDIDATE-038
         * Test Objective: Validate TC_CANDIDATE_027_sendEmailToCandidate_emailConfigNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-027: sendEmailToCandidate lỗi khi chưa cấu hình email")
        void TC_CANDIDATE_027_sendEmailToCandidate_emailConfigNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(null);

            com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest request = new com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest();
            request.setJobAdId(10L);

            assertThatThrownBy(() -> candidateService.sendEmailToCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.EMAIL_CONFIG_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-039
         * Test Objective: Validate TC_CANDIDATE_028_sendEmailToCandidate_jobAdNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-028: sendEmailToCandidate lỗi khi job ad không tồn tại")
        void TC_CANDIDATE_028_sendEmailToCandidate_jobAdNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            when(jobAdService.findById(10L)).thenReturn(null);

            com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest request = new com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest();
            request.setJobAdId(10L);

            assertThatThrownBy(() -> candidateService.sendEmailToCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.JOB_AD_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-040
         * Test Objective: Validate TC_CANDIDATE_029_sendEmailToCandidate_accessDenied behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-029: sendEmailToCandidate lỗi access denied khi khác org")
        void TC_CANDIDATE_029_sendEmailToCandidate_accessDenied() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            JobAdDto jobAdDto = new JobAdDto();
            jobAdDto.setId(10L);
            jobAdDto.setOrgId(2L);
            when(jobAdService.findById(10L)).thenReturn(jobAdDto);

            com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest request = new com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest();
            request.setJobAdId(10L);

            assertThatThrownBy(() -> candidateService.sendEmailToCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CANDIDATE-041
         * Test Objective: Validate TC_CANDIDATE_030_sendEmailToCandidate_candidateInfoNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-030: sendEmailToCandidate lỗi khi candidate info apply không tồn tại trong job")
        void TC_CANDIDATE_030_sendEmailToCandidate_candidateInfoNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            JobAdDto jobAdDto = new JobAdDto();
            jobAdDto.setId(10L);
            jobAdDto.setOrgId(1L);
            when(jobAdService.findById(10L)).thenReturn(jobAdDto);
            when(jobAdCandidateRepository.existsByJobAdIdAndCandidateInfoId(10L, 20L)).thenReturn(false);

            com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest request = new com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest();
            request.setJobAdId(10L);
            request.setCandidateInfoId(20L);

            assertThatThrownBy(() -> candidateService.sendEmailToCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_INFO_APPLY_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-042
         * Test Objective: Validate TC_CANDIDATE_031_sendEmailToCandidate_templateNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-031: sendEmailToCandidate lỗi khi email template không tồn tại")
        void TC_CANDIDATE_031_sendEmailToCandidate_templateNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new com.cvconnect.dto.internal.response.EmailConfigDto());
            JobAdDto jobAdDto = new JobAdDto();
            jobAdDto.setId(10L);
            jobAdDto.setOrgId(1L);
            when(jobAdService.findById(10L)).thenReturn(jobAdDto);
            when(jobAdCandidateRepository.existsByJobAdIdAndCandidateInfoId(10L, 20L)).thenReturn(true);
            when(restTemplateClient.getEmailTemplateById(999L)).thenReturn(null);

            com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest request = new com.cvconnect.dto.jobAdCandidate.SendEmailToCandidateRequest();
            request.setJobAdId(10L);
            request.setCandidateInfoId(20L);
            request.setEmailTemplateId(999L);

            assertThatThrownBy(() -> candidateService.sendEmailToCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.EMAIL_TEMPLATE_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CANDIDATE-043
         * Test Objective: Validate TC_CANDIDATE_032_jobAdCandidateConversationForOrg_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-032: jobAdCandidateConversationForOrg trả data và skip projection null")
        void TC_CANDIDATE_032_jobAdCandidateConversationForOrg_success() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            com.cvconnect.dto.internal.response.ConversationDto c1 = new com.cvconnect.dto.internal.response.ConversationDto();
            c1.setJobAdId(10L);
            c1.setCandidateId(101L);
            c1.setLastMessageSenderId(555L);
            c1.setLastMessageSeenBy(List.of());

            com.cvconnect.dto.internal.response.ConversationDto c2 = new com.cvconnect.dto.internal.response.ConversationDto();
            c2.setJobAdId(11L);
            c2.setCandidateId(102L);
            c2.setLastMessageSenderId(100L);
            c2.setLastMessageSeenBy(List.of(100L));

            nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.internal.response.ConversationDto> conversationPage =
                new nmquan.commonlib.dto.response.FilterResponse<>();
            conversationPage.setData(List.of(c1, c2));
            nmquan.commonlib.dto.PageInfo pageInfo = new nmquan.commonlib.dto.PageInfo();
            pageInfo.setPageIndex(0);
            pageInfo.setPageSize(20);
            conversationPage.setPageInfo(pageInfo);

            when(restTemplateClient.getMyConversationsWithFilter(any())).thenReturn(conversationPage);

            com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection projection = mock(com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection.class);
            when(projection.getId()).thenReturn(500L);
            when(projection.getJobAdId()).thenReturn(10L);
            when(projection.getJobAdTitle()).thenReturn("Backend Dev");
            when(projection.getCandidateInfoId()).thenReturn(900L);
            when(projection.getFullName()).thenReturn("Candidate A");

            when(jobAdCandidateRepository.getJobAdCandidateByJobAdIdAndCandidateId(10L, 101L)).thenReturn(projection);
            when(jobAdCandidateRepository.getJobAdCandidateByJobAdIdAndCandidateId(11L, 102L)).thenReturn(null);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(100L);

                com.cvconnect.dto.jobAdCandidate.MyConversationWithFilter request = new com.cvconnect.dto.jobAdCandidate.MyConversationWithFilter();
                nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto> response =
                    candidateService.jobAdCandidateConversationForOrg(request);

                assertThat(response).isNotNull();
                assertThat(response.getData()).hasSize(1);
                assertThat(response.getData().get(0).getId()).isEqualTo(500L);
                assertThat(response.getData().get(0).isHasMessageUnread()).isTrue();
                assertThat(response.getPageInfo()).isEqualTo(pageInfo);
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-044
         * Test Objective: Validate TC_CANDIDATE_033_changeOnboardDate_failPastDate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-033: changeOnboardDate lỗi khi ngày onboard trong quá khứ")
        void TC_CANDIDATE_033_changeOnboardDate_failPastDate() {
            com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest request =
                com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest.builder()
                    .jobAdCandidateId(77L)
                    .newOnboardDate(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                    .build();

            assertThatThrownBy(() -> candidateService.changeOnboardDate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                    .isEqualTo(com.cvconnect.enums.CoreErrorCode.ONBOARD_DATE_INVALID));
        }

        /**
         * Test Case ID: TC-CANDIDATE-045
         * Test Objective: Validate TC_CANDIDATE_034_changeOnboardDate_failNotInOnboardProcess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-034: changeOnboardDate lỗi khi candidate không ở process ONBOARD")
        void TC_CANDIDATE_034_changeOnboardDate_failNotInOnboardProcess() {
            com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest request =
                com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest.builder()
                    .jobAdCandidateId(77L)
                    .newOnboardDate(java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.DAYS))
                    .build();

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(77L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            jac.setCandidateInfoId(20L);
            when(jobAdCandidateRepository.findById(77L)).thenReturn(Optional.of(jac));
            when(jobAdProcessCandidateService.validateCurrentProcessTypeIs(77L, ProcessTypeEnum.ONBOARD.name())).thenReturn(false);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(77L, 1L)).thenReturn(true);

                assertThatThrownBy(() -> candidateService.changeOnboardDate(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode())
                        .isEqualTo(com.cvconnect.enums.CoreErrorCode.CANDIDATE_NOT_IN_ONBOARD_PROCESS));
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-046
         * Test Objective: Validate TC_CANDIDATE_034B_changeOnboardDate_failAccessDeniedForNonAdmin behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-034B: changeOnboardDate lỗi access denied cho non-admin không được phân quyá»Ân")
        void TC_CANDIDATE_034B_changeOnboardDate_failAccessDeniedForNonAdmin() {
            com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest request =
                com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest.builder()
                    .jobAdCandidateId(78L)
                    .newOnboardDate(java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.DAYS))
                    .build();

            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndHrContactId(78L, 99L)).thenReturn(false);

                assertThatThrownBy(() -> candidateService.changeOnboardDate(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException)e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));

                verify(jobAdCandidateRepository, never()).findById(anyLong());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-047
         * Test Objective: Validate TC_CANDIDATE_035_changeOnboardDate_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-035: changeOnboardDate thành công và gửi notify")
        void TC_CANDIDATE_035_changeOnboardDate_success() {
            java.time.Instant newOnboardDate = java.time.Instant.now().plus(3, java.time.temporal.ChronoUnit.DAYS);
            com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest request =
                com.cvconnect.dto.jobAdCandidate.ChangeOnboardDateRequest.builder()
                    .jobAdCandidateId(77L)
                    .newOnboardDate(newOnboardDate)
                    .build();

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            JobAdCandidate jac = new JobAdCandidate();
            jac.setId(77L);
            jac.setJobAdId(10L);
            jac.setCandidateInfoId(20L);
            jac.setCandidateStatus(CandidateStatus.IN_PROGRESS.name());
            when(jobAdCandidateRepository.findById(77L)).thenReturn(Optional.of(jac));
            when(jobAdProcessCandidateService.validateCurrentProcessTypeIs(77L, ProcessTypeEnum.ONBOARD.name())).thenReturn(true);
            when(jobAdCandidateRepository.existsByCandidateInfoAndOrg(20L, 1L, CandidateStatus.ONBOARDED.name())).thenReturn(false);

            JobAdDto jobAdDto = new JobAdDto();
            jobAdDto.setId(10L);
            jobAdDto.setTitle("Backend Engineer");
            when(jobAdService.findById(10L)).thenReturn(jobAdDto);

            CandidateInfoApplyDto candidateInfo = new CandidateInfoApplyDto();
            candidateInfo.setId(20L);
            candidateInfo.setCandidateId(200L);
            candidateInfo.setFullName("Candidate A");
            when(candidateInfoApplyService.getById(20L)).thenReturn(candidateInfo);

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of(com.cvconnect.constant.Constants.RoleCode.ORG_ADMIN));
                when(jobAdCandidateRepository.existsByJobAdCandidateIdAndOrgId(77L, 1L)).thenReturn(true);

                candidateService.changeOnboardDate(request);

                verify(jobAdCandidateRepository).save(jac);
                verify(kafkaUtils).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
                assertThat(jac.getOnboardDate()).isEqualTo(newOnboardDate);
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-048
         * Test Objective: Validate TC_CANDIDATE_036_getJobAdsAppliedByCandidate_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-036: getJobAdsAppliedByCandidate trả dữ liệu mapping thành công")
        void TC_CANDIDATE_036_getJobAdsAppliedByCandidate_success() {
            com.cvconnect.dto.jobAdCandidate.JobAdAppliedFilterRequest request = new com.cvconnect.dto.jobAdCandidate.JobAdAppliedFilterRequest();

            com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection projection = mock(com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection.class);
            when(projection.getJobAdId()).thenReturn(10L);
            when(projection.getJobAdTitle()).thenReturn("Backend Engineer");
            when(projection.getHrContactId()).thenReturn(1L);
            when(projection.getJobAdCandidateId()).thenReturn(1000L);
            when(projection.getCandidateStatus()).thenReturn(CandidateStatus.IN_PROGRESS.name());
            when(projection.getApplyDate()).thenReturn(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
            when(projection.getOnboardDate()).thenReturn(null);
            when(projection.getEliminateReasonType()).thenReturn(null);
            when(projection.getEliminateDate()).thenReturn(null);
            when(projection.getJobAdProcessId()).thenReturn(50L);
            when(projection.getProcessName()).thenReturn("Interview");
            when(projection.getTransferDate()).thenReturn(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
            when(projection.getOrgId()).thenReturn(2L);
            when(projection.getOrgName()).thenReturn("CVConnect Org");
            when(projection.getFullName()).thenReturn("Candidate A");
            when(projection.getPhone()).thenReturn("0900000000");
            when(projection.getEmail()).thenReturn("candidate@test.com");
            when(projection.getCoverLetter()).thenReturn("Cover letter");
            when(projection.getCvFileId()).thenReturn(3L);
            when(projection.getCandidateId()).thenReturn(200L);

            org.springframework.data.domain.Page<com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection> page =
                new org.springframework.data.domain.PageImpl<>(List.of(projection));
            when(jobAdCandidateRepository.getJobAdsAppliedByCandidate(any(), any())).thenReturn(page);

            UserDto hr = new UserDto();
            hr.setId(1L);
            hr.setFullName("HR A");
            when(restTemplateClient.getUsersByIds(anyList())).thenReturn(java.util.Map.of(1L, hr));

            com.cvconnect.dto.org.OrgDto orgDto = new com.cvconnect.dto.org.OrgDto();
            orgDto.setId(2L);
            orgDto.setLogoUrl("logo-url");
            when(orgService.getOrgMapByIds(anyList())).thenReturn(java.util.Map.of(2L, orgDto));

            com.cvconnect.dto.internal.response.ConversationDto conversation = new com.cvconnect.dto.internal.response.ConversationDto();
            conversation.setJobAdId(10L);
            when(restTemplateClient.getConversationUnread()).thenReturn(List.of(conversation));

            com.cvconnect.dto.attachFile.AttachFileDto cvFile = new com.cvconnect.dto.attachFile.AttachFileDto();
            cvFile.setSecureUrl("https://cv.file");
            when(attachFileService.getAttachFiles(anyList())).thenReturn(List.of(cvFile));

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(100L);

                nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto> response =
                    candidateService.getJobAdsAppliedByCandidate(request);

                assertThat(response).isNotNull();
                assertThat(response.getData()).hasSize(1);
                assertThat(response.getData().get(0).isHasMessageUnread()).isTrue();
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-049
         * Test Objective: Validate TC_CANDIDATE_037_jobAdCandidateConversation_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-037: jobAdCandidateConversation trả dữ liệu có unread")
        void TC_CANDIDATE_037_jobAdCandidateConversation_success() {
            com.cvconnect.dto.jobAdCandidate.JobAdAppliedFilterRequest request = new com.cvconnect.dto.jobAdCandidate.JobAdAppliedFilterRequest();

            com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection projection = mock(com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection.class);
            when(projection.getJobAdCandidateId()).thenReturn(1000L);
            when(projection.getJobAdId()).thenReturn(10L);
            when(projection.getJobAdTitle()).thenReturn("Backend Engineer");
            when(projection.getOrgId()).thenReturn(2L);
            when(projection.getOrgName()).thenReturn("CVConnect Org");
            when(projection.getCandidateStatus()).thenReturn(CandidateStatus.IN_PROGRESS.name());
            when(projection.getApplyDate()).thenReturn(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
            when(projection.getOnboardDate()).thenReturn(null);
            when(projection.getEliminateReasonType()).thenReturn(null);
            when(projection.getEliminateDate()).thenReturn(null);
            when(projection.getJobAdProcessId()).thenReturn(50L);
            when(projection.getProcessName()).thenReturn("Interview");
            when(projection.getTransferDate()).thenReturn(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
            when(projection.getFullName()).thenReturn("Candidate A");
            when(projection.getPhone()).thenReturn("0900000000");
            when(projection.getEmail()).thenReturn("candidate@test.com");
            when(projection.getCoverLetter()).thenReturn("Cover letter");
            when(projection.getCvFileId()).thenReturn(3L);
            when(projection.getCandidateId()).thenReturn(200L);

            org.springframework.data.domain.Page<com.cvconnect.dto.jobAdCandidate.JobAdAppliedProjection> page =
                new org.springframework.data.domain.PageImpl<>(List.of(projection));
            when(jobAdCandidateRepository.getJobAdsAppliedByCandidate(any(), any())).thenReturn(page);

            com.cvconnect.dto.org.OrgDto orgDto = new com.cvconnect.dto.org.OrgDto();
            orgDto.setId(2L);
            orgDto.setLogoUrl("logo-url");
            when(orgService.getOrgMapByIds(anyList())).thenReturn(java.util.Map.of(2L, orgDto));

            com.cvconnect.dto.internal.response.ConversationDto conversation = new com.cvconnect.dto.internal.response.ConversationDto();
            conversation.setJobAdId(10L);
            conversation.setLastMessageSenderId(999L);
            conversation.setLastMessageSeenBy(List.of());
            conversation.setLastMessageSentAt(java.time.Instant.now());
            when(restTemplateClient.getMyConversations()).thenReturn(List.of(conversation));

            when(jobAdProcessCandidateService.getDetailByJobAdCandidateIds(anyList())).thenReturn(java.util.Map.of(1000L, List.of()));

            com.cvconnect.dto.attachFile.AttachFileDto cvFile = new com.cvconnect.dto.attachFile.AttachFileDto();
            cvFile.setSecureUrl("https://cv.file");
            when(attachFileService.getAttachFiles(anyList())).thenReturn(List.of(cvFile));

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(100L);

                nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto> response =
                    candidateService.jobAdCandidateConversation(request);

                assertThat(response).isNotNull();
                assertThat(response.getData()).hasSize(1);
                assertThat(response.getData().get(0).isHasMessageUnread()).isTrue();
                assertThat(request.getSortBy()).isEqualTo("applyDate");
                assertThat(request.getPageIndex()).isEqualTo(0);
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-050
         * Test Objective: Validate TC_CANDIDATE_038_getListOfOnboardedCandidates_nonAdmin behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-038: getListOfOnboardedCandidates cho non-admin set participantId và sortBy")
        void TC_CANDIDATE_038_getListOfOnboardedCandidates_nonAdmin() {
            com.cvconnect.dto.jobAdCandidate.CandidateOnboardFilterRequest request = new com.cvconnect.dto.jobAdCandidate.CandidateOnboardFilterRequest();
            request.setOnboardDateEnd(java.time.Instant.now());
            request.setApplyDateEnd(java.time.Instant.now());

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            org.springframework.data.domain.Page<com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection> page =
                new org.springframework.data.domain.PageImpl<>(List.of());
            when(jobAdCandidateRepository.getListOfOnboardedCandidates(any(), any(), any())).thenReturn(page);
            when(restTemplateClient.getUsersByIds(anyList())).thenReturn(java.util.Map.of());

            try (org.mockito.MockedStatic<nmquan.commonlib.utils.WebUtils> webUtils = org.mockito.Mockito.mockStatic(nmquan.commonlib.utils.WebUtils.class)) {
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));
                webUtils.when(nmquan.commonlib.utils.WebUtils::getCurrentUserId).thenReturn(99L);

                nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto> response =
                    candidateService.getListOfOnboardedCandidates(request);

                assertThat(response).isNotNull();
                assertThat(response.getData()).isEmpty();
                assertThat(request.getOrgId()).isEqualTo(1L);
                assertThat(request.getSortBy()).isEqualTo("onboardDate");
                verify(jobAdCandidateRepository).getListOfOnboardedCandidates(eq(request), eq(99L), any());
            }
        }

        /**
         * Test Case ID: TC-CANDIDATE-051
         * Test Objective: Validate TC_CANDIDATE_039_getJobAdCandidateData_success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CANDIDATE-039: getJobAdCandidateData trả dữ liệu tóm tắt")
        void TC_CANDIDATE_039_getJobAdCandidateData_success() {
            com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection projection = mock(com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection.class);
            when(projection.getJobAdTitle()).thenReturn("Backend Engineer");
            when(projection.getFullName()).thenReturn("Candidate A");
            when(projection.getCandidateInfoId()).thenReturn(20L);
            when(jobAdCandidateRepository.getJobAdCandidateByJobAdIdAndCandidateId(10L, 200L)).thenReturn(projection);

            com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto result = candidateService.getJobAdCandidateData(10L, 200L);

            assertThat(result).isNotNull();
            assertThat(result.getJobAdTitle()).isEqualTo("Backend Engineer");
            assertThat(result.getFullName()).isEqualTo("Candidate A");
            assertThat(result.getCandidateInfoId()).isEqualTo(20L);
        }
    }
}


