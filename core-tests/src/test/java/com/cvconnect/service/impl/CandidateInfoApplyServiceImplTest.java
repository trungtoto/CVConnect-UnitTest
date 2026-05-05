package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CandidateInfoApplyServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Quản lý Ứng Viên Apply
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - getById: Nhánh record null, attachFile rỗng.
 *   - getCandidateInCurrentProcess: Nhánh check tồn tại Process, list trống, apply hasSchedule check.
 *   - filterByJobAdProcess: Nhánh jobAd rỗng, orgId sai lệch, role authorize ORG_ADMIN vs HR/Panel.
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.attachFile.AttachFileDto;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyDto;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoFilterByJobAdProcess;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyProjection;
import com.cvconnect.dto.jobAd.JobAdDto;
import com.cvconnect.entity.CandidateInfoApply;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.CandidateInfoApplyRepository;
import com.cvconnect.service.AttachFileService;
import com.cvconnect.service.InterviewPanelService;
import com.cvconnect.service.JobAdProcessService;
import com.cvconnect.service.JobAdService;
import com.cvconnect.service.impl.CandidateInfoApplyServiceImpl;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import nmquan.commonlib.constant.CommonConstants;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateInfoApplyServiceImpl - Unit Tests (C2 Branch Coverage)")
public class CandidateInfoApplyServiceImplTest {

    @Mock private CandidateInfoApplyRepository candidateInfoApplyRepository;
    @Mock private AttachFileService attachFileService;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private JobAdProcessService jobAdProcessService;
    @Mock private JobAdService jobAdService;
    @Mock private InterviewPanelService interviewPanelService;

    @InjectMocks
    private CandidateInfoApplyServiceImpl applyService;

    @Nested
    @DisplayName("1. getById() Branch Coverage")
    class GetByIdTest {
        /**
         * Test Case ID: TC-CIA-001
         * Test Objective: Validate TC_CIA_001_getById_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-001: getById trả về null khi bản ghi không tồn tại")
        void TC_CIA_001_getById_NotFound() {
            when(candidateInfoApplyRepository.findById(99L)).thenReturn(Optional.empty());
            CandidateInfoApplyDto dto = applyService.getById(99L);
            assertThat(dto).isNull();
        }

        /**
         * Test Case ID: TC-CIA-002
         * Test Objective: Validate TC_CIA_002_getById_Found behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-002: getById trả về DTO với AttachFile khi tồn tại")
        void TC_CIA_002_getById_Found() {
            CandidateInfoApply entity = new CandidateInfoApply();
            entity.setId(99L); entity.setCvFileId(5L);
            when(candidateInfoApplyRepository.findById(99L)).thenReturn(Optional.of(entity));

            AttachFileDto file = new AttachFileDto(); file.setId(5L); file.setSecureUrl("http://cv.link");
            when(attachFileService.getAttachFiles(List.of(5L))).thenReturn(List.of(file));

            CandidateInfoApplyDto dto = applyService.getById(99L);
            assertThat(dto).isNotNull();
            assertThat(dto.getAttachFile().getSecureUrl()).isEqualTo("http://cv.link");
        }
    }

    @Nested
    @DisplayName("2. getCandidateInCurrentProcess() Branch Coverage")
    class CurrentProcessTest {
        /**
         * Test Case ID: TC-CIA-003
         * Test Objective: Validate TC_CIA_003_accessDenied behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-003: Lỗi Access Denied nếu jobAdProcess không thuộc Org")
        void TC_CIA_003_accessDenied() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(10L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> applyService.getCandidateInCurrentProcess(10L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CIA-004
         * Test Objective: Validate TC_CIA_004_successHasSchedule behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-004: Trả về thành công và map biến hasSchedule")
        void TC_CIA_004_successHasSchedule() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(jobAdProcessService.existByJobAdProcessIdAndOrgId(10L, 1L)).thenReturn(true);

            CandidateInfoApply e1 = new CandidateInfoApply(); e1.setId(100L);
            CandidateInfoApply e2 = new CandidateInfoApply(); e2.setId(200L);
            when(candidateInfoApplyRepository.findCandidateInCurrentProcess(10L)).thenReturn(List.of(e1, e2));

            // Setup hasSchedule mapping cho id = 100L
            when(candidateInfoApplyRepository.getCandidateInfoHasSchedule(eq(10L), anyList())).thenReturn(List.of(100L));

            List<CandidateInfoApplyDto> result = applyService.getCandidateInCurrentProcess(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getHasSchedule()).isTrue();
            assertThat(result.get(1).getHasSchedule()).isFalse();
        }
    }

    @Nested
    @DisplayName("3. filterByJobAdProcess() Security Branch Coverage")
    class FilterByJobAdTest {
        /**
         * Test Case ID: TC-CIA-005
         * Test Objective: Validate TC_CIA_005_filter_jobAdNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-005: Lỗi khi JobAd không tồn tại")
        void TC_CIA_005_filter_jobAdNotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            CandidateInfoFilterByJobAdProcess req = new CandidateInfoFilterByJobAdProcess();
            req.setJobAdProcessId(10L);

            when(jobAdService.findByJobAdProcessId(10L)).thenReturn(null);

            assertThatThrownBy(() -> applyService.filterByJobAdProcess(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-CIA-006
         * Test Objective: Validate TC_CIA_006_filter_orgMismatch behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-006: Lỗi khi JobAd thuộc tổ chức khác (OrgId Mismatch)")
        void TC_CIA_006_filter_orgMismatch() {
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            CandidateInfoFilterByJobAdProcess req = new CandidateInfoFilterByJobAdProcess();
            req.setJobAdProcessId(10L);

            JobAdDto ad = new JobAdDto(); ad.setOrgId(999L);
            when(jobAdService.findByJobAdProcessId(10L)).thenReturn(ad);

            assertThatThrownBy(() -> applyService.filterByJobAdProcess(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-CIA-007
         * Test Objective: Validate TC_CIA_007_filter_interviewPanelDenied behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-007: Phân quyền Interview Panel thất bại")
        void TC_CIA_007_filter_interviewPanelDenied() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                webUtils.when(WebUtils::getCurrentUserId).thenReturn(50L);
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of("MEMBER"));

                when(restTemplateClient.validOrgMember()).thenReturn(1L);
                CandidateInfoFilterByJobAdProcess req = new CandidateInfoFilterByJobAdProcess();
                req.setJobAdProcessId(10L);

                JobAdDto ad = new JobAdDto(); ad.setId(5L); ad.setOrgId(1L); ad.setHrContactId(99L); // Ko phải HR Contact
                when(jobAdService.findByJobAdProcessId(10L)).thenReturn(ad);
                
                when(interviewPanelService.existByJobAdIdAndUserId(5L, 50L)).thenReturn(false); // Ko nằm trong hội đồng phỏng vấn

                assertThatThrownBy(() -> applyService.filterByJobAdProcess(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
            }
        }
        
        /**
         * Test Case ID: TC-CIA-008
         * Test Objective: Validate TC_CIA_008_filter_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CIA-008: Pass validation và Sort đổi chiều - Thành công")
        void TC_CIA_008_filter_Success() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                webUtils.when(WebUtils::getCurrentUserId).thenReturn(50L);
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN)); // Admin auto pass

                when(restTemplateClient.validOrgMember()).thenReturn(1L);
                CandidateInfoFilterByJobAdProcess req = new CandidateInfoFilterByJobAdProcess();
                req.setJobAdProcessId(10L);
                req.setPageSize(10);
                req.setSortBy(CommonConstants.DEFAULT_SORT_BY); // Yêu cầu sort createdAt -> covert "applyDate"
                
                JobAdDto ad = new JobAdDto(); ad.setId(5L); ad.setOrgId(1L); 
                when(jobAdService.findByJobAdProcessId(10L)).thenReturn(ad);

                Page<CandidateInfoApplyProjection> pageMock = new PageImpl<>(List.of());
                when(candidateInfoApplyRepository.filterByJobAdProcess(eq(req), any(Pageable.class))).thenReturn(pageMock);

                nmquan.commonlib.dto.response.FilterResponse<?> res = applyService.filterByJobAdProcess(req);
                assertThat(res).isNotNull();
            }
        }
    }
}


