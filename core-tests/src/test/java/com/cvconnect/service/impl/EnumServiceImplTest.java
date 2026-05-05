package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: EnumServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Enums Service
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - Các hàm gọi Enum tĩnh cơ bản.
 * ============================================================
 */

import com.cvconnect.dto.enums.*;
import com.cvconnect.service.impl.EnumServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnumServiceImpl - Unit Tests (C2 Branch Coverage)")
public class EnumServiceImplTest {

    @InjectMocks
    private EnumServiceImpl enumService;

    /**
     * Test Case ID: TC-ENUM-001
     * Test Objective: Validate testGetCurrencyType behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-001: Lấy danh sách CurrencyType")
    void testGetCurrencyType() {
        List<CurrencyTypeDto> result = enumService.getCurrencyType();
        assertThat(result).isNotEmpty();
    }

    /**
     * Test Case ID: TC-ENUM-002
     * Test Objective: Validate testGetJobAdStatus behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-002: Lấy danh sách JobAdStatus")
    void testGetJobAdStatus() {
        List<JobAdStatusDto> result = enumService.getJobAdStatus();
        assertThat(result).isNotEmpty();
    }

    /**
     * Test Case ID: TC-ENUM-003
     * Test Objective: Validate testGetJobType behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-003: Lấy danh sách JobType")
    void testGetJobType() {
        List<JobTypeDto> result = enumService.getJobType();
        assertThat(result).isNotEmpty();
    }

    /**
     * Test Case ID: TC-ENUM-004
     * Test Objective: Validate testGetSalaryType behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-004: Lấy danh sách SalaryType")
    void testGetSalaryType() {
        List<SalaryTypeDto> result = enumService.getSalaryType();
        assertThat(result).isNotEmpty();
    }

    /**
     * Test Case ID: TC-ENUM-005
     * Test Objective: Validate testGetEliminateReason behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-005: Lấy danh sách EliminateReason")
    void testGetEliminateReason() {
        List<EliminateReasonEnumDto> result = enumService.getEliminateReason();
        assertThat(result).isNotEmpty();
    }

    /**
     * Test Case ID: TC-ENUM-006
     * Test Objective: Validate testGetCalendarType behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-ENUM-006: Lấy danh sách CalendarType")
    void testGetCalendarType() {
        List<CalendarTypeDto> result = enumService.getCalendarType();
        assertThat(result).isNotEmpty();
    }
}


