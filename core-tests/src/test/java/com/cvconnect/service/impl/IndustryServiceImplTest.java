package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: IndustryServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.IndustryServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - create()
 *   - update()
 *   - deleteByIds()
 *   - filter()
 *   - findByIds()
 * ============================================================
 */

import com.cvconnect.dto.industry.IndustryDto;
import com.cvconnect.dto.industry.IndustryFilterRequest;
import com.cvconnect.dto.industry.IndustryRequest;
import com.cvconnect.entity.Industry;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.IndustryRepository;
import com.cvconnect.service.impl.IndustryServiceImpl;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryServiceImpl - Unit Tests")
public class IndustryServiceImplTest {

    @Mock
    private IndustryRepository industryRepository;

    @InjectMocks
    private IndustryServiceImpl industryService;

    private Industry taoIndustryEntity(Long id, String code, String name) {
        Industry i = new Industry();
        i.setId(id);
        i.setCode(code);
        i.setName(name);
        return i;
    }

    private IndustryRequest taoIndustryRequest(Long id, String code, String name) {
        return IndustryRequest.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class TaoMoiIndustry {

        /**
         * Test Case ID: TC-INDUSTRY-001
         * Test Objective: Tạo thành công khi code chưa tồn tại
         * Input: IndustryRequest với code mới
         * Expected Output: Trả về ID của industry mới
         * Notes: Kiểm tra thao tác lưu DB (cần có check repository.save)
         */
        @Test
        void TC_INDUSTRY_001_taoThanhCong_KhiCodeChuaTonTai() {
            IndustryRequest request = taoIndustryRequest(null, "TECH", "Technology");

            when(industryRepository.existsByCode("TECH")).thenReturn(false);
            when(industryRepository.save(any())).thenAnswer(inv -> {
                Industry i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            IDResponse<Long> result = industryService.create(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(industryRepository, times(1)).save(any(Industry.class));
        }

        /**
         * Test Case ID: TC-INDUSTRY-002
         * Test Objective: Ném lỗi khi code đã tồn tại
         * Input: IndustryRequest với code bị trùng
         * Expected Output: AppException với lỗi INDUSTRY_CODE_ALREADY_EXISTS
         * Notes: Xác nhận không lưu DB khi trùng code
         */
        @Test
        void TC_INDUSTRY_002_nemException_KhiCodeDaTonTai() {
            IndustryRequest request = taoIndustryRequest(null, "TECH", "Technology");

            when(industryRepository.existsByCode("TECH")).thenReturn(true);

            assertThatThrownBy(() -> industryService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.INDUSTRY_CODE_ALREADY_EXISTS));
            verify(industryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update()")
    class CapNhatIndustry {

        /**
         * Test Case ID: TC-INDUSTRY-003
         * Test Objective: Update thành công khi id tồn tại và không trùng code của industry khác
         * Input: IndustryRequest thay đổi code hợp lệ
         * Expected Output: Cập nhật và trả về ID
         * Notes: Kiểm tra việc lưu DB
         */
        @Test
        void TC_INDUSTRY_003_updateThanhCong_KhiValidRequest() {
            Industry existing = taoIndustryEntity(1L, "IT", "IT");
            IndustryRequest request = taoIndustryRequest(1L, "IT-NEW", "IT Updated");

            when(industryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(industryRepository.findByCode("IT-NEW")).thenReturn(null);
            when(industryRepository.save(any())).thenReturn(existing);

            IDResponse<Long> result = industryService.update(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(industryRepository, times(1)).save(any(Industry.class));
        }

        /**
         * Test Case ID: TC-INDUSTRY-004
         * Test Objective: Update thành công khi giữ nguyên code cũ
         * Input: IndustryRequest giữ nguyên code
         * Expected Output: Cập nhật và trả về ID
         * Notes: code cũ nên findByCode sẽ trả về chính nó (cùng id)
         */
        @Test
        void TC_INDUSTRY_004_updateThanhCong_KhiGiuNguyenCode() {
            Industry existing = taoIndustryEntity(1L, "IT", "IT");
            IndustryRequest request = taoIndustryRequest(1L, "IT", "IT Renamed");

            when(industryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(industryRepository.findByCode("IT")).thenReturn(existing); 
            when(industryRepository.save(any())).thenReturn(existing);

            IDResponse<Long> result = industryService.update(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(industryRepository, times(1)).save(any(Industry.class));
        }

        /**
         * Test Case ID: TC-INDUSTRY-005
         * Test Objective: Ném lỗi khi ID không tồn tại
         * Input: IndustryRequest chứa id sai
         * Expected Output: AppException mang mã INDUSTRY_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_INDUSTRY_005_nemException_KhiIdKhongTonTai() {
            IndustryRequest request = taoIndustryRequest(999L, "X", "X");

            when(industryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> industryService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.INDUSTRY_NOT_FOUND));
            verify(industryRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-INDUSTRY-006
         * Test Objective: Ném lỗi khi code đã thuộc về industry khác
         * Input: IndustryRequest cập nhật code thành "FIN", nhưng "FIN" của id=2
         * Expected Output: AppException mang mã INDUSTRY_CODE_ALREADY_EXISTS
         * Notes:
         */
        @Test
        void TC_INDUSTRY_006_nemException_KhiCodeThuocVeKhac() {
            Industry existing = taoIndustryEntity(1L, "IT", "IT");
            Industry other = taoIndustryEntity(2L, "FIN", "Finance");
            IndustryRequest request = taoIndustryRequest(1L, "FIN", "Finance");

            when(industryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(industryRepository.findByCode("FIN")).thenReturn(other);

            assertThatThrownBy(() -> industryService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.INDUSTRY_CODE_ALREADY_EXISTS));
            verify(industryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteByIds()")
    class XoaIndustry {

        /**
         * Test Case ID: TC-INDUSTRY-007
         * Test Objective: Xóa thành công khi tất cả ID đều tồn tại
         * Input: Danh sách ID tồn tại trong DB
         * Expected Output: Không lỗi, gọi repository.deleteAll()
         * Notes:
         */
        @Test
        void TC_INDUSTRY_007_xoaThanhCong_KhiTatCaIdsTonTai() {
            List<Industry> industries = List.of(
                    taoIndustryEntity(1L, "IT", "IT"),
                    taoIndustryEntity(2L, "FIN", "FIN")
            );
            List<Long> idsToDel = List.of(1L, 2L);

            when(industryRepository.findAllById(idsToDel)).thenReturn(industries);

            industryService.deleteByIds(idsToDel);

            verify(industryRepository, times(1)).deleteAll(industries);
        }

        /**
         * Test Case ID: TC-INDUSTRY-008
         * Test Objective: Ném lỗi khi số lượng entities tìm được không khớp với số ids
         * Input: Có ID không tồn tại
         * Expected Output: AppException mang mã INDUSTRY_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_INDUSTRY_008_nemException_KhiMotVaiIdKhongTonTai() {
            List<Long> idsToDel = List.of(1L, 999L);
            when(industryRepository.findAllById(idsToDel))
                    .thenReturn(List.of(taoIndustryEntity(1L, "IT", "IT")));

            assertThatThrownBy(() -> industryService.deleteByIds(idsToDel))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.INDUSTRY_NOT_FOUND));
            verify(industryRepository, never()).deleteAll(any());
        }
    }

    @Nested
    @DisplayName("filter()")
    class LocIndustry {

        /**
         * Test Case ID: TC-INDUSTRY-009
         * Test Objective: Trả về dữ liệu filter đúng
         * Input: IndustryFilterRequest không rỗng
         * Expected Output: FilterResponse chứa danh sách IndustryDto
         * Notes:
         */
        @Test
        void TC_INDUSTRY_009_traVeDuLieu() {
            Page<Industry> page = new PageImpl<>(List.of(taoIndustryEntity(1L, "IT", "IT")));
            when(industryRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            FilterResponse<IndustryDto> result = industryService.filter(new IndustryFilterRequest());

            assertThat(result).isNotNull();
            verify(industryRepository, times(1)).filter(any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIds()")
    class TimTheoids {

        /**
         * Test Case ID: TC-INDUSTRY-010
         * Test Objective: Trả về danh sách DTO theo ids
         * Input: Danh sách ids hợp lệ
         * Expected Output: Danh sách IndustryDto tương ứng
         * Notes:
         */
        @Test
        void TC_INDUSTRY_010_traVeDanhSach_KhiIdTonTai() {
            List<Industry> industries = List.of(
                    taoIndustryEntity(1L, "IT", "IT"),
                    taoIndustryEntity(2L, "FIN", "FIN")
            );
            when(industryRepository.findAllById(List.of(1L, 2L))).thenReturn(industries);

            List<IndustryDto> result = industryService.findByIds(List.of(1L, 2L));

            assertThat(result).hasSize(2);
        }

        /**
         * Test Case ID: TC-INDUSTRY-011
         * Test Objective: Trả về danh sách rỗng nếu ids không tồn tại
         * Input: Danh sách ids không có cái nào khớp
         * Expected Output: Empty list
         * Notes:
         */
        @Test
        void TC_INDUSTRY_011_traVeRong_KhiNoMatch() {
            when(industryRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

            List<IndustryDto> result = industryService.findByIds(List.of(999L));

            assertThat(result).isEmpty();
        }
    }
}

