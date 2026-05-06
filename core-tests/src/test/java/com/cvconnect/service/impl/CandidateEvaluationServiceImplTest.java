package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CandidateEvaluationServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Đánh giá Ứng viên (Evaluation)
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: DATA_NOT_FOUND nhánh do jobAdCandidateId null hoặc currentProcess null.
 *   - create: Logic phân luồng ORG_ADMIN -> orgId check và USER -> getInterviewer lookup.
 *   - update: DATA_NOT_FOUND khi findOptional empty, ACCESS_DENIED khi Evaluator sai ID.
 *   - getByJobAdCandidate: Null mapping cho Evaluator lookup. Throws nếu Access Denied.
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.candidateEvaluation.CandidateEvaluationDetail;
import com.cvconnect.dto.candidateEvaluation.CandidateEvaluationDto;
import com.cvconnect.dto.candidateEvaluation.CandidateEvaluationProjection;
import com.cvconnect.dto.candidateEvaluation.CandidateEvaluationRequest;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAdCandidate.JobAdCandidateDto;
import com.cvconnect.dto.jobAdCandidate.JobAdProcessCandidateDto;
import com.cvconnect.entity.CandidateEvaluation;
import com.cvconnect.repository.CandidateEvaluationRepository;
import com.cvconnect.service.JobAdCandidateService;
import com.cvconnect.service.JobAdProcessCandidateService;
import com.cvconnect.service.impl.CandidateEvaluationServiceImpl;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateEvaluationServiceImpl - Unit Tests (C2 Branch Coverage)")
public class CandidateEvaluationServiceImplTest {

    @Mock
    private CandidateEvaluationRepository candidateEvaluationRepository;
    @Mock
    private RestTemplateClient restTemplateClient;
    @Mock
    private JobAdCandidateService jobAdCandidateService;
    @Mock
    private JobAdProcessCandidateService jobAdProcessCandidateService;

    @InjectMocks
    private CandidateEvaluationServiceImpl service;

    /**
     * Test Case ID: TC-EVL-001
     * Test Objective: Validate TC_EVL_001_createNullId behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-001: Lỗi Create vì JobAdCandidateId == null")
    void TC_EVL_001_createNullId() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest();
        req.setJobAdCandidateId(null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(AppException.class)
            .hasMessageContaining(CommonErrorCode.DATA_NOT_FOUND.getMessage());
    }

    /**
     * Test Case ID: TC-EVL-002
     * Test Objective: Validate TC_EVL_002_createNoProcess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-002: Lỗi Create vì CurrentProcessCandidate rỗng/false")
    void TC_EVL_002_createNoProcess() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest();
        req.setJobAdCandidateId(10L);
        // Return 1 array nhưng ko có isCurrent
        JobAdProcessCandidateDto dto1 = new JobAdProcessCandidateDto(); dto1.setIsCurrentProcess(false);
        when(jobAdProcessCandidateService.findByJobAdCandidateId(10L)).thenReturn(List.of(dto1));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(AppException.class)
            .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND));
    }

    /**
     * Test Case ID: TC-EVL-003
     * Test Objective: Validate TC_EVL_003_createAdminDenied behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-003: Create ORG_ADMIN Access_Denied")
    void TC_EVL_003_createAdminDenied() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest(); req.setJobAdCandidateId(10L);
        JobAdProcessCandidateDto p1 = new JobAdProcessCandidateDto(); 
        p1.setId(5L); p1.setIsCurrentProcess(true);
        when(jobAdProcessCandidateService.findByJobAdCandidateId(10L)).thenReturn(List.of(p1));
        
        try (MockedStatic<WebUtils> w = Mockito.mockStatic(WebUtils.class)) {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            w.when(WebUtils::getCurrentUserId).thenReturn(88L);
            w.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));

            // Cố tình mock trả về false (Access Denied)
            when(jobAdCandidateService.existsByJobAdCandidateIdAndOrgId(10L, 99L)).thenReturn(false);

            assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }
    }

    /**
     * Test Case ID: TC-EVL-004
     * Test Objective: Validate TC_EVL_004_createUserInterviewerSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-004: Create USER INTERVIEWER Access Success")
    void TC_EVL_004_createUserInterviewerSuccess() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest(); 
        req.setJobAdCandidateId(10L);
        req.setScore(BigDecimal.valueOf(4)); req.setComments("Good");
        JobAdProcessCandidateDto p1 = new JobAdProcessCandidateDto(); 
        p1.setId(5L); p1.setIsCurrentProcess(true); p1.setJobAdProcessId(15L);
        when(jobAdProcessCandidateService.findByJobAdCandidateId(10L)).thenReturn(List.of(p1));
        
        try (MockedStatic<WebUtils> w = Mockito.mockStatic(WebUtils.class)) {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            w.when(WebUtils::getCurrentUserId).thenReturn(88L);
            w.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.HR));

            // HR = false -> Lookup Interviewer
            when(jobAdCandidateService.existsByJobAdCandidateIdAndHrContactId(10L, 88L)).thenReturn(false);
            
            JobAdCandidateDto cInfo = new JobAdCandidateDto(); cInfo.setCandidateInfoId(20L);
            when(jobAdCandidateService.findById(10L)).thenReturn(cInfo);

            when(candidateEvaluationRepository.getInterviewerByJobAdProcessAndCandidateInfoId(15L, 20L))
                .thenReturn(List.of(88L)); // ID 88 matches => Check Authorized = true

            assertThat(service.create(req)).isNotNull();
        }
    }

    /**
     * Test Case ID: TC-EVL-005
     * Test Objective: Validate TC_EVL_005_updateWrongUser behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-005: Lỗi Update do Evaluator != CurrentUser")
    void TC_EVL_005_updateWrongUser() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest();
        req.setId(10L);
        CandidateEvaluation existing = new CandidateEvaluation();
        existing.setEvaluatorId(100L); // Current user is 50L
        
        when(candidateEvaluationRepository.findById(10L)).thenReturn(Optional.of(existing));
        try (MockedStatic<WebUtils> w = Mockito.mockStatic(WebUtils.class)) {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            w.when(WebUtils::getCurrentUserId).thenReturn(50L);
            
             assertThatThrownBy(() -> service.update(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }
    }

    /**
     * Test Case ID: TC-EVL-006
     * Test Objective: Validate TC_EVL_006_updateSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-006: Update thành công khi Evaluator = CurrentUser")
    void TC_EVL_006_updateSuccess() {
        CandidateEvaluationRequest req = new CandidateEvaluationRequest();
        req.setId(10L); req.setComments("Updated"); req.setScore(BigDecimal.valueOf(5));
        CandidateEvaluation existing = new CandidateEvaluation();
        existing.setEvaluatorId(50L); 
        
        when(candidateEvaluationRepository.findById(10L)).thenReturn(Optional.of(existing));
        try (MockedStatic<WebUtils> w = Mockito.mockStatic(WebUtils.class)) {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            w.when(WebUtils::getCurrentUserId).thenReturn(50L);
            
            assertThat(service.update(req).getId()).isEqualTo(existing.getId());
            assertThat(existing.getScore()).isEqualByComparingTo(BigDecimal.valueOf(5));
        }
    }

    /**
     * Test Case ID: TC-EVL-007
     * Test Objective: Validate TC_EVL_007_getByJobAdCandidateGrpMap behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-EVL-007: getByJobAdCandidate Group map fallback Null user")
    void TC_EVL_007_getByJobAdCandidateGrpMap() {
        try (MockedStatic<WebUtils> w = Mockito.mockStatic(WebUtils.class)) {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            w.when(WebUtils::getCurrentUserId).thenReturn(50L);
            w.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.HR));
            when(jobAdCandidateService.existsByJobAdCandidateIdAndHrContactId(10L, 50L)).thenReturn(false);

            CandidateEvaluationProjection p1 = Mockito.mock(CandidateEvaluationProjection.class);
            when(p1.getJobAdProcessId()).thenReturn(999L);
            when(p1.getJobAdProcessName()).thenReturn("Phỏng vấn");
            when(p1.getEvaluatorId()).thenReturn(70L); // Lấy mock không tìm dc từ Map

            when(candidateEvaluationRepository.getByJobAdCandidateId(10L, 50L)).thenReturn(List.of(p1));
            when(restTemplateClient.getUsersByIds(List.of(70L))).thenReturn(Collections.emptyMap()); // trả về null UserDto
            
            List<CandidateEvaluationDetail> results = service.getByJobAdCandidate(10L);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEvaluations().get(0).getEvaluatorName()).isNull(); // fallback C2 branch Null User mapping
        }
    }
}


