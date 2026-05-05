package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CandidateSummaryOrgServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Lưu Đánh giá Tóm tắt Ứng Viên của Tổ Chức
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - saveSummary: Check ROLE Org_Admin (bỏ qua check userid) vs HR (check the userid).
 *   - saveSummary: Lỗi Access Denied rẽ nhánh.
 *   - saveSummary: Tạo Mới Summary (khi null) và Tái Sử Dụng Summary (khi tồn tại).
 *   - saveSummary: Rẽ nhánh cập nhật Level (tồn tại/không tồn tại) và Skill.
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.candidateSummaryOrg.CandidateSummaryOrgRequest;
import com.cvconnect.dto.level.LevelDto;
import com.cvconnect.entity.CandidateSummaryOrg;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.CandidateSummaryOrgRepository;
import com.cvconnect.service.JobAdCandidateService;
import com.cvconnect.service.LevelService;
import com.cvconnect.service.impl.CandidateSummaryOrgServiceImpl;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateSummaryOrgServiceImpl - Unit Tests (C2 Branch Coverage)")
public class CandidateSummaryOrgServiceImplTest {

    @Mock private CandidateSummaryOrgRepository candidateSummaryOrgRepository;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private JobAdCandidateService jobAdCandidateService;
    @Mock private LevelService levelService;

    @InjectMocks
    private CandidateSummaryOrgServiceImpl summaryService;

    @Nested
    @DisplayName("1. saveSummary() Branch Coverage")
    class SaveSummaryTest {

        /**
         * Test Case ID: TC-CSO-001
         * Test Objective: Validate TC_CSO_001_accessDenied_HR_NotAuthorized behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CSO-001: Lỗi Access Denied (HR lưu cho process KHÔNG thuộc quyền của mình)")
        void TC_CSO_001_accessDenied_HR_NotAuthorized() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of("HR"));
                webUtils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(1L);

                // HR -> parameter hrContactId passes 99L.
                when(jobAdCandidateService.checkCandidateInfoInOrg(5L, 1L, 99L)).thenReturn(false);

                CandidateSummaryOrgRequest req = new CandidateSummaryOrgRequest();
                req.setCandidateInfoId(5L);

                assertThatThrownBy(() -> summaryService.saveSummary(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
            }
        }

        /**
         * Test Case ID: TC-CSO-002
         * Test Objective: Validate TC_CSO_002_levelNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CSO-002: Thất bại khi truyền LevelId không tồn tại")
        void TC_CSO_002_levelNotFound() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                // Role ADMIN -> hrContactId pass 'null'
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                when(restTemplateClient.validOrgMember()).thenReturn(1L);
                
                when(jobAdCandidateService.checkCandidateInfoInOrg(5L, 1L, null)).thenReturn(true);
                
                // Return existing summary
                when(candidateSummaryOrgRepository.findByCandidateInfoIdAndOrgId(5L, 1L)).thenReturn(new CandidateSummaryOrg());

                CandidateSummaryOrgRequest req = new CandidateSummaryOrgRequest();
                req.setCandidateInfoId(5L);
                req.setLevelId(999L); // ID ảo
                when(levelService.getById(999L)).thenReturn(null);

                assertThatThrownBy(() -> summaryService.saveSummary(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.LEVEL_NOT_FOUND));
            }
        }

        /**
         * Test Case ID: TC-CSO-003
         * Test Objective: Validate TC_CSO_003_saveNew_WithLevelAndSkill behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CSO-003: Lưu thành công Tạo mới Summary + Check nhánh Skill và Level hợp lệ")
        void TC_CSO_003_saveNew_WithLevelAndSkill() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                // Role ADMIN
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of(Constants.RoleCode.ORG_ADMIN));
                when(restTemplateClient.validOrgMember()).thenReturn(1L);
                
                when(jobAdCandidateService.checkCandidateInfoInOrg(5L, 1L, null)).thenReturn(true);
                
                // Return Null summary => Tạo record nhánh mới
                when(candidateSummaryOrgRepository.findByCandidateInfoIdAndOrgId(5L, 1L)).thenReturn(null);
                
                CandidateSummaryOrgRequest req = new CandidateSummaryOrgRequest();
                req.setCandidateInfoId(5L);
                req.setLevelId(10L); 
                req.setSkill("Java, OOP");

                LevelDto lvDto = new LevelDto(); lvDto.setId(10L);
                when(levelService.getById(10L)).thenReturn(lvDto);

                IDResponse<Long> res = summaryService.saveSummary(req);

                assertThat(res).isNotNull();
                verify(candidateSummaryOrgRepository).save(any(CandidateSummaryOrg.class));
            }
        }
        
        /**
         * Test Case ID: TC-CSO-004
         * Test Objective: Validate TC_CSO_004_saveExisting_NoLevelNoSkill behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-CSO-004: Lưu thành công Tái sử dụng Summary (Không update nhánh level và skill nếu rỗng)")
        void TC_CSO_004_saveExisting_NoLevelNoSkill() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                // Role MEMBER -> Falls to HR Contact Id setup path
                webUtils.when(WebUtils::getCurrentRole).thenReturn(List.of("MEMBER"));
                webUtils.when(WebUtils::getCurrentUserId).thenReturn(99L);
                when(restTemplateClient.validOrgMember()).thenReturn(1L);
                
                when(jobAdCandidateService.checkCandidateInfoInOrg(5L, 1L, 99L)).thenReturn(true);
                
                // Trả về một exist root
                CandidateSummaryOrg root = new CandidateSummaryOrg(); root.setId(555L);
                when(candidateSummaryOrgRepository.findByCandidateInfoIdAndOrgId(5L, 1L)).thenReturn(root);

                CandidateSummaryOrgRequest req = new CandidateSummaryOrgRequest();
                req.setCandidateInfoId(5L);
                req.setLevelId(null);
                req.setSkill(null);

                IDResponse<Long> res = summaryService.saveSummary(req);

                // ID của return response phải chính là 555L
                assertThat(res.getId()).isEqualTo(555L);
                verify(candidateSummaryOrgRepository).save(any(CandidateSummaryOrg.class));
            }
        }
    }
}


