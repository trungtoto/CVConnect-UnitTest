package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: SearchHistoryOutsideServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Lưu Lịch Sử Tìm Kiếm Outsite
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - getMySearchHistoryOutside: WebUtils.getCurrentUserId null (return empty) vs có kết quả
 *   - findByUserId: check limit, parse array return null list.
 *   - deleteByIds: Lỗi không tìm đủ size (throw DEPARTMENT_NOT_FOUND).
 * ============================================================
 */

import com.cvconnect.dto.searchHistoryOutside.SearchHistoryOutsideDto;
import com.cvconnect.entity.SearchHistoryOutside;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.SearchHistoryOutsideRepository;
import com.cvconnect.service.impl.SearchHistoryOutsideServiceImpl;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchHistoryOutsideServiceImpl - Unit Tests (C2 Branch Coverage)")
public class SearchHistoryOutsideServiceImplTest {

    @Mock
    private SearchHistoryOutsideRepository searchHistoryOutsideRepository;

    @InjectMocks
    private SearchHistoryOutsideServiceImpl searchHistoryOutsideService;

    /**
     * Test Case ID: TC-SHO-001
     * Test Objective: Validate TC_SHO_001_create behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-001: Tạo History record mới")
    void TC_SHO_001_create() {
        SearchHistoryOutsideDto dto = new SearchHistoryOutsideDto();
        dto.setKeyword("Java");
        searchHistoryOutsideService.create(dto);
        verify(searchHistoryOutsideRepository).save(any());
    }

    /**
     * Test Case ID: TC-SHO-002
     * Test Objective: Validate TC_SHO_002_getMySearchHistoryUserIdNull behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-002: Lấy Lịch Sử (WebUtils userId = null)")
    void TC_SHO_002_getMySearchHistoryUserIdNull() {
        try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(null);
            
            List<SearchHistoryOutsideDto> res = searchHistoryOutsideService.getMySearchHistoryOutside();
            assertThat(res).isEmpty(); // Branch fallback
        }
    }

    /**
     * Test Case ID: TC-SHO-003
     * Test Objective: Validate TC_SHO_003_getMySearchHistorySuccessLimit behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-003: Lấy Lịch Sử và Giới Hạn Limit(5)")
    void TC_SHO_003_getMySearchHistorySuccessLimit() {
        try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(9L);
            
            // Mock DB return 6 records
            List<SearchHistoryOutside> mockDB = Collections.nCopies(6, new SearchHistoryOutside());
            when(searchHistoryOutsideRepository.findByUserId(9L)).thenReturn(mockDB);
            
            List<SearchHistoryOutsideDto> res = searchHistoryOutsideService.getMySearchHistoryOutside();
            
            assertThat(res).hasSize(5); // check steam().limit(5)
        }
    }

    /**
     * Test Case ID: TC-SHO-004
     * Test Objective: Validate TC_SHO_004_getByUserId behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-004: getByUserId rỗng list")
    void TC_SHO_004_getByUserId() {
        when(searchHistoryOutsideRepository.findByUserId(10L)).thenReturn(Collections.emptyList());
        List<SearchHistoryOutsideDto> res = searchHistoryOutsideService.findByUserId(10L);
        assertThat(res).isEmpty();
    }

    /**
     * Test Case ID: TC-SHO-005
     * Test Objective: Validate TC_SHO_005_deleteByIdsThrow behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-005: Xoá ID lỗi Throw Exception vì Size không tương đồng")
    void TC_SHO_005_deleteByIdsThrow() {
        try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(9L);
            
            // Delete req 2 Ids but DB matched 1 Id.
            when(searchHistoryOutsideRepository.findByIds(List.of(1L, 2L), 9L)).thenReturn(List.of(new SearchHistoryOutside()));
            
            assertThatThrownBy(() -> searchHistoryOutsideService.deleteByIds(List.of(1L, 2L)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DEPARTMENT_NOT_FOUND));
        }
    }

    /**
     * Test Case ID: TC-SHO-006
     * Test Objective: Validate TC_SHO_006_deleteAll behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-SHO-006: Delete tất cả qua UserId")
    void TC_SHO_006_deleteAll() {
        try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
            webUtils.when(WebUtils::getCurrentUserId).thenReturn(9L);
            
            searchHistoryOutsideService.deleteAllByUserId();
            verify(searchHistoryOutsideRepository).deleteByUserId(9L);
        }
    }
}


