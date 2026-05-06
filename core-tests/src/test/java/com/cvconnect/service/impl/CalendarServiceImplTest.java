package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CalendarServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test for {@link com.cvconnect.service.impl.CalendarServiceImpl}
 *
 * COVERED BRANCHES:
 *   - createCalendar (joinSameTime true/false, sendEmail true/false, emailTemplate present/absent)
 *   - filterViewCandidateCalendars (role based access, participation types)
 *   - detailInViewCandidate (joinSameTime logic)
 *   - filterViewGeneral (date range normalization, grouping logic)
 *   - detailInViewGeneral (OFFLINE vs ONLINE detail differences)
 *   - validateCreateCalendar (all exception branches)
 * ============================================================
 */

import com.cvconnect.common.ReplacePlaceholder;
import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.calendar.*;
import com.cvconnect.dto.common.NotificationDto;
import com.cvconnect.dto.internal.response.ConversationDto;
import com.cvconnect.dto.internal.response.EmailConfigDto;
import com.cvconnect.dto.internal.response.EmailTemplateDto;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAd.JobAdDto;
import com.cvconnect.dto.jobAd.JobAdProcessDto;
import com.cvconnect.dto.org.OrgAddressDto;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.enums.CalendarType;
import com.cvconnect.enums.ParticipationType;
import com.cvconnect.repository.CalendarRepository;
import com.cvconnect.service.*;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import nmquan.commonlib.service.SendEmailService;
import nmquan.commonlib.utils.KafkaUtils;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarServiceImpl - Unit Tests")
public class CalendarServiceImplTest {

    @Mock private CalendarRepository calendarRepository;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private InterviewPanelService interviewPanelService;
    @Mock private CalendarCandidateInfoService calendarCandidateInfoService;
    @Mock private JobAdProcessService jobAdProcessService;
    @Mock private CandidateInfoApplyService candidateInfoApplyService;
    @Mock private SendEmailService sendEmailService;
    @Mock private ReplacePlaceholder replacePlaceholder;
    @Mock private JobAdService jobAdService;
    @Mock private JobAdCandidateService jobAdCandidateService;
    @Mock private OrgAddressService orgAddressService;
    @Mock private KafkaUtils kafkaUtils;

    @InjectMocks
    private com.cvconnect.service.impl.CalendarServiceImpl calendarService;

    private CalendarRequest taoRequest() {
        CalendarRequest req = new CalendarRequest();
        req.setJobAdProcessId(10L);
        req.setCalendarType(CalendarType.INTERVIEW_ONLINE);
        req.setMeetingLink("https://zoom.us");
        req.setParticipantIds(List.of(1L));
        req.setCandidateInfoIds(List.of(2L));
        req.setDate(LocalDate.now().plusDays(1));
        req.setTimeFrom(LocalTime.of(10, 0));
        req.setDurationMinutes(30);
        return req;
    }

    @Nested
    @DisplayName("1. createCalendar()")
    class CreateCalendarTest {

        /**
         * Test Case ID: TC-CAL-001
         * Test Objective: Validate TC_CAL_001_createSuccess_JoinSameTime behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-001: Tạo thành công - joinSameTime=true, sendEmail=false")
        void TC_CAL_001_createSuccess_JoinSameTime() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);
                when(jobAdService.findByJobAdProcessId(anyLong())).thenReturn(new com.cvconnect.dto.jobAd.JobAdDto());

                CalendarRequest req = taoRequest();
                req.setJoinSameTime(true);
                req.setSendEmail(false);

                IDResponse<Long> res = calendarService.createCalendar(req);

                assertThat(res).isNotNull();
                verify(calendarRepository).save(any());
                verify(kafkaUtils).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
            }
        }

        /**
         * Test Case ID: TC-CAL-002
         * Test Objective: Validate TC_CAL_002_createWithEmailTemplate behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-002: Tạo thành công - sendEmail=true với TemplateId")
        void TC_CAL_002_createWithEmailTemplate() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);

                when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new EmailConfigDto());
                EmailTemplateDto template = new EmailTemplateDto();
                template.setBody("Hello ${candidateName}");
                template.setPlaceholderCodes(List.of("${candidateName}"));
                when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(template);
                when(restTemplateClient.getUser(99L)).thenReturn(new UserDto());
                when(jobAdService.findByJobAdProcessId(anyLong())).thenReturn(new com.cvconnect.dto.jobAd.JobAdDto());
                when(candidateInfoApplyService.getByIds(anyList())).thenReturn(Map.of(2L, com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(2L).email("c@g.com").build()));

                CalendarRequest req = taoRequest();
                req.setSendEmail(true);
                req.setEmailTemplateId(5L);

                calendarService.createCalendar(req);

                verify(sendEmailService).sendEmailWithBody(any());
            }
        }

        /**
         * Test Case ID: TC-CAL-003
         * Test Objective: Validate TC_CAL_003_dateInvalid behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-003: Thất bại do ngày trước hôm nay")
        void TC_CAL_003_dateInvalid() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
            
            CalendarRequest req = taoRequest();
            req.setDate(LocalDate.now().minusDays(1));

            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class);
        }

        /**
         * Test Case ID: TC-CAL-004
         * Test Objective: Validate TC_CAL_006_durationInvalid behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-004: Thất bại do duration <= 0")
        void TC_CAL_006_durationInvalid() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);

            CalendarRequest req = taoRequest();
            req.setDurationMinutes(0);

            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DURATION_MINUTES_INVALID));
        }

        /**
         * Test Case ID: TC-CAL-005
         * Test Objective: Validate TC_CAL_007_candidateInfoNotInProcess behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-005: Thất bại do candidateInfo không ở process")
        void TC_CAL_007_candidateInfoNotInProcess() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
            when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
            when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(false);

            CalendarRequest req = taoRequest();
            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CANDIDATE_INFO_EXISTS_NOT_IN_PROCESS));
        }

        /**
         * Test Case ID: TC-CAL-006
         * Test Objective: Validate TC_CAL_008_createSuccess_JoinDifferentTime_NotifyEachCandidate behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-006: Tạo thành công joinSameTime=false gửi notify theo từng candidate")
        void TC_CAL_008_createSuccess_JoinDifferentTime_NotifyEachCandidate() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);

                JobAdDto jobAdDto = new JobAdDto();
                jobAdDto.setId(55L);
                jobAdDto.setTitle("Backend Engineer");
                when(jobAdService.findByJobAdProcessId(anyLong())).thenReturn(jobAdDto);

                CalendarRequest req = taoRequest();
                req.setJoinSameTime(false);
                req.setSendEmail(false);
                req.setCandidateInfoIds(List.of(2L, 3L));

                calendarService.createCalendar(req);

                verify(kafkaUtils, times(2)).sendWithJson(eq(Constants.KafkaTopic.NOTIFICATION), any(NotificationDto.class));
            }
        }

        /**
         * Test Case ID: TC-CAL-007
         * Test Objective: Validate TC_CAL_009_createFail_EmailConfigNotFound behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-007: Thất bại sendEmail do thiếu email config")
        void TC_CAL_009_createFail_EmailConfigNotFound() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);

                CalendarRequest req = taoRequest();
                req.setSendEmail(true);
                req.setEmailTemplateId(5L);
                when(restTemplateClient.getEmailConfigByOrg()).thenReturn(null);

                assertThatThrownBy(() -> calendarService.createCalendar(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.EMAIL_CONFIG_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-008
         * Test Objective: Validate TC_CAL_010_createFail_EmailTemplateNotFound behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-008: Thất bại sendEmail do email template không tồn tại")
        void TC_CAL_010_createFail_EmailTemplateNotFound() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);

                CalendarRequest req = taoRequest();
                req.setSendEmail(true);
                req.setEmailTemplateId(5L);
                when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new EmailConfigDto());
                when(restTemplateClient.getEmailTemplateById(5L)).thenReturn(null);

                assertThatThrownBy(() -> calendarService.createCalendar(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.EMAIL_TEMPLATE_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-009
         * Test Objective: Validate TC_CAL_018_createFail_OfflineMissingAddress behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-009: Thất bại do OFFLINE nhưng thiếu địa điểm")
        void TC_CAL_018_createFail_OfflineMissingAddress() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);

            CalendarRequest req = taoRequest();
            req.setCalendarType(CalendarType.INTERVIEW_OFFLINE);
            req.setOrgAddressId(null);

            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.MEETING_ADDRESS_NOT_NULL));
        }

        /**
         * Test Case ID: TC-CAL-010
         * Test Objective: Validate TC_CAL_019_createFail_OnlineMissingMeetingLink behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-010: Thất bại do ONLINE nhưng thiếu meeting link")
        void TC_CAL_019_createFail_OnlineMissingMeetingLink() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);

            CalendarRequest req = taoRequest();
            req.setCalendarType(CalendarType.INTERVIEW_ONLINE);
            req.setMeetingLink(null);

            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.MEETING_LINK_NOT_NULL));
        }

        /**
         * Test Case ID: TC-CAL-011
         * Test Objective: Validate TC_CAL_020_createFail_InvalidParticipants behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-011: Thất bại do participant không thuộc org")
        void TC_CAL_020_createFail_InvalidParticipants() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
            when(restTemplateClient.checkOrgMember(anyList())).thenReturn(false);

            CalendarRequest req = taoRequest();

            assertThatThrownBy(() -> calendarService.createCalendar(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CAL-012
         * Test Objective: Validate TC_CAL_021_createSuccess_ManualEmailAndSkipNullCandidate behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-012: Tạo thành công sendEmail manual và skip candidateInfo null")
        void TC_CAL_021_createSuccess_ManualEmailAndSkipNullCandidate() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdProcessService.existByJobAdProcessIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(restTemplateClient.checkOrgMember(anyList())).thenReturn(true);
                when(candidateInfoApplyService.validateCandidateInfoInProcess(anyList(), anyLong())).thenReturn(true);
                when(restTemplateClient.getEmailConfigByOrg()).thenReturn(new EmailConfigDto());

                JobAdDto jobAd = new JobAdDto();
                jobAd.setId(55L);
                jobAd.setTitle("Backend Engineer");
                jobAd.setPositionId(10L);
                when(jobAdService.findByJobAdProcessId(anyLong())).thenReturn(jobAd);

                UserDto hr = new UserDto();
                hr.setId(99L);
                hr.setFullName("HR A");
                hr.setEmail("hr@test.com");
                hr.setPhoneNumber("0900000000");
                when(restTemplateClient.getUser(99L)).thenReturn(hr);

                when(replacePlaceholder.replacePlaceholder(anyString(), anyList(), any())).thenReturn("mail-body");

                CalendarRequest req = taoRequest();
                req.setJoinSameTime(false);
                req.setSendEmail(true);
                req.setEmailTemplateId(null);
                req.setSubject("Interview");
                req.setTemplate("Hello {{candidate_name}}");
                req.setPlaceholders(List.of("candidate_name"));
                req.setCandidateInfoIds(List.of(2L, 3L));

                com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto candidate2 = com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder()
                        .id(2L)
                        .fullName("Candidate A")
                        .email("a@test.com")
                        .build();
                when(candidateInfoApplyService.getByIds(req.getCandidateInfoIds())).thenReturn(Map.of(2L, candidate2));

                calendarService.createCalendar(req);

                verify(sendEmailService, times(1)).sendEmailWithBody(any());
            }
        }
    }

    @Nested
    @DisplayName("2B. detailInViewCandidate()")
    class DetailInViewCandidateTest {

        /**
         * Test Case ID: TC-CAL-013
         * Test Objective: Validate TC_CAL_022_detailInViewCandidate_notFound behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-013: detailInViewCandidate lỗi khi không tìm thấy calendar")
        void TC_CAL_022_detailInViewCandidate_notFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(calendarRepository.detailInViewCandidate(11L, 100L, null)).thenReturn(null);

            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                assertThatThrownBy(() -> calendarService.detailInViewCandidate(11L))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CALENDAR_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-014
         * Test Objective: Validate TC_CAL_023_detailInViewCandidate_candidateNotFoundInCalendar behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-014: detailInViewCandidate joinSameTime=false thiếu candidate trong danh sách")
        void TC_CAL_023_detailInViewCandidate_candidateNotFoundInCalendar() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);

            CalendarDetailInViewCandidateProjection projection = mock(CalendarDetailInViewCandidateProjection.class);
            when(projection.getJobAdId()).thenReturn(10L);
            when(projection.getJobAdTitle()).thenReturn("Java Dev");
            when(projection.getJobAdProcessId()).thenReturn(20L);
            when(projection.getJobAdProcessName()).thenReturn("Interview");
            when(projection.getCreatorId()).thenReturn(99L);
            when(projection.getCalendarType()).thenReturn(CalendarType.INTERVIEW_ONLINE.name());
            when(projection.getDate()).thenReturn(LocalDate.now().plusDays(1));
            when(projection.getTimeFrom()).thenReturn(LocalTime.of(9, 0));
            when(projection.getTimeTo()).thenReturn(LocalTime.of(10, 0));
            when(projection.getLocationId()).thenReturn(null);
            when(projection.getMeetingLink()).thenReturn("https://meet");
            when(projection.getCalendarId()).thenReturn(1L);
            when(projection.getJoinSameTime()).thenReturn(false);
            when(projection.getCandidateInfoId()).thenReturn(2L);

            when(calendarRepository.detailInViewCandidate(11L, 100L, null)).thenReturn(projection);
            when(candidateInfoApplyService.getByCalendarId(1L))
                    .thenReturn(List.of(com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(3L).build()));

            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

                assertThatThrownBy(() -> calendarService.detailInViewCandidate(11L))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CANDIDATE_INFO_APPLY_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-015
         * Test Objective: Validate TC_CAL_024_detailInViewCandidate_nonAdminJoinSameTimeWithLocation behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-015: detailInViewCandidate non-admin lấy userId auth, joinSameTime=true và có location")
        void TC_CAL_024_detailInViewCandidate_nonAdminJoinSameTimeWithLocation() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);

            CalendarDetailInViewCandidateProjection projection = mock(CalendarDetailInViewCandidateProjection.class);
            when(projection.getJobAdId()).thenReturn(10L);
            when(projection.getJobAdTitle()).thenReturn("Java Dev");
            when(projection.getJobAdProcessId()).thenReturn(20L);
            when(projection.getJobAdProcessName()).thenReturn("Interview");
            when(projection.getCreatorId()).thenReturn(88L);
            when(projection.getCalendarType()).thenReturn(CalendarType.INTERVIEW_OFFLINE.name());
            when(projection.getDate()).thenReturn(LocalDate.now().plusDays(1));
            when(projection.getTimeFrom()).thenReturn(LocalTime.of(9, 0));
            when(projection.getTimeTo()).thenReturn(LocalTime.of(10, 0));
            when(projection.getLocationId()).thenReturn(50L);
            when(projection.getMeetingLink()).thenReturn(null);
            when(projection.getCalendarId()).thenReturn(1L);
            when(projection.getJoinSameTime()).thenReturn(true);

            when(calendarRepository.detailInViewCandidate(11L, 100L, 99L)).thenReturn(projection);
            when(restTemplateClient.getUser(88L)).thenReturn(UserDto.builder().id(88L).fullName("Creator A").build());
            when(orgAddressService.getById(50L)).thenReturn(new OrgAddressDto());
            when(candidateInfoApplyService.getByCalendarId(1L))
                    .thenReturn(List.of(
                            com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(2L).build(),
                            com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(3L).build()
                    ));
            when(interviewPanelService.getByCalendarId(1L)).thenReturn(List.of(UserDto.builder().id(99L).build()));

            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                CalendarDetail detail = calendarService.detailInViewCandidate(11L);

                assertThat(detail.getLocation()).isNotNull();
                assertThat(detail.getCandidates()).hasSize(2);
                assertThat(detail.getParticipants()).hasSize(1);
            }
        }
    }

    @Nested
    @DisplayName("2. filterViewCandidateCalendars()")
    class FilterViewTest {
        /**
         * Test Case ID: TC-CAL-016
         * Test Objective: Validate TC_CAL_004_filterAdmin behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-016: Role Admin không bị giới hạn participantIdAuth")
        void TC_CAL_004_filterAdmin() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdCandidateService.existsByJobAdCandidateIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                
                CalendarFilterRequest req = new CalendarFilterRequest();
                req.setJobAdCandidateId(1L);
                
                calendarService.filterViewCandidateCalendars(req);
                
                verify(calendarRepository).filterViewCandidateCalendars(any(), any(), any(), isNull());
            }
        }

        /**
         * Test Case ID: TC-CAL-017
         * Test Objective: Validate TC_CAL_011_filterViewCandidate_missingJobAdCandidateId behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-017: filterViewCandidateCalendars lỗi khi thiếu jobAdCandidateId")
        void TC_CAL_011_filterViewCandidate_missingJobAdCandidateId() {
            CalendarFilterRequest req = new CalendarFilterRequest();

            assertThatThrownBy(() -> calendarService.filterViewCandidateCalendars(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CAL-018
         * Test Objective: Validate TC_CAL_012_filterViewCandidate_accessDenied behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-018: filterViewCandidateCalendars lỗi access denied")
        void TC_CAL_012_filterViewCandidate_accessDenied() {
            CalendarFilterRequest req = new CalendarFilterRequest();
            req.setJobAdCandidateId(1L);
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(jobAdCandidateService.existsByJobAdCandidateIdAndOrgId(1L, 100L)).thenReturn(false);

            assertThatThrownBy(() -> calendarService.filterViewCandidateCalendars(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CAL-019
         * Test Objective: Validate TC_CAL_025_filterViewCandidate_nonAdminJoinedByMe behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-019: filterViewCandidateCalendars non-admin JOINED_BY_ME set participantId + participantIdAuth")
        void TC_CAL_025_filterViewCandidate_nonAdminJoinedByMe() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(jobAdCandidateService.existsByJobAdCandidateIdAndOrgId(1L, 100L)).thenReturn(true);
                when(jobAdCandidateService.existsByJobAdCandidateIdAndHrContactId(1L, 99L)).thenReturn(false);
                when(calendarRepository.filterViewCandidateCalendars(any(), isNull(), eq(99L), eq(99L)))
                        .thenReturn(Collections.emptyList());

                CalendarFilterRequest req = new CalendarFilterRequest();
                req.setJobAdCandidateId(1L);
                req.setParticipationType(ParticipationType.JOINED_BY_ME);

                List<CalendarFitterViewCandidateResponse> responses = calendarService.filterViewCandidateCalendars(req);

                assertThat(responses).isEmpty();
                verify(calendarRepository).filterViewCandidateCalendars(any(), isNull(), eq(99L), eq(99L));
            }
        }
    }

    @Nested
    @DisplayName("3. detailInViewGeneral()")
    class DetailViewTest {
        /**
         * Test Case ID: TC-CAL-020
         * Test Objective: Validate TC_CAL_005_detailOffline behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-020: Xử lý detail OFFLINE logic")
        void TC_CAL_005_detailOffline() {
             try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                
                com.cvconnect.entity.Calendar c = new com.cvconnect.entity.Calendar();
                c.setId(1L);
                c.setCalendarType(CalendarType.INTERVIEW_OFFLINE.name());
                c.setJoinSameTime(true);
                c.setOrgAddressId(50L);
                c.setJobAdProcessId(10L);
                c.setDate(java.time.LocalDate.now());
                c.setTimeFrom(java.time.LocalTime.now());
                c.setDurationMinutes(30);
                when(calendarRepository.existsByIdAndOrgId(anyLong(), anyLong())).thenReturn(true);
                when(calendarRepository.findById(1L)).thenReturn(Optional.of(c));
                
                when(jobAdProcessService.getById(10L)).thenReturn(com.cvconnect.dto.jobAd.JobAdProcessDto.builder().jobAdId(5L).build());
                when(jobAdService.findById(5L)).thenReturn(new com.cvconnect.dto.jobAd.JobAdDto());
                when(orgAddressService.getById(50L)).thenReturn(new com.cvconnect.dto.org.OrgAddressDto());

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);
                CalendarDetail detail = calendarService.detailInViewGeneral(req);
                
                assertThat(detail.getLocation()).isNotNull();
            }
        }

        /**
         * Test Case ID: TC-CAL-021
         * Test Objective: Validate TC_CAL_013_detailInViewGeneral_accessDenied behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-021: detailInViewGeneral lỗi access denied khi không phải admin/hr/participant")
        void TC_CAL_013_detailInViewGeneral_accessDenied() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(calendarRepository.existsByIdAndOrgId(1L, 100L)).thenReturn(true);
                when(calendarRepository.existsByCalendarIdAndHrContactId(1L, 99L)).thenReturn(false);
                when(interviewPanelService.getByCalendarId(1L)).thenReturn(List.of(UserDto.builder().id(10L).build()));

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);

                assertThatThrownBy(() -> calendarService.detailInViewGeneral(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
            }
        }

        /**
         * Test Case ID: TC-CAL-022
         * Test Objective: Validate TC_CAL_014_detailInViewGeneral_missingCandidateInfoId behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-022: detailInViewGeneral joinSameTime=false thiếu candidateInfoId")
        void TC_CAL_014_detailInViewGeneral_missingCandidateInfoId() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(calendarRepository.existsByIdAndOrgId(1L, 100L)).thenReturn(true);

                com.cvconnect.entity.Calendar c = new com.cvconnect.entity.Calendar();
                c.setId(1L);
                c.setJoinSameTime(false);
                c.setCalendarType(CalendarType.INTERVIEW_ONLINE.name());
                c.setMeetingLink("https://zoom.us");
                c.setJobAdProcessId(10L);
                c.setCreatorId(99L);
                when(calendarRepository.findById(1L)).thenReturn(Optional.of(c));

                when(interviewPanelService.getByCalendarId(1L)).thenReturn(List.of(UserDto.builder().id(99L).build()));

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);

                assertThatThrownBy(() -> calendarService.detailInViewGeneral(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CANDIDATE_INFO_APPLY_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-023
         * Test Objective: Validate TC_CAL_015_detailInViewGeneral_joinSameTimeOnline behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-023: detailInViewGeneral joinSameTime=true và ONLINE")
        void TC_CAL_015_detailInViewGeneral_joinSameTimeOnline() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(calendarRepository.existsByIdAndOrgId(1L, 100L)).thenReturn(true);

                com.cvconnect.entity.Calendar c = new com.cvconnect.entity.Calendar();
                c.setId(1L);
                c.setCalendarType(CalendarType.INTERVIEW_ONLINE.name());
                c.setJoinSameTime(true);
                c.setMeetingLink("https://meet");
                c.setJobAdProcessId(10L);
                c.setCreatorId(99L);
                c.setDate(LocalDate.now().plusDays(1));
                c.setTimeFrom(LocalTime.of(9, 0));
                c.setDurationMinutes(45);
                when(calendarRepository.findById(1L)).thenReturn(Optional.of(c));

                JobAdProcessDto processDto = JobAdProcessDto.builder().id(10L).jobAdId(5L).name("Interview").build();
                when(jobAdProcessService.getById(10L)).thenReturn(processDto);
                when(jobAdService.findById(5L)).thenReturn(new JobAdDto());
                when(restTemplateClient.getUser(99L)).thenReturn(UserDto.builder().id(99L).build());
                when(interviewPanelService.getByCalendarId(1L)).thenReturn(List.of(UserDto.builder().id(99L).build()));
                when(candidateInfoApplyService.getByCalendarId(1L)).thenReturn(List.of(com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(2L).build()));

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);
                CalendarDetail detail = calendarService.detailInViewGeneral(req);

                assertThat(detail.getMeetingLink()).isEqualTo("https://meet");
                assertThat(detail.getCandidates()).hasSize(1);
                assertThat(detail.getTimeFrom()).isNotNull();
                assertThat(detail.getTimeTo()).isNotNull();
            }
        }

        /**
         * Test Case ID: TC-CAL-024
         * Test Objective: Validate TC_CAL_016_detailInViewGeneral_calendarCandidateNotFound behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-024: detailInViewGeneral joinSameTime=false nhưng không có calendarCandidate")
        void TC_CAL_016_detailInViewGeneral_calendarCandidateNotFound() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(calendarRepository.existsByIdAndOrgId(1L, 100L)).thenReturn(true);

                com.cvconnect.entity.Calendar c = new com.cvconnect.entity.Calendar();
                c.setId(1L);
                c.setCalendarType(CalendarType.INTERVIEW_OFFLINE.name());
                c.setJoinSameTime(false);
                c.setOrgAddressId(50L);
                c.setJobAdProcessId(10L);
                c.setCreatorId(99L);
                when(calendarRepository.findById(1L)).thenReturn(Optional.of(c));

                when(interviewPanelService.getByCalendarId(1L)).thenReturn(List.of(UserDto.builder().id(99L).build()));
                when(jobAdProcessService.getById(10L)).thenReturn(JobAdProcessDto.builder().jobAdId(5L).build());
                when(jobAdService.findById(5L)).thenReturn(new JobAdDto());
                when(restTemplateClient.getUser(99L)).thenReturn(UserDto.builder().id(99L).build());
                when(orgAddressService.getById(50L)).thenReturn(new OrgAddressDto());
                when(candidateInfoApplyService.getById(2L)).thenReturn(com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto.builder().id(2L).build());
                when(calendarCandidateInfoService.getByCalendarIdAndCandidateInfoId(1L, 2L)).thenReturn(null);

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);
                req.setCandidateInfoId(2L);

                assertThatThrownBy(() -> calendarService.detailInViewGeneral(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CALENDAR_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CAL-025
         * Test Objective: Validate TC_CAL_026_detailInViewGeneral_calendarNotInOrg behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-025: detailInViewGeneral lỗi khi calendar không thuộc org")
        void TC_CAL_026_detailInViewGeneral_calendarNotInOrg() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);

                when(restTemplateClient.validOrgMember()).thenReturn(100L);
                when(calendarRepository.existsByIdAndOrgId(1L, 100L)).thenReturn(false);

                CalendarDetailInViewGeneralRequest req = new CalendarDetailInViewGeneralRequest();
                req.setCalendarId(1L);

                assertThatThrownBy(() -> calendarService.detailInViewGeneral(req))
                        .isInstanceOf(AppException.class)
                        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.CALENDAR_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("4. filterViewGeneral()")
    class FilterViewGeneralTest {

        /**
         * Test Case ID: TC-CAL-026
         * Test Objective: Validate TC_CAL_017_filterViewGeneral_nonAdminCreatedByMe behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-026: filterViewGeneral non-admin set participantIdAuth + CREATED_BY_ME")
        void TC_CAL_017_filterViewGeneral_nonAdminCreatedByMe() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));

                when(restTemplateClient.validOrgMember()).thenReturn(100L);

                CalendarDetailInViewCandidateProjection projection = mock(CalendarDetailInViewCandidateProjection.class);
                when(projection.getDate()).thenReturn(LocalDate.now().plusDays(1));
                when(projection.getCalendarId()).thenReturn(1L);
                when(projection.getJoinSameTime()).thenReturn(true);
                when(projection.getHrContactId()).thenReturn(7L);
                when(projection.getJobAdId()).thenReturn(10L);
                when(projection.getJobAdTitle()).thenReturn("Java Dev");
                when(projection.getCalendarType()).thenReturn(CalendarType.INTERVIEW_ONLINE.name());
                when(projection.getTimeFrom()).thenReturn(LocalTime.of(9, 0));
                when(projection.getTimeTo()).thenReturn(LocalTime.of(10, 0));
                when(projection.getCandidateInfoId()).thenReturn(2L);
                when(projection.getFullName()).thenReturn("Candidate A");

                when(calendarRepository.filterViewGeneral(any(), eq(100L), eq(99L), isNull(), eq(99L), eq(99L)))
                        .thenReturn(List.of(projection));
                when(restTemplateClient.getUsersByIds(anyList())).thenReturn(Map.of(7L, UserDto.builder().id(7L).fullName("HR A").build()));

                CalendarFilterRequest req = new CalendarFilterRequest();
                req.setParticipationType(ParticipationType.CREATED_BY_ME);

                List<CalendarFilterResponse> response = calendarService.filterViewGeneral(req);

                assertThat(response).hasSize(1);
                assertThat(response.get(0).getDetails()).hasSize(1);
                assertThat(response.get(0).getDetails().get(0).getCandidateInfos()).hasSize(1);
            }
        }

        /**
         * Test Case ID: TC-CAL-027
         * Test Objective: Validate TC_CAL_027_filterViewGeneral_nonAdminJoinedByMe_DateNormalize_JoinDifferentTime behavior.
         * Input: Mock data based on test context.
         * Expected Output: Correct result based on test case assert/verify.
         * Notes: Auto-generated comment based on RestTemplateClientTest standard.
         */
        @Test
        @DisplayName("TC-CAL-027: filterViewGeneral non-admin JOINED_BY_ME + normalize date + joinSameTime=false")
        void TC_CAL_027_filterViewGeneral_nonAdminJoinedByMe_DateNormalize_JoinDifferentTime() {
            try (MockedStatic<WebUtils> utils = mockStatic(WebUtils.class)) {
                utils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                utils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR_MEMBER"));

                when(restTemplateClient.validOrgMember()).thenReturn(100L);

                CalendarDetailInViewCandidateProjection projection1 = mock(CalendarDetailInViewCandidateProjection.class);
                when(projection1.getDate()).thenReturn(LocalDate.now().plusDays(2));
                when(projection1.getCalendarId()).thenReturn(1L);
                when(projection1.getJoinSameTime()).thenReturn(false);
                when(projection1.getHrContactId()).thenReturn(7L);
                when(projection1.getJobAdId()).thenReturn(10L);
                when(projection1.getJobAdTitle()).thenReturn("Java Dev");
                when(projection1.getCalendarType()).thenReturn(CalendarType.INTERVIEW_ONLINE.name());
                when(projection1.getTimeFrom()).thenReturn(LocalTime.of(9, 0));
                when(projection1.getTimeTo()).thenReturn(LocalTime.of(9, 30));
                when(projection1.getCandidateInfoId()).thenReturn(2L);
                when(projection1.getFullName()).thenReturn("Candidate A");

                CalendarDetailInViewCandidateProjection projection2 = mock(CalendarDetailInViewCandidateProjection.class);
                when(projection2.getDate()).thenReturn(LocalDate.now().plusDays(2));
                when(projection2.getCalendarId()).thenReturn(1L);
                when(projection2.getHrContactId()).thenReturn(7L);
                when(projection2.getJobAdId()).thenReturn(10L);
                when(projection2.getJobAdTitle()).thenReturn("Java Dev");
                when(projection2.getCalendarType()).thenReturn(CalendarType.INTERVIEW_ONLINE.name());
                when(projection2.getTimeFrom()).thenReturn(LocalTime.of(9, 30));
                when(projection2.getTimeTo()).thenReturn(LocalTime.of(10, 0));
                when(projection2.getCandidateInfoId()).thenReturn(3L);
                when(projection2.getFullName()).thenReturn("Candidate B");

                when(calendarRepository.filterViewGeneral(any(), eq(100L), isNull(), eq(99L), eq(99L), eq(99L)))
                        .thenReturn(List.of(projection1, projection2));
                when(restTemplateClient.getUsersByIds(anyList())).thenReturn(Map.of(7L, UserDto.builder().id(7L).fullName("HR A").build()));

                CalendarFilterRequest req = new CalendarFilterRequest();
                req.setParticipationType(ParticipationType.JOINED_BY_ME);
                Instant dateFrom = Instant.parse("2026-04-15T00:00:00Z");
                Instant dateTo = Instant.parse("2026-04-20T23:59:00Z");
                req.setDateFrom(dateFrom);
                req.setDateTo(dateTo);

                List<CalendarFilterResponse> response = calendarService.filterViewGeneral(req);

                assertThat(req.getDateFrom()).isEqualTo(dateFrom.plusSeconds(7 * 3600L));
                assertThat(req.getDateTo()).isEqualTo(dateTo.plusSeconds(7 * 3600L));
                assertThat(response).hasSize(1);
                assertThat(response.get(0).getDetails()).hasSize(2);
                assertThat(response.get(0).getDetails().get(0).getCandidateInfos()).hasSize(1);
                assertThat(response.get(0).getDetails().get(1).getCandidateInfos()).hasSize(1);

                ArgumentCaptor<CalendarFilterRequest> requestCaptor = ArgumentCaptor.forClass(CalendarFilterRequest.class);
                verify(calendarRepository).filterViewGeneral(requestCaptor.capture(), eq(100L), isNull(), eq(99L), eq(99L), eq(99L));
                assertThat(requestCaptor.getValue().getDateFrom()).isEqualTo(dateFrom.plusSeconds(7 * 3600L));
                assertThat(requestCaptor.getValue().getDateTo()).isEqualTo(dateTo.plusSeconds(7 * 3600L));
            }
        }
    }
}

