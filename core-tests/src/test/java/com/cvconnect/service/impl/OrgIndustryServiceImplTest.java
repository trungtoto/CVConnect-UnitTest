package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: OrgIndustryServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Ngành Nghề Tổ Chức
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create, delete trực tiếp repository wrapper.
 * ============================================================
 */

import com.cvconnect.dto.org.OrgIndustryDto;
import com.cvconnect.repository.OrgIndustryRepository;
import com.cvconnect.service.impl.OrgIndustryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgIndustryServiceImpl - Unit Tests (C2 Branch Coverage)")
public class OrgIndustryServiceImplTest {

    @Mock
    private OrgIndustryRepository orgIndustryRepository;

    @InjectMocks
    private OrgIndustryServiceImpl orgIndustryService;

    /**
     * Test Case ID: TC-OI-001
     * Test Objective: Validate testCreateIndustries behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-OI-001: Convert list & lưu thông tin Industry")
    void testCreateIndustries() {
        OrgIndustryDto dto = new OrgIndustryDto();
        dto.setIndustryId(1L);

        orgIndustryService.createIndustries(List.of(dto));

        verify(orgIndustryRepository).saveAll(any());
    }

    /**
     * Test Case ID: TC-OI-002
     * Test Objective: Validate testDeleteByOrgId behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-OI-002: Xóa by orgId")
    void testDeleteByOrgId() {
        orgIndustryService.deleteByOrgId(5L);
        verify(orgIndustryRepository).deleteByOrgId(5L);
    }

    /**
     * Test Case ID: TC-OI-003
     * Test Objective: Validate testDeleteByIndustryIds behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-OI-003: Xóa by list ID")
    void testDeleteByIndustryIds() {
        orgIndustryService.deleteByIndustryIdsAndOrgId(List.of(7L, 8L), 5L);
        verify(orgIndustryRepository).deleteByIndustryIdsAndOrgId(List.of(7L, 8L), 5L);
    }
}


