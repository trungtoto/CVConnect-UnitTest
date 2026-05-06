package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: OrgAddressServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Địa Chỉ Tổ Chức
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - Các hàm getBy...: Empty return branches, Data return branches (bao gồm logic mapping address string)
 *   - save: Deletes Request null, Lỗi ACCESS_DENIED (ID ko tồn tại trong DB), delete các DB IDs thiếu.
 *   - save: Lỗi ORG_ADDRESS_AT_LEAST_ONE sau khi save.
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.org.OrgAddressDto;
import com.cvconnect.dto.org.OrgAddressProjection;
import com.cvconnect.dto.org.OrgAddressRequest;
import com.cvconnect.entity.OrganizationAddress;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.OrgAddressRepository;
import com.cvconnect.service.impl.OrgAddressServiceImpl;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgAddressServiceImpl - Unit Tests (C2 Branch Coverage)")
public class OrgAddressServiceImplTest {

    @Mock
    private OrgAddressRepository orgAddressRepository;

    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private OrgAddressServiceImpl orgAddressService;

    @Nested
    @DisplayName("1. Basic Get and Address Formatting")
    class GetAndFormatTest {
        /**
         * Test Case ID: TC-OA-001
         * Test Objective: Validate TC_OA_001_getEmpty1 behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-001: getByOrgIdAndIds trả về List rỗng")
        void TC_OA_001_getEmpty1() {
            when(orgAddressRepository.findByOrgIdAndIdIn(1L, List.of(2L))).thenReturn(Collections.emptyList());
            assertThat(orgAddressService.getByOrgIdAndIds(1L, List.of(2L))).isEmpty();
        }

        /**
         * Test Case ID: TC-OA-002
         * Test Objective: Validate TC_OA_002_getAllFormatAddress behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-002: getAll xử lý StringJoiner hiển thị các level location")
        void TC_OA_002_getAllFormatAddress() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            OrganizationAddress o1 = new OrganizationAddress();
            o1.setId(10L); 
            o1.setDetailAddress("123 Street");
            o1.setWard("Ward X");
            o1.setDistrict("District Y");
            o1.setProvince("City Z");
            
            when(orgAddressRepository.findByOrgId(99L)).thenReturn(List.of(o1));
            
            List<OrgAddressDto> results = orgAddressService.getAll();
            
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDisplayAddress()).isEqualTo("123 Street, Ward X, District Y, City Z");
        }

        /**
         * Test Case ID: TC-OA-003
         * Test Objective: Validate TC_OA_003_getAllEmpty behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-003: getAll empty fallback branch")
        void TC_OA_003_getAllEmpty() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            when(orgAddressRepository.findByOrgId(99L)).thenReturn(Collections.emptyList());
            assertThat(orgAddressService.getAll()).isEmpty();
        }

        /**
         * Test Case ID: TC-OA-004
         * Test Objective: Validate TC_OA_004_getByIdNull behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-004: getById null check handling")
        void TC_OA_004_getByIdNull() {
            when(orgAddressRepository.findById(10L)).thenReturn(Optional.empty());
            assertThat(orgAddressService.getById(10L)).isNull();

            OrganizationAddress del = new OrganizationAddress(); del.setIsDeleted(true);
            when(orgAddressRepository.findById(11L)).thenReturn(Optional.of(del));
            assertThat(orgAddressService.getById(11L)).isNull(); // test entity.getIsDeleted == true branch
        }

        /**
         * Test Case ID: TC-OA-005
         * Test Objective: Validate TC_OA_005_getByIdFormat behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-005: Tồn tại Address thì ánh xạ formatter")
        void TC_OA_005_getByIdFormat() {
            OrganizationAddress addr = new OrganizationAddress(); 
            addr.setIsDeleted(false);
            addr.setDetailAddress("A");
            addr.setProvince("B");
            when(orgAddressRepository.findById(10L)).thenReturn(Optional.of(addr));
            
            OrgAddressDto dto = orgAddressService.getById(10L);
            assertThat(dto).isNotNull();
            assertThat(dto.getDisplayAddress()).isEqualTo("A, B"); // Check branch mapping some format skip
        }
    }

    @Nested
    @DisplayName("2. Save Branches & Map Operations")
    class AdvancedSaveTest {

        /**
         * Test Case ID: TC-OA-006
         * Test Objective: Validate TC_OA_006_saveEmptyReq behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-006: Save request rỗng nhưng có DB exist => Delete All IDs")
        void TC_OA_006_saveEmptyReq() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            OrganizationAddress o1 = new OrganizationAddress(); o1.setId(10L);
            // mock getAll
            when(orgAddressRepository.findByOrgId(99L)).thenReturn(List.of(o1));

            orgAddressService.save(Collections.emptyList());

            verify(orgAddressRepository).deleteByIds(List.of(10L));
            verify(orgAddressRepository, never()).saveAll(any());
        }

        /**
         * Test Case ID: TC-OA-007
         * Test Objective: Validate TC_OA_007_saveAccessDenied behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-007: Save Lỗi Truy Cập (ACCESS_DENIED) nếu reqId không thuộc DBIds")
        void TC_OA_007_saveAccessDenied() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            OrganizationAddress db1 = new OrganizationAddress(); db1.setId(5L);
            when(orgAddressRepository.findByOrgId(99L)).thenReturn(List.of(db1)); // DB ID contains [5]

            OrgAddressRequest req1 = new OrgAddressRequest(); req1.setId(9L); // Malicious ID
            
            assertThatThrownBy(() -> orgAddressService.save(List.of(req1)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-OA-008
         * Test Objective: Validate TC_OA_008_saveSuccessAndCheckMissingIds behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-008: Save thành công xóa ID cũ, thêm ID mới, và update Map")
        void TC_OA_008_saveSuccessAndCheckMissingIds() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            OrganizationAddress db1 = new OrganizationAddress(); db1.setId(5L);
            OrganizationAddress db2 = new OrganizationAddress(); db2.setId(8L);
            // Call 1 getAll
            when(orgAddressRepository.findByOrgId(99L))
                    .thenReturn(List.of(db1, db2)) // first call returns [5, 8]
                    .thenReturn(List.of(db1)); // second call for constraint check returns [5]

            // Update only ID 5, meaning 8 should be deleted.
            OrgAddressRequest req1 = new OrgAddressRequest(); req1.setId(5L); req1.setDetailAddress("H");
            
            orgAddressService.save(List.of(req1));

            // Should trigger delete [8L]
            verify(orgAddressRepository).deleteByIds(List.of(8L));
            // Should trigger saveAll
            verify(orgAddressRepository).saveAll(anyList());
        }

        /**
         * Test Case ID: TC-OA-009
         * Test Objective: Validate TC_OA_009_saveEmptyPostConditions behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-009: Save lỗi xóa hêt thì báo ORG_ADDRESS_AT_LEAST_ONE")
        void TC_OA_009_saveEmptyPostConditions() {
            when(restTemplateClient.validOrgMember()).thenReturn(99L);
            when(orgAddressRepository.findByOrgId(99L))
                    .thenReturn(List.of()) // db is empty
                    .thenReturn(List.of()); // after save the second block check fails

            OrgAddressRequest req1 = new OrgAddressRequest(); req1.setId(null); // Just inserting new
            
            assertThatThrownBy(() -> orgAddressService.save(List.of(req1)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ORG_ADDRESS_AT_LEAST_ONE));
        }
    }

    @Nested
    @DisplayName("3. Get Projection Data")
    class GetProjections {
        /**
         * Test Case ID: TC-OA-010
         * Test Objective: Validate TC_OA_010_emptyProjectionsMap behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-010: Grouping Mapping OrgAddressByJobAdIds (Empty)")
        void TC_OA_010_emptyProjectionsMap() {
            when(orgAddressRepository.findByJobAdIdIn(any())).thenReturn(Collections.emptyList());
            Map<Long, List<OrgAddressDto>> map = orgAddressService.getOrgAddressByJobAdIds(List.of(1L));
            assertThat(map).isEmpty();
        }

        /**
         * Test Case ID: TC-OA-011
         * Test Objective: Validate TC_OA_011_getProjectionsGrouped behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-011: Grouping Mapping Address from Object Projections")
        void TC_OA_011_getProjectionsGrouped() {
            OrgAddressProjection proj = Mockito.mock(OrgAddressProjection.class);
            when(proj.getJobAdId()).thenReturn(100L);
            when(proj.getProvince()).thenReturn("Hanoi");

            when(orgAddressRepository.findByJobAdIdIn(List.of(100L))).thenReturn(List.of(proj));

            Map<Long, List<OrgAddressDto>> result = orgAddressService.getOrgAddressByJobAdIds(List.of(100L));
            
            assertThat(result).hasSize(1);
            assertThat(result.get(100L).get(0).getProvince()).isEqualTo("Hanoi");
            assertThat(result.get(100L).get(0).getDisplayAddress()).isEqualTo("Hanoi"); // Ensure formatter ran
        }

        /**
         * Test Case ID: TC-OA-012
         * Test Objective: Validate TC_OA_012_mapOrg behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-OA-012: Map OrgAddress loop builder fallback")
        void TC_OA_012_mapOrg() {
            OrganizationAddress addr = new OrganizationAddress(); addr.setOrgId(50L); addr.setDistrict("D1");
            when(orgAddressRepository.findByOrgIds(List.of(50L))).thenReturn(List.of(addr));

            Map<Long, List<OrgAddressDto>> res = orgAddressService.getMapOrgAddressByOrgIds(List.of(50L));
            assertThat(res.get(50L).get(0).getDisplayAddress()).isEqualTo("D1");
        }
    }
}


