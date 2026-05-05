package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: DashboardServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho DashboardServiceImpl (Thống kê)
 *
 * BAO PHỦ CÁC LUỒNG:
 *   - getPercentPassed (Luồng count = 0, tính % làm tròn)
 *   - getCandidateApplyMost (Candidate bị xoá -> N/A)
 *   - getPercentEliminatedReason (Gộp lý do thứ 7+ vào Others)
 *   - getJobAdByCareer (Format lương triệu/N/A)
 *   - getOrgAdminDashboardOverview (So sánh tháng trước - Index)
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.dashboard.admin.DashboardFilter;
import nmquan.commonlib.constant.CommonConstants;
import com.cvconnect.dto.dashboard.admin.DashboardPercentPassedDto;
import com.cvconnect.dto.dashboard.admin.DashboardApplyMostDto;
import com.cvconnect.dto.dashboard.admin.DashboardEliminatedReasonDto;
import com.cvconnect.dto.dashboard.org.OrgAdminDashboardFilter;
import com.cvconnect.dto.dashboard.org.OrgAdminDashboardOverviewDto;
import com.cvconnect.entity.JobAd;
import com.cvconnect.entity.Organization;
import com.cvconnect.dto.jobAdCandidate.JobAdCandidateProjection;
import com.cvconnect.dto.candidateInfoApply.CandidateInfoApplyProjection;
import com.cvconnect.repository.DashboardRepository;
import com.cvconnect.service.AttachFileService;
import com.cvconnect.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl - Unit Tests")
public class DashboardServiceImplTest {

    @Mock private DashboardRepository dashboardRepository;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private AttachFileService attachFileService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Nested
    @DisplayName("1. System Admin Stats")
    class SystemAdminStatsTest {

        /**
         * Test Case ID: TC-DASH-001
         * Test Objective: Validate TC_DASH_001_getPercentPassed_DivisionByZero behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-001: Tính phần trăm ứng viên đạt (Percent Passed) - Case 0 ứng viên")
        void TC_DASH_001_getPercentPassed_DivisionByZero() {
            DashboardFilter filter = new DashboardFilter();
            filter.setStartTime(Instant.parse("2024-01-01T00:00:00Z"));
            filter.setEndTime(Instant.parse("2024-01-31T23:59:59Z"));

            lenient().when(dashboardRepository.getByApplyDate(any(DashboardFilter.class))).thenReturn(new ArrayList<>()); // Apply = 0
            lenient().when(dashboardRepository.getByOnboard(any(DashboardFilter.class))).thenReturn(new ArrayList<>());

            List<DashboardPercentPassedDto> result = dashboardService.getPercentPassed(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPercent()).isEqualTo(0.0);
        }

        /**
         * Test Case ID: TC-DASH-002
         * Test Objective: Validate TC_DASH_002_getCandidateApplyMost_CandidateDeleted behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-002: Thống kê ứng viên nộp nhiều nhất - Case Candidate bị xóa (N/A)")
        void TC_DASH_002_getCandidateApplyMost_CandidateDeleted() {
            DashboardFilter filter = new DashboardFilter();
            CandidateInfoApplyProjection p = mock(CandidateInfoApplyProjection.class);
            when(p.getCandidateId()).thenReturn(1L);
            when(p.getNumOfApply()).thenReturn(5L);
            when(dashboardRepository.getCandidateApplyMost(any(DashboardFilter.class))).thenReturn(List.of(p));
            
            // Rest client returns empty map (user not found)
            when(restTemplateClient.getUsersByIds(anyList())).thenReturn(Map.of());

            List<DashboardApplyMostDto> result = dashboardService.getCandidateApplyMost(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCandidateName()).isEqualTo("N/A");
        }

        /**
         * Test Case ID: TC-DASH-003
         * Test Objective: Validate TC_DASH_003_getPercentEliminatedReason_GroupingOthers behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-003: Thống kê lý do loại - Case Gộp lý do thứ 7 trở đi")
        void TC_DASH_003_getPercentEliminatedReason_GroupingOthers() {
            DashboardFilter filter = new DashboardFilter();
            List<JobAdCandidateProjection> projections = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                JobAdCandidateProjection p = mock(JobAdCandidateProjection.class);
                when(p.getEliminateReasonType()).thenReturn("REASON_" + i);
                when(p.getNumOfApply()).thenReturn(1L);
                projections.add(p);
            }
            when(dashboardRepository.getEliminatedReasonData(any(DashboardFilter.class))).thenReturn(projections);

            List<DashboardEliminatedReasonDto> result = dashboardService.getPercentEliminatedReason(filter);

            // 7 lý do đầu + 1 lý do "Các lý do khác" = 8
            assertThat(result).hasSize(8);
            assertThat(result.get(7).getEliminateReason().getDescription()).isEqualTo("Các lý do khác");
            assertThat(result.get(7).getNumberOfEliminated()).isEqualTo(3L); // 10 - 7 = 3
        }
    }

    @Nested
    @DisplayName("2. Org Admin Stats")
    class OrgAdminStatsTest {

        /**
         * Test Case ID: TC-DASH-004
         * Test Objective: Validate TC_DASH_004_getOrgAdminDashboardOverview_WithComparison behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-004: Dashboard Overview - So sánh với tháng trước (Index Change)")
        void TC_DASH_004_getOrgAdminDashboardOverview_WithComparison() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            // Thiết lập startTime là ngày đầu tháng để trigger logic so sánh
            LocalDate firstDay = LocalDate.now(ZoneId.of("UTC")).withDayOfMonth(1);
            filter.setStartTime(firstDay.atStartOfDay(ZoneId.of("UTC")).toInstant());
            filter.setEndTime(Instant.now());

            lenient().when(restTemplateClient.validOrgMember()).thenReturn(1L);
            
            // Mock số liệu tháng này
            lenient().when(dashboardRepository.numberOfJobAds(any(OrgAdminDashboardFilter.class))).thenReturn(20L);
            lenient().when(dashboardRepository.numberOfApplications(any(OrgAdminDashboardFilter.class))).thenReturn(100L);
            lenient().when(dashboardRepository.numberOfOnboard(any(OrgAdminDashboardFilter.class))).thenReturn(10L);

            // Mock số liệu tháng trước (được gọi lại trong cùng 1 method với filter đã modify)
            // Lần gọi đầu: trả về 20, 100, 10. Lần gọi sau (tháng trước): trả về 10, 50, 5.
            lenient().when(dashboardRepository.numberOfJobAds(any(OrgAdminDashboardFilter.class)))
                .thenReturn(20L) // current
                .thenReturn(10L); // previous
            lenient().when(dashboardRepository.numberOfApplications(any(OrgAdminDashboardFilter.class)))
                .thenReturn(100L) // current
                .thenReturn(50L);  // previous
            lenient().when(dashboardRepository.numberOfOnboard(any(OrgAdminDashboardFilter.class)))
                .thenReturn(10L) // current
                .thenReturn(5L);  // previous

            OrgAdminDashboardOverviewDto resp = dashboardService.getOrgAdminDashboardOverview(filter);

            assertThat(resp.getIndeTotalJobAds()).contains("+100.0%");
            assertThat(resp.getIndeTotalApplications()).contains("+100.0%");
        }
    }

    @Nested
    @DisplayName("3. Format/Visual Logic")
    class VisualLogicTest {

        /**
         * Test Case ID: TC-DASH-005
         * Test Objective: Validate TC_DASH_005_getJobAdByCareer_SalaryFormatting behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-005: Format lương trong getJobAdByCareer")
        void TC_DASH_005_getJobAdByCareer_SalaryFormatting() {
            DashboardFilter filter = new DashboardFilter();
            Object[] row1 = new Object[]{1L, "Career 1", 10L, new java.math.BigDecimal(15500000)}; // 15.5M
            Object[] row2 = new Object[]{2L, "Career 2", 5L, null}; // N/A
            when(dashboardRepository.getJobAdByCareer(any(DashboardFilter.class))).thenReturn(List.of(row1, row2));

            List<com.cvconnect.dto.dashboard.admin.DashboardJobAdByCareerDto> result = dashboardService.getJobAdByCareer(filter);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAvgSalaryStr()).isEqualTo("15.5 triệu");
            assertThat(result.get(1).getAvgSalaryStr()).isEqualTo("N/A");
        }

        /**
         * Test Case ID: TC-DASH-006
         * Test Objective: Validate TC_DASH_006_getOrgFeatured_LogoHandling behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-006: Thống kê Org nổi bật - Xử lý Logo")
        void TC_DASH_006_getOrgFeatured_LogoHandling() {
            DashboardFilter filter = new DashboardFilter();
            Object[] row = new Object[]{1L, "Org 1", 500L, 10L, 100L, 5L}; // logoId = 500
            org.springframework.data.domain.Page<Object[]> page = new org.springframework.data.domain.PageImpl<Object[]>(java.util.List.<Object[]>of(row));
            
            when(dashboardRepository.getOrgFeatured(any(DashboardFilter.class), any())).thenReturn(page);
            
            com.cvconnect.dto.attachFile.AttachFileDto file = new com.cvconnect.dto.attachFile.AttachFileDto();
            file.setSecureUrl("https://s3/logo.png");
            when(attachFileService.getAttachFiles(List.of(500L))).thenReturn(List.of(file));

            nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.dashboard.admin.DashboardOrgFeaturedDto> result = dashboardService.getOrgFeatured(filter);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getOrgLogo()).isEqualTo("https://s3/logo.png");
        }

        /**
         * Test Case ID: TC-DASH-007
         * Test Objective: Validate TC_DASH_007_getOrgStaffSize_EmptyData behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-007: getOrgStaffSize - Empty data trả về danh sách rỗng")
        void TC_DASH_007_getOrgStaffSize_EmptyData() {
            when(dashboardRepository.getOrgStaffSize()).thenReturn(List.of());

            List<com.cvconnect.dto.dashboard.admin.DashboardOrgStaffSizeDto> result = dashboardService.getOrgStaffSize();

            assertThat(result).isEmpty();
        }

        /**
         * Test Case ID: TC-DASH-008
         * Test Objective: Validate TC_DASH_008_getOrgStaffSize_Mapping behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-008: getOrgStaffSize - Mapping dữ liệu thành công")
        void TC_DASH_008_getOrgStaffSize_Mapping() {
            when(dashboardRepository.getOrgStaffSize()).thenReturn(List.<Object[]>of(new Object[]{"10-49", 4L}));

            List<com.cvconnect.dto.dashboard.admin.DashboardOrgStaffSizeDto> result = dashboardService.getOrgStaffSize();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStaffSize()).isEqualTo("10-49");
            assertThat(result.get(0).getNumberOfOrgs()).isEqualTo(4L);
        }

        /**
         * Test Case ID: TC-DASH-009
         * Test Objective: Validate TC_DASH_009_getOrgFeatured_DefaultSort_NoLogoId behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-009: getOrgFeatured - Default sort, không có logoId")
        void TC_DASH_009_getOrgFeatured_DefaultSort_NoLogoId() {
            DashboardFilter filter = new DashboardFilter();
            filter.setSortBy(CommonConstants.DEFAULT_SORT_BY);

            Object[] row = new Object[]{7L, "Org No Logo", null, null, null, null};
            org.springframework.data.domain.Page<Object[]> page = new org.springframework.data.domain.PageImpl<Object[]>(List.<Object[]>of(row));
            when(dashboardRepository.getOrgFeatured(any(DashboardFilter.class), any())).thenReturn(page);

            nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.dashboard.admin.DashboardOrgFeaturedDto> result = dashboardService.getOrgFeatured(filter);

            assertThat(filter.getSortBy()).isEqualTo("numberOfJobAds");
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getOrgLogo()).isNull();
            assertThat(result.getData().get(0).getNumberOfJobAds()).isEqualTo(0L);
            assertThat(result.getData().get(0).getNumberOfApplications()).isEqualTo(0L);
            assertThat(result.getData().get(0).getNumberOfOnboarded()).isEqualTo(0L);
            verifyNoInteractions(attachFileService);
        }

        /**
         * Test Case ID: TC-DASH-010
         * Test Objective: Validate TC_DASH_010_getOrgFeatured_LogoIdButEmptyAttachFiles behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-010: getOrgFeatured - Có logoId nhưng attach files rỗng")
        void TC_DASH_010_getOrgFeatured_LogoIdButEmptyAttachFiles() {
            DashboardFilter filter = new DashboardFilter();
            Object[] row = new Object[]{9L, "Org Empty Logo", 901L, 3L, 20L, 1L};
            org.springframework.data.domain.Page<Object[]> page = new org.springframework.data.domain.PageImpl<Object[]>(List.<Object[]>of(row));
            when(dashboardRepository.getOrgFeatured(any(DashboardFilter.class), any())).thenReturn(page);
            when(attachFileService.getAttachFiles(List.of(901L))).thenReturn(List.of());

            nmquan.commonlib.dto.response.FilterResponse<com.cvconnect.dto.dashboard.admin.DashboardOrgFeaturedDto> result = dashboardService.getOrgFeatured(filter);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getOrgLogo()).isNull();
        }

        /**
         * Test Case ID: TC-DASH-011
         * Test Objective: Validate TC_DASH_011_getJobAdFeatured_DefaultSort behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-011: getJobAdFeatured - Default sort chuyển sang numberOfApplications")
        void TC_DASH_011_getJobAdFeatured_DefaultSort() {
            DashboardFilter filter = new DashboardFilter();
            filter.setSortBy(CommonConstants.FALLBACK_SORT_BY);

            Object[] row = new Object[]{11L, "Java Dev", 22L, "CV Org", 100L, 15L};
            org.springframework.data.domain.Page<Object[]> page = new org.springframework.data.domain.PageImpl<Object[]>(List.<Object[]>of(row));
            when(dashboardRepository.getJobAdFeatured(any(DashboardFilter.class), any())).thenReturn(page);

            List<com.cvconnect.dto.jobAd.JobAdDto> result = dashboardService.getJobAdFeatured(filter);

            assertThat(filter.getSortBy()).isEqualTo("numberOfApplications");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(11L);
            assertThat(result.get(0).getOrg().getName()).isEqualTo("CV Org");
        }
    }

    @Nested
    @DisplayName("4. Additional Org Admin Branches")
    class OrgAdminAdditionalBranchesTest {

        /**
         * Test Case ID: TC-DASH-012
         * Test Objective: Validate TC_DASH_012_orgOverview_NotFirstDayOfMonth behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-012: Org overview không phải đầu tháng -> không tính index tháng trước")
        void TC_DASH_012_orgOverview_NotFirstDayOfMonth() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            LocalDate secondDay = LocalDate.now(ZoneId.of("UTC")).withDayOfMonth(1).plusDays(1);
            filter.setStartTime(secondDay.atStartOfDay(ZoneId.of("UTC")).toInstant());
            filter.setEndTime(Instant.now());

            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            when(dashboardRepository.numberOfJobAds(any(OrgAdminDashboardFilter.class))).thenReturn(8L);
            when(dashboardRepository.numberOfOpenJobAds(any(OrgAdminDashboardFilter.class))).thenReturn(3L);
            when(dashboardRepository.numberOfApplications(any(OrgAdminDashboardFilter.class))).thenReturn(0L);
            when(dashboardRepository.numberOfCandidateInProcess(any(OrgAdminDashboardFilter.class))).thenReturn(5L);
            when(dashboardRepository.numberOfOnboard(any(OrgAdminDashboardFilter.class))).thenReturn(2L);

            OrgAdminDashboardOverviewDto result = dashboardService.getOrgAdminDashboardOverview(filter);

            assertThat(result.getTotalJobAds()).isEqualTo(8L);
            assertThat(result.getTotalOpenJobAds()).isEqualTo(3L);
            assertThat(result.getPercentPassed()).isEqualTo(0.0);
            assertThat(result.getIndeTotalJobAds()).isNull();
            assertThat(result.getIndeTotalApplications()).isNull();
            assertThat(result.getIndePercentPassed()).isNull();
        }

        /**
         * Test Case ID: TC-DASH-013
         * Test Objective: Validate TC_DASH_013_getOrgAdminJobAdByHr_HrNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-013: getOrgAdminJobAdByHr - HR không tồn tại trong map => N/A")
        void TC_DASH_013_getOrgAdminJobAdByHr_HrNotFound() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            when(dashboardRepository.getOrgAdminJobAdByHr(any(OrgAdminDashboardFilter.class)))
                    .thenReturn(List.<Object[]>of(new Object[]{55L, 2L, 9L, 1L}));
            when(restTemplateClient.getUsersByIds(List.of(55L))).thenReturn(Map.of());

            List<com.cvconnect.dto.dashboard.org.OrgAdminDashboardJobAdByHrDto> result = dashboardService.getOrgAdminJobAdByHr(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getHrName()).isEqualTo("N/A");
            assertThat(result.get(0).getJobAdCount()).isEqualTo(2L);
        }

        /**
         * Test Case ID: TC-DASH-014
         * Test Objective: Validate TC_DASH_014_getOrgAdminPercentPassed behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-014: getOrgAdminPercentPassed tính theo tháng")
        void TC_DASH_014_getOrgAdminPercentPassed() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            filter.setStartTime(Instant.parse("2024-01-01T00:00:00Z"));
            filter.setEndTime(Instant.parse("2024-01-31T23:59:59Z"));

            JobAdCandidateProjection apply = mock(JobAdCandidateProjection.class);
            when(apply.getApplyDate()).thenReturn(Instant.parse("2024-01-10T00:00:00Z"));
            JobAdCandidateProjection onboard = mock(JobAdCandidateProjection.class);
            when(onboard.getApplyDate()).thenReturn(Instant.parse("2024-01-11T00:00:00Z"));

            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(dashboardRepository.getByApplyDate(any(OrgAdminDashboardFilter.class))).thenReturn(List.of(apply));
            when(dashboardRepository.getByOnboard(any(OrgAdminDashboardFilter.class))).thenReturn(List.of(onboard));

            List<com.cvconnect.dto.dashboard.admin.DashboardPercentPassedDto> result = dashboardService.getOrgAdminPercentPassed(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNumberOfApplications()).isEqualTo(1L);
            assertThat(result.get(0).getNumberOfPassed()).isEqualTo(1L);
            assertThat(result.get(0).getPercent()).isEqualTo(100.0);
        }

        /**
         * Test Case ID: TC-DASH-015
         * Test Objective: Validate TC_DASH_015_getOrgAdminJobAdByDepartment behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-015: getOrgAdminJobAdByDepartment mapping dữ liệu")
        void TC_DASH_015_getOrgAdminJobAdByDepartment() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(dashboardRepository.getOrgAdminJobAdByDepartment(any(OrgAdminDashboardFilter.class)))
                    .thenReturn(List.<Object[]>of(new Object[]{"IT", "Information Tech", 3L, 2L}));

            List<com.cvconnect.dto.dashboard.org.OrgAdminDashboardJobAdByDepartmentDto> result = dashboardService.getOrgAdminJobAdByDepartment(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartmentCode()).isEqualTo("IT");
            assertThat(result.get(0).getJobAdCount()).isEqualTo(3L);
            assertThat(result.get(0).getTotalOnboarded()).isEqualTo(2L);
        }

        /**
         * Test Case ID: TC-DASH-016
         * Test Objective: Validate TC_DASH_016_getOrgAdminPassByLevel behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-016: getOrgAdminPassByLevel mapping dữ liệu")
        void TC_DASH_016_getOrgAdminPassByLevel() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            when(restTemplateClient.validOrgMember()).thenReturn(1L);
            when(dashboardRepository.getOrgAdminPassByLevel(any(OrgAdminDashboardFilter.class)))
                    .thenReturn(List.<Object[]>of(new Object[]{"Junior", 4L}));

            List<com.cvconnect.dto.dashboard.org.OrgAdminDashboardPassByLevelDto> result = dashboardService.getOrgAdminPassByLevel(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLevel()).isEqualTo("Junior");
            assertThat(result.get(0).getTotalOnboarded()).isEqualTo(4L);
        }

        /**
         * Test Case ID: TC-DASH-017
         * Test Objective: Validate TC_DASH_017_getOrgAdminPercentEliminatedReason behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-017: getOrgAdminPercentEliminatedReason lọc null và gộp others")
        void TC_DASH_017_getOrgAdminPercentEliminatedReason() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            List<JobAdCandidateProjection> projections = new ArrayList<>();
            JobAdCandidateProjection nullReason = mock(JobAdCandidateProjection.class);
            when(nullReason.getEliminateReasonType()).thenReturn(null);
            projections.add(nullReason);

            for (int i = 0; i < 9; i++) {
                JobAdCandidateProjection p = mock(JobAdCandidateProjection.class);
                when(p.getEliminateReasonType()).thenReturn("REASON_" + i);
                when(p.getNumOfApply()).thenReturn(1L);
                projections.add(p);
            }

            when(dashboardRepository.getEliminatedReasonData(any(OrgAdminDashboardFilter.class))).thenReturn(projections);

            List<com.cvconnect.dto.dashboard.admin.DashboardEliminatedReasonDto> result =
                    dashboardService.getOrgAdminPercentEliminatedReason(filter);

            assertThat(result).hasSize(8);
            assertThat(result.get(7).getEliminateReason().getDescription()).isEqualTo("Các lý do khác");
            assertThat(result.get(7).getNumberOfEliminated()).isEqualTo(2L);
        }

        /**
         * Test Case ID: TC-DASH-018
         * Test Objective: Validate TC_DASH_018_getOrgAdminJobAdFeatured_fallbackSort behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-018: getOrgAdminJobAdFeatured fallback sort")
        void TC_DASH_018_getOrgAdminJobAdFeatured_fallbackSort() {
            OrgAdminDashboardFilter filter = new OrgAdminDashboardFilter();
            filter.setSortBy(CommonConstants.DEFAULT_SORT_BY);
            when(restTemplateClient.validOrgMember()).thenReturn(1L);

            Object[] row = new Object[]{101L, "Java", 50L, 6L};
            org.springframework.data.domain.Page<Object[]> page =
                    new org.springframework.data.domain.PageImpl<Object[]>(List.<Object[]>of(row));
            when(dashboardRepository.getJobAdFeatured(any(OrgAdminDashboardFilter.class), any())).thenReturn(page);

            List<com.cvconnect.dto.jobAd.JobAdDto> result = dashboardService.getOrgAdminJobAdFeatured(filter);

            assertThat(filter.getSortBy()).isEqualTo("numberOfApplications");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(101L);
            assertThat(result.get(0).getNumberOfViews()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("5. Additional System Admin Branches")
    class SystemAdminAdditionalBranchesTest {

        /**
         * Test Case ID: TC-DASH-019
         * Test Objective: Validate TC_DASH_019_getJobAdByLevel behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-019: getJobAdByLevel mapping dữ liệu")
        void TC_DASH_019_getJobAdByLevel() {
            DashboardFilter filter = new DashboardFilter();
            when(dashboardRepository.getJobAdByLevel(any(DashboardFilter.class)))
                    .thenReturn(List.<Object[]>of(new Object[]{1L, "Junior", 7L}));

            List<com.cvconnect.dto.dashboard.admin.DashboardJobAdByLevelDto> result = dashboardService.getJobAdByLevel(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLevel().getName()).isEqualTo("Junior");
            assertThat(result.get(0).getNumberOfJobAds()).isEqualTo(7L);
        }

        /**
         * Test Case ID: TC-DASH-020
         * Test Objective: Validate TC_DASH_020_getJobAdByTime behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-020: getJobAdByTime đếm theo tháng")
        void TC_DASH_020_getJobAdByTime() {
            DashboardFilter filter = new DashboardFilter();
            filter.setStartTime(Instant.parse("2024-01-01T00:00:00Z"));
            filter.setEndTime(Instant.parse("2024-01-31T23:59:59Z"));

            JobAd ad = new JobAd();
            ad.setCreatedAt(Instant.parse("2024-01-20T00:00:00Z"));
            when(dashboardRepository.getJobAdByTime(any(DashboardFilter.class))).thenReturn(List.of(ad));

            List<com.cvconnect.dto.dashboard.admin.DashboardJobAdByTimeDto> result = dashboardService.getJobAdByTime(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNumberOfJobAds()).isEqualTo(1L);
        }

        /**
         * Test Case ID: TC-DASH-021
         * Test Objective: Validate TC_DASH_021_getNewOrgByTime behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-021: getNewOrgByTime đếm theo tháng")
        void TC_DASH_021_getNewOrgByTime() {
            DashboardFilter filter = new DashboardFilter();
            filter.setStartTime(Instant.parse("2024-01-01T00:00:00Z"));
            filter.setEndTime(Instant.parse("2024-01-31T23:59:59Z"));

            Organization org = new Organization();
            org.setCreatedAt(Instant.parse("2024-01-15T00:00:00Z"));
            when(dashboardRepository.getNewOrgByTime(any(DashboardFilter.class))).thenReturn(List.of(org));

            List<com.cvconnect.dto.dashboard.admin.DashboardNewOrgByTimeDto> result = dashboardService.getNewOrgByTime(filter);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNumberOfOrgs()).isEqualTo(1L);
        }

        /**
         * Test Case ID: TC-DASH-022
         * Test Objective: Validate TC_DASH_022_splitByMonth_multiMonths behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-DASH-022: splitByMonth với khoảng qua nhiều tháng")
        void TC_DASH_022_splitByMonth_multiMonths() {
            Map<String, com.cvconnect.dto.dashboard.DateRange> ranges = dashboardService.splitByMonth(
                    Instant.parse("2024-01-15T00:00:00Z"),
                    Instant.parse("2024-03-10T23:59:59Z")
            );

            assertThat(ranges).hasSize(3);
            assertThat(ranges).containsKeys("T1-2024", "T2-2024", "T3-2024");
        }
    }
}


