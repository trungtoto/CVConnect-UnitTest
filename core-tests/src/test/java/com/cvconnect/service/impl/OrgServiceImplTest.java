package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: OrgServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.OrgServiceImpl}
 *
 * BAO PHỦ CÁC LUỒNG:
 *   - createOrg (validations, relationships, exceed limits)
 *   - updateOrg (logo, cover photo, info)
 *   - retrieve (findById, getOrgInfo, getFeaturedOrg, etc.)
 *   - status toggle & rollback (changeStatusActive)
 *   - deleteOrg
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.attachFile.AttachFileDto;
import com.cvconnect.dto.common.NotificationDto;
import com.cvconnect.dto.industry.IndustryDto;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAd.JobAdDto;
import com.cvconnect.dto.org.*;
import com.cvconnect.entity.Organization;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.OrgRepository;
import com.cvconnect.service.*;
import com.cvconnect.service.impl.OrgServiceImpl;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import nmquan.commonlib.utils.DateUtils;
import nmquan.commonlib.utils.KafkaUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgServiceImpl - Unit Tests")
public class OrgServiceImplTest {

    @Mock private OrgRepository orgRepository;
    @Mock private OrgAddressService orgAddressService;
    @Mock private OrgIndustryService orgIndustryService;
    @Mock private IndustryService industryService;
    @Mock private AttachFileService attachFileService;
    @Mock private RestTemplateClient restTemplateClient;
    @Mock private JobAdService jobAdService;
    @Mock private KafkaUtils kafkaUtils;
    @Mock private FailedRollbackService failedRollbackService;

    @InjectMocks
    private OrgServiceImpl orgService;

    // --- Helpers ---
    private Organization taoOrg(Long id, String name) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        return org;
    }

    private IndustryDto taoIndustryDto(Long id) {
        IndustryDto d = new IndustryDto();
        d.setId(id);
        d.setIsActive(true);
        return d;
    }

    private AttachFileDto taoAttachFile(Long id, String url) {
        AttachFileDto d = new AttachFileDto();
        d.setId(id);
        d.setSecureUrl(url);
        return d;
    }

    private MultipartFile taoFile(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    // =========================================================================

    @Nested
    @DisplayName("1. createOrg()")
    class CreateOrgTest {

        /**
         * Test Case ID: TC-ORG-001
         * Test Objective: Validate TC_ORG_001_createSuccess_FullData behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-001: Tạo Organization thành công, đủ logo + bìa (branch cover)")
        void TC_ORG_001_createSuccess_FullData() {
            MultipartFile[] files = {taoFile("logo.jpg"), taoFile("cover.jpg")};
            when(attachFileService.uploadFile(files)).thenReturn(List.of(100L, 200L));

            OrganizationRequest req = new OrganizationRequest();
            req.setName("TechCorp");
            req.setCreatedBy("99");
            req.setIndustryIds(List.of(1L));
            OrgAddressRequest addr = new OrgAddressRequest();
            addr.setHeadquarter(true);
            req.setAddresses(List.of(addr));

            when(industryService.findByIds(List.of(1L))).thenReturn(List.of(taoIndustryDto(1L)));

            when(orgRepository.save(any(Organization.class))).thenAnswer(inv -> {
                Organization o = inv.getArgument(0);
                o.setId(10L);
                return o;
            });

            IDResponse<Long> res = orgService.createOrg(req, files);

            assertThat(res.getId()).isEqualTo(10L);
            verify(orgRepository).updateCreatedBy(10L, "99");
            verify(orgIndustryService).createIndustries(any());
            verify(orgAddressService).createAddresses(any());
        }

        /**
         * Test Case ID: TC-ORG-002
         * Test Objective: Validate TC_ORG_002_fileNotImage behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-002: Thất bại do lỗi định dạng file không phải ảnh")
        void TC_ORG_002_fileNotImage() {
            MultipartFile[] files = { new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1,2}) };
            OrganizationRequest req = new OrganizationRequest();

            assertThatThrownBy(() -> orgService.createOrg(req, files))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.IMAGE_FILE_INVALID));
        }

        /**
         * Test Case ID: TC-ORG-003
         * Test Objective: Validate TC_ORG_003_exceedIndustryLimit behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-003: Thất bại do kích thước mảng vượt quá MAX_INDUSTRY (C2 branch limits)")
        void TC_ORG_003_exceedIndustryLimit() {
            MultipartFile[] files = {taoFile("logo.jpg")};
            when(attachFileService.uploadFile(files)).thenReturn(List.of(100L));

            OrganizationRequest req = new OrganizationRequest();
            req.setIndustryIds(Collections.nCopies(Constants.MAX_INDUSTRY_PER_ORG + 1, 1L)); 
            // Vượt gới hạn Constants.MAX_INDUSTRY_PER_ORG 

            assertThatThrownBy(() -> orgService.createOrg(req, files))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.INDUSTRY_EXCEED_LIMIT));
        }

        /**
         * Test Case ID: TC-ORG-004
         * Test Objective: Validate TC_ORG_004_industryNotFoundOrInactive behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-004: Thất bại do không tìm thấy đủ Industry/Industry bị khóa (C2 branch sizes not equal)")
        void TC_ORG_004_industryNotFoundOrInactive() {
            MultipartFile[] files = {taoFile("logo.jpg")};
            when(attachFileService.uploadFile(files)).thenReturn(List.of(100L));

            OrganizationRequest req = new OrganizationRequest();
            req.setIndustryIds(List.of(1L, 2L));

            // Chỉ mock 1 industry active trả về 
            when(industryService.findByIds(List.of(1L, 2L))).thenReturn(List.of(taoIndustryDto(1L)));

            assertThatThrownBy(() -> orgService.createOrg(req, files))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.INDUSTRY_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-ORG-005
         * Test Objective: Validate TC_ORG_005_createSuccess_NoOptionals behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-005: Thành công với request không truyền address, industry, createdBy (C2 null/empty branch)")
        void TC_ORG_005_createSuccess_NoOptionals() {
            MultipartFile[] files = {taoFile("logo.jpg")};
            when(attachFileService.uploadFile(files)).thenReturn(List.of(100L));

            OrganizationRequest req = new OrganizationRequest();
            req.setIndustryIds(null);
            req.setAddresses(null);
            req.setCreatedBy(null);

            when(orgRepository.save(any(Organization.class))).thenAnswer(inv -> {
                Organization o = inv.getArgument(0);
                o.setId(10L);
                return o;
            });

            IDResponse<Long> res = orgService.createOrg(req, files);

            assertThat(res.getId()).isEqualTo(10L);
            verify(orgRepository, never()).updateCreatedBy(anyLong(), anyString());
            verify(orgIndustryService, never()).createIndustries(any());
            verify(orgAddressService, never()).createAddresses(any());
        }
    }

    @Nested
    @DisplayName("2. Queries: findById, getOrgInfo, getOrgMapByIds, ...")
    class QueriesOrgTest {

        /**
         * Test Case ID: TC-ORG-006
         * Test Objective: Validate TC_ORG_006_findById_TraVeNull_ViEmpty behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_006_findById_TraVeNull_ViEmpty() {
            when(orgRepository.findById(99L)).thenReturn(Optional.empty());
            OrgDto result = orgService.findById(99L);
            assertThat(result).isNull();
        }

        /**
         * Test Case ID: TC-ORG-007
         * Test Objective: Validate TC_ORG_007_findById_TraVeDto behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_007_findById_TraVeDto() {
            when(orgRepository.findById(1L)).thenReturn(Optional.of(taoOrg(1L, "T1")));
            OrgDto result = orgService.findById(1L);
            assertThat(result.getId()).isEqualTo(1L);
        }

        /**
         * Test Case ID: TC-ORG-008
         * Test Objective: Validate TC_ORG_008_getOrgInfo_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_008_getOrgInfo_NotFound() {
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            when(orgRepository.findById(10L)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> orgService.getOrgInfo())
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ORG_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-ORG-009
         * Test Objective: Validate TC_ORG_009_getOrgInfo_SuccessWithFiles behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_009_getOrgInfo_SuccessWithFiles() {
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            Organization o = taoOrg(10L, "T10");
            o.setLogoId(11L);
            o.setCoverPhotoId(22L);
            when(orgRepository.findById(10L)).thenReturn(Optional.of(o));
            
            when(attachFileService.getAttachFiles(List.of(11L))).thenReturn(List.of(taoAttachFile(11L, "url-logo")));
            when(attachFileService.getAttachFiles(List.of(22L))).thenReturn(List.of(taoAttachFile(22L, "url-cover")));
            
            OrgDto dto = orgService.getOrgInfo();
            assertThat(dto.getLogoUrl()).isEqualTo("url-logo");
            assertThat(dto.getCoverPhotoUrl()).isEqualTo("url-cover");
        }

        /**
         * Test Case ID: TC-ORG-010
         * Test Objective: Validate TC_ORG_010_getOrgMapByIds_EmptyList behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_010_getOrgMapByIds_EmptyList() {
            Map<Long, OrgDto> map = orgService.getOrgMapByIds(Collections.emptyList());
            assertThat(map).isEmpty();
        }

        /**
         * Test Case ID: TC-ORG-011
         * Test Objective: Validate TC_ORG_011_getFeatureOrgOutside_EmptyDB behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_011_getFeatureOrgOutside_EmptyDB() {
            when(orgRepository.findFeaturedOrgs()).thenReturn(Collections.emptyList());
            List<OrgDto> res = orgService.getFeaturedOrgOutside();
            assertThat(res).isEmpty();
        }

        /**
         * Test Case ID: TC-ORG-012
         * Test Objective: Validate TC_ORG_011B_getFeaturedOrgOutside_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-011B: getFeaturedOrgOutside trả về org có logo/industry/address")
        void TC_ORG_011B_getFeaturedOrgOutside_Success() {
            OrgProjection prj = new OrgProjection() {
                public Long getId() { return 10L; }
                public Boolean getIsActive() { return true; }
                public Boolean getIsDeleted() { return false; }
                public String getCreatedBy() { return "admin"; }
                public String getUpdatedBy() { return "admin"; }
                public java.time.Instant getCreatedAt() { return java.time.Instant.now(); }
                public java.time.Instant getUpdatedAt() { return java.time.Instant.now(); }
                public String getName() { return "Org 10"; }
                public String getDescription() { return null; }
                public Long getLogoId() { return 11L; }
                public Long getCoverPhotoId() { return null; }
                public String getWebsite() { return null; }
                public Integer getStaffCountFrom() { return null; }
                public Integer getStaffCountTo() { return null; }
                public Long getNumOfJobAds() { return 2L; }
            };

            when(orgRepository.findFeaturedOrgs()).thenReturn(List.of(prj));
            when(attachFileService.getAttachFiles(List.of(11L))).thenReturn(List.of(taoAttachFile(11L, "logo-10")));
            when(industryService.getIndustriesByOrgId(10L)).thenReturn(List.of(taoIndustryDto(1L)));
            when(orgAddressService.getByOrgId(10L)).thenReturn(List.of(OrgAddressDto.builder().displayAddress("HN").build()));

            try (MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> mapper = mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                OrgDto dto = new OrgDto();
                dto.setId(10L);
                dto.setLogoId(11L);
                mapper.when(() -> nmquan.commonlib.utils.ObjectMapperUtils.convertToList(any(), eq(OrgDto.class)))
                        .thenReturn(List.of(dto));

                List<OrgDto> result = orgService.getFeaturedOrgOutside();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getLogoUrl()).isEqualTo("logo-10");
                assertThat(result.get(0).getIndustryList()).hasSize(1);
                assertThat(result.get(0).getAddresses()).hasSize(1);
            }
        }

        /**
         * Test Case ID: TC-ORG-013
         * Test Objective: Validate TC_ORG_010B_getOrgMapByIds_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-010B: getOrgMapByIds trả map có logoUrl")
        void TC_ORG_010B_getOrgMapByIds_Success() {
            Organization org = taoOrg(10L, "Org 10");
            org.setLogoId(11L);
            when(orgRepository.findAllById(List.of(10L))).thenReturn(List.of(org));
            when(attachFileService.getAttachFiles(List.of(11L))).thenReturn(List.of(taoAttachFile(11L, "logo-10")));

            try (MockedStatic<nmquan.commonlib.utils.ObjectMapperUtils> mapper = mockStatic(nmquan.commonlib.utils.ObjectMapperUtils.class)) {
                OrgDto dto = new OrgDto();
                dto.setId(10L);
                dto.setLogoId(11L);
                mapper.when(() -> nmquan.commonlib.utils.ObjectMapperUtils.convertToList(any(), eq(OrgDto.class)))
                        .thenReturn(List.of(dto));

                Map<Long, OrgDto> map = orgService.getOrgMapByIds(List.of(10L));
                assertThat(map).containsKey(10L);
                assertThat(map.get(10L).getLogoUrl()).isEqualTo("logo-10");
            }
        }

        /**
         * Test Case ID: TC-ORG-014
         * Test Objective: Validate TC_ORG_013B_getOrgInfoOutside_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-013B: getOrgInfoOutside success có logo, cover, industry, address")
        void TC_ORG_013B_getOrgInfoOutside_Success() {
            Organization org = taoOrg(10L, "Org 10");
            org.setLogoId(11L);
            org.setCoverPhotoId(22L);
            when(orgRepository.findById(10L)).thenReturn(Optional.of(org));
            when(attachFileService.getAttachFiles(List.of(11L))).thenReturn(List.of(taoAttachFile(11L, "logo-10")));
            when(attachFileService.getAttachFiles(List.of(22L))).thenReturn(List.of(taoAttachFile(22L, "cover-10")));
            when(industryService.getIndustriesByOrgId(10L)).thenReturn(List.of(taoIndustryDto(1L)));
            when(orgAddressService.getByOrgId(10L)).thenReturn(List.of(OrgAddressDto.builder().displayAddress("HN").build()));

            OrgDto result = orgService.getOrgInfoOutside(10L);
            assertThat(result.getLogoUrl()).isEqualTo("logo-10");
            assertThat(result.getCoverPhotoUrl()).isEqualTo("cover-10");
            assertThat(result.getIndustryList()).hasSize(1);
            assertThat(result.getAddresses()).hasSize(1);
        }

        /**
         * Test Case ID: TC-ORG-015
         * Test Objective: Validate TC_ORG_013C_getOrgDetails_Delegate behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-013C: getOrgDetails delegate tới getOrgInfoOutside")
        void TC_ORG_013C_getOrgDetails_Delegate() {
            Organization org = taoOrg(20L, "Org 20");
            when(orgRepository.findById(20L)).thenReturn(Optional.of(org));
            when(industryService.getIndustriesByOrgId(20L)).thenReturn(List.of());
            when(orgAddressService.getByOrgId(20L)).thenReturn(List.of());

            OrgDto result = orgService.getOrgDetails(20L);
            assertThat(result.getId()).isEqualTo(20L);
        }

        /**
         * Test Case ID: TC-ORG-016
         * Test Objective: Validate TC_ORG_013D_getOrgInfoOutside_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-013D: getOrgInfoOutside fail khi org không tồn tại")
        void TC_ORG_013D_getOrgInfoOutside_NotFound() {
            when(orgRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orgService.getOrgInfoOutside(404L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ORG_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-ORG-017
         * Test Objective: Validate TC_ORG_012_getOrgByJobAd_NotFoundJobAd behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_012_getOrgByJobAd_NotFoundJobAd() {
            when(jobAdService.findById(1L)).thenReturn(null);
            assertThatThrownBy(() -> orgService.getOrgByJobAd(1L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.JOB_AD_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-ORG-018
         * Test Objective: Validate TC_ORG_013_getOrgByJobAd_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_013_getOrgByJobAd_Success() {
            JobAdDto d = new JobAdDto();
            d.setOrgId(10L);
            d.setHrContactId(999L);
            when(jobAdService.findById(1L)).thenReturn(d);
            
            Organization o = taoOrg(10L, "T1");
            when(orgRepository.findById(10L)).thenReturn(Optional.of(o));

            UserDto u = new UserDto(); u.setId(999L);
            when(restTemplateClient.getUser(999L)).thenReturn(u);

            OrgDto ret = orgService.getOrgByJobAd(1L);
            assertThat(ret.getHrContact().getId()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("3. Update Operations (Info, Logo, Cover)")
    class UpdateOrgTest {

        /**
         * Test Case ID: TC-ORG-019
         * Test Objective: Validate TC_ORG_014_updateOrgLogo_SuccessAndClearOld behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_014_updateOrgLogo_SuccessAndClearOld() {
            MultipartFile file = taoFile("newLogo.jpg");
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            
            Organization org = taoOrg(10L, "T10");
            org.setLogoId(111L); // old id
            when(orgRepository.findById(10L)).thenReturn(Optional.of(org));

            when(attachFileService.uploadFile(any())).thenReturn(List.of(222L));

            orgService.updateOrgLogo(file);

            assertThat(org.getLogoId()).isEqualTo(222L);
            verify(attachFileService).deleteByIds(List.of(111L));
        }

        /**
         * Test Case ID: TC-ORG-020
         * Test Objective: Validate TC_ORG_015_updateOrgCover_SuccessNoOld behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_015_updateOrgCover_SuccessNoOld() {
            MultipartFile file = taoFile("newCover.png");
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            
            Organization org = taoOrg(10L, "T10");
            org.setCoverPhotoId(null);
            when(orgRepository.findById(10L)).thenReturn(Optional.of(org));

            when(attachFileService.uploadFile(any())).thenReturn(List.of(333L));

            orgService.updateOrgCoverPhoto(file);

            assertThat(org.getCoverPhotoId()).isEqualTo(333L);
            verify(attachFileService, never()).deleteByIds(any());
        }

        /**
         * Test Case ID: TC-ORG-021
         * Test Objective: Validate TC_ORG_016_updateOrgInfo_ClearIndustriesIfNull behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_016_updateOrgInfo_ClearIndustriesIfNull() {
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            Organization org = taoOrg(10L, "T10");
            when(orgRepository.findById(10L)).thenReturn(Optional.of(org));

            OrganizationRequest req = new OrganizationRequest();
            req.setIndustryIds(null);

            orgService.updateOrgInfo(req);

            verify(orgIndustryService).deleteByOrgId(10L); 
        }

        /**
         * Test Case ID: TC-ORG-022
         * Test Objective: Validate TC_ORG_017_updateOrgInfo_InsertMissingAndRemoveRedundant behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_017_updateOrgInfo_InsertMissingAndRemoveRedundant() {
            when(restTemplateClient.validOrgMember()).thenReturn(10L);
            Organization org = taoOrg(10L, "T10");
            when(orgRepository.findById(10L)).thenReturn(Optional.of(org));

            OrganizationRequest req = new OrganizationRequest();
            req.setIndustryIds(List.of(2L, 3L)); // Delete 1, keep 2, add 3

            IndustryDto oldInd1 = taoIndustryDto(1L);
            IndustryDto oldInd2 = taoIndustryDto(2L);
            when(industryService.getIndustriesByOrgId(10L)).thenReturn(List.of(oldInd1, oldInd2));

            orgService.updateOrgInfo(req);

            // Delete 1
            verify(orgIndustryService).deleteByIndustryIdsAndOrgId(List.of(1L), 10L);
            // Insert 3
            ArgumentCaptor<List<OrgIndustryDto>> captor = ArgumentCaptor.forClass(List.class);
            verify(orgIndustryService).createIndustries(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getIndustryId()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("4. Export / Filter")
    class ExportOrgTest {
        /**
         * Test Case ID: TC-ORG-023
         * Test Objective: Validate TC_ORG_018_exportOrg behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_018_exportOrg() {
            OrgFilterRequest req = new OrgFilterRequest();
            req.setCreatedAtEnd(java.time.Instant.now());
            req.setUpdatedAtEnd(java.time.Instant.now());

            // Dung anonymous class day du tat ca methods tu BaseRepositoryDto + OrgProjection
            OrgProjection prj = new OrgProjection() {
                public Long getId() { return 1L; }
                public Boolean getIsActive() { return true; }
                public Boolean getIsDeleted() { return false; }
                public String getCreatedBy() { return "test"; }
                public String getUpdatedBy() { return "test"; }
                public java.time.Instant getCreatedAt() { return java.time.Instant.now(); }
                public java.time.Instant getUpdatedAt() { return java.time.Instant.now(); }
                public String getName() { return "A"; }
                public String getDescription() { return null; }
                public Long getLogoId() { return null; }
                public Long getCoverPhotoId() { return null; }
                public String getWebsite() { return null; }
                public Integer getStaffCountFrom() { return 10; }
                public Integer getStaffCountTo() { return 50; }
                public Long getNumOfJobAds() { return 0L; }
            };
            Page<OrgProjection> page = new PageImpl<>(List.of(prj));
            when(orgRepository.filterOrgs(any(), any(Pageable.class))).thenReturn((Page)page);

            InputStreamResource res = orgService.exportOrg(req);
            assertThat(res).isNotNull();
        }

        /**
         * Test Case ID: TC-ORG-024
         * Test Objective: Validate TC_ORG_018B_exportOrg_StaffVariants behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-018B: exportOrg với các biến thể staffCount")
        void TC_ORG_018B_exportOrg_StaffVariants() {
            OrgFilterRequest req = new OrgFilterRequest();
            
            // Dùng anonymous class để ObjectMapperUtils.convertToList không bị null
            OrgProjection p1 = new OrgProjection() {
                public Long getId() { return 100L; }
                public String getName() { return "Org 100"; }
                public Integer getStaffCountFrom() { return 100; }
                public Integer getStaffCountTo() { return null; }
                public Boolean getIsActive() { return true; }
                public Boolean getIsDeleted() { return false; }
                public String getCreatedBy() { return "admin"; }
                public String getUpdatedBy() { return "admin"; }
                public java.time.Instant getCreatedAt() { return java.time.Instant.now(); }
                public java.time.Instant getUpdatedAt() { return java.time.Instant.now(); }
                public String getDescription() { return null; }
                public Long getLogoId() { return null; }
                public Long getCoverPhotoId() { return null; }
                public String getWebsite() { return null; }
                public Long getNumOfJobAds() { return 0L; }
            };
            
            OrgProjection p2 = new OrgProjection() {
                public Long getId() { return 200L; }
                public String getName() { return "Org 200"; }
                public Integer getStaffCountFrom() { return null; }
                public Integer getStaffCountTo() { return 500; }
                public Boolean getIsActive() { return false; }
                public Boolean getIsDeleted() { return false; }
                public String getCreatedBy() { return "admin"; }
                public String getUpdatedBy() { return "admin"; }
                public java.time.Instant getCreatedAt() { return java.time.Instant.now(); }
                public java.time.Instant getUpdatedAt() { return java.time.Instant.now(); }
                public String getDescription() { return null; }
                public Long getLogoId() { return null; }
                public Long getCoverPhotoId() { return null; }
                public String getWebsite() { return null; }
                public Long getNumOfJobAds() { return 0L; }
            };

            Page<OrgProjection> page = new PageImpl<>(List.of(p1, p2));
            when(orgRepository.filterOrgs(any(), any(Pageable.class))).thenReturn((Page)page);

            InputStreamResource res = orgService.exportOrg(req);
            assertThat(res).isNotNull();
        }
    }

    @Nested
    @DisplayName("5. deleteOrg()")
    class DeleteOrgTest {
        /**
         * Test Case ID: TC-ORG-025
         * Test Objective: Validate TC_ORG_019_deleteSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_019_deleteSuccess() {
            Organization o = taoOrg(1L, "T");
            o.setLogoId(11L);
            when(orgRepository.findById(1L)).thenReturn(Optional.of(o));

            orgService.deleteOrg(FailedRollbackOrgCreation.builder().orgId(1L).build());

            verify(orgRepository).delete(o);
            verify(attachFileService).deleteByIds(List.of(11L));
        }

        /**
         * Test Case ID: TC-ORG-026
         * Test Objective: Validate TC_ORG_020_deleteIgnoreIfNotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        void TC_ORG_020_deleteIgnoreIfNotFound() {
            when(orgRepository.findById(1L)).thenReturn(Optional.empty());
            orgService.deleteOrg(FailedRollbackOrgCreation.builder().orgId(1L).build());
            verify(orgRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("6. changeStatusActive() - ROLLBACK and KAFKA")
    class ChangeStatusActiveTest {

        /**
         * Test Case ID: TC-ORG-027
         * Test Objective: Validate TC_ORG_021_happyPath behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-021: Happy Path gửi Kafka cho Admin (Branch True)")
        void TC_ORG_021_happyPath() {
            ChangeStatusActiveRequest req = ChangeStatusActiveRequest.builder().ids(List.of(1L)).active(true).build();
            
            Organization o = taoOrg(1L, "T1");
            when(orgRepository.findAllById(List.of(1L))).thenReturn(List.of(o));

            UserDto admin = new UserDto(); admin.setId(99L);
            when(restTemplateClient.getUserByRoleCodeOrg(anyString(), eq(1L))).thenReturn(List.of(admin));

            orgService.changeStatusActive(req);

            verify(orgRepository).updateStatus(List.of(1L), true);
            verify(jobAdService).updateJobAdStatusByOrgIds(List.of(1L), true);
            verify(restTemplateClient).updateAccountStatusByOrgIds(req);
            verify(kafkaUtils, times(1)).sendWithJson(anyString(), any(NotificationDto.class));
        }

        /**
         * Test Case ID: TC-ORG-028
         * Test Objective: Validate TC_ORG_022_rollbackWhenFail behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-022: Nhảy vào branch ROLLBACK nếu update status/gửi Kafka thất bại")
        void TC_ORG_022_rollbackWhenFail() {
            ChangeStatusActiveRequest req = ChangeStatusActiveRequest.builder().ids(List.of(1L)).active(true).build();
            Organization o = taoOrg(1L, "T1");
            
            // Giả lập exception ở KafkaUtils (đại diện cho luồng lỗi lúc sau updateAccountStatus)
            when(orgRepository.findAllById(List.of(1L))).thenReturn(List.of(o));
            UserDto admin = new UserDto(); admin.setId(99L);
            when(restTemplateClient.getUserByRoleCodeOrg(anyString(), eq(1L))).thenReturn(List.of(admin));
            
            doThrow(new RuntimeException("Kafka error")).when(kafkaUtils).sendWithJson(anyString(), any());

            assertThatThrownBy(() -> orgService.changeStatusActive(req)).isInstanceOf(RuntimeException.class);

            // Xác nhận rằng hàm try-catch-rollback đã được gá»Âi
            verify(restTemplateClient).rollbackUpdateAccountStatusByOrgIds(any());
        }
        
        /**
         * Test Case ID: TC-ORG-029
         * Test Objective: Validate TC_ORG_023_saveFailedRollback_IfRollbackFails behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ORG-023: Lưu Fallback Cứng nếu RestClient Rollback cũng lỗi -> Full C2 Branch")
        void TC_ORG_023_saveFailedRollback_IfRollbackFails() {
            ChangeStatusActiveRequest req = ChangeStatusActiveRequest.builder().ids(List.of(1L)).active(true).build();
            // Lỗi ở restTemplateClient updateAccountStatus (lúc này isUpdateAccount = false) nên ko thèm rollback
            // Vậy ta giả lập lỗi ở findAllById tức là sau khi updateAccountStatus ok (isUpdateAccount = true)
            doAnswer(invocation -> {
                return null;
            }).when(restTemplateClient).updateAccountStatusByOrgIds(req);
            
            when(orgRepository.findAllById(any())).thenThrow(new RuntimeException("DB Connection Lost"));

            // Luồng Catch được trigger -> cố gắng gá»Âi rollback qua REST -> giả lập lỗi REST
            doThrow(new RuntimeException("Microservice unavailable"))
                .when(restTemplateClient).rollbackUpdateAccountStatusByOrgIds(any());

            assertThatThrownBy(() -> orgService.changeStatusActive(req)).isInstanceOf(RuntimeException.class);

            // Kết quả là phải trigger failedRollbackService
            verify(failedRollbackService, times(1)).save(any());
        }
    }
}


