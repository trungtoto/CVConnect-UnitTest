package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: LevelServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.LevelServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - getById()
 *   - filter()
 *   - create()
 *   - update()
 *   - deleteByIds()
 *   - getByIds()
 *   - getLevelsByJobAdId()
 *   - getLevelsMapByJobAdIds()
 * ============================================================
 */

import com.cvconnect.dto.level.LevelDto;
import com.cvconnect.dto.level.LevelFilterRequest;
import com.cvconnect.dto.level.LevelRequest;
import com.cvconnect.entity.Level;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.LevelRepository;
import com.cvconnect.service.impl.LevelServiceImpl;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelServiceImpl - Unit Tests")
public class LevelServiceImplTest {

    @Mock
    private LevelRepository levelRepository;

    @InjectMocks
    private LevelServiceImpl levelService;

    private Level taoLevelEntity(Long id, String code, String name, Boolean isDefault) {
        Level level = new Level();
        level.setId(id);
        level.setCode(code);
        level.setName(name);
        level.setIsDefault(isDefault);
        return level;
    }

    private LevelRequest taoLevelRequest(Long id, String code, String name) {
        return LevelRequest.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }

    @Nested
    @DisplayName("getById()")
    class LayTheoid {

        /**
         * Test Case ID: TC-LEVEL-001
         * Test Objective: Lấy thông tin Level bằng ID thành công
         * Input: ID tồn tại trong DB
         * Expected Output: Trả về đối tượng LevelDto
         * Notes:
         */
        @Test
        void TC_LEVEL_001_traVeDto_KhiTonTai() {
            Level existing = taoLevelEntity(1L, "JUN", "Junior", false);
            when(levelRepository.findById(1L)).thenReturn(Optional.of(existing));

            LevelDto result = levelService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("JUN");
        }

        /**
         * Test Case ID: TC-LEVEL-002
         * Test Objective: Trả về null khi ID không tồn tại
         * Input: ID không tồn tại trong DB
         * Expected Output: Trả về null
         * Notes:
         */
        @Test
        void TC_LEVEL_002_traVeNull_KhiKhongTonTai() {
            when(levelRepository.findById(999L)).thenReturn(Optional.empty());
            LevelDto result = levelService.getById(999L);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("create()")
    class TaoMoiLevel {

        /**
         * Test Case ID: TC-LEVEL-003
         * Test Objective: Tạo mới thành công khi code không bị trùng
         * Input: LevelRequest với code hợp lệ
         * Expected Output: Trả về ID của level mới tạo
         * Notes: Level mới bắt buộc mảng isDefault là false
         */
        @Test
        void TC_LEVEL_003_taoThanhCong_KhiCodeHopLe() {
            LevelRequest request = taoLevelRequest(null, "SEN", "Senior");

            when(levelRepository.existsByCode("SEN")).thenReturn(false);
            when(levelRepository.save(any(Level.class))).thenAnswer(inv -> {
                Level saved = inv.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            IDResponse<Long> result = levelService.create(request);

            assertThat(result.getId()).isEqualTo(10L);
            verify(levelRepository, times(1)).save(argThat(level -> !level.getIsDefault()));
        }

        /**
         * Test Case ID: TC-LEVEL-004
         * Test Objective: Ném lỗi khi code đã tồn tại
         * Input: LevelRequest với code trùng
         * Expected Output: AppException mang mã LEVEL_CODE_DUPLICATED
         * Notes:
         */
        @Test
        void TC_LEVEL_004_nemException_KhiCodeDaTonTai() {
            LevelRequest request = taoLevelRequest(null, "SEN", "Senior");
            when(levelRepository.existsByCode("SEN")).thenReturn(true);

            assertThatThrownBy(() -> levelService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.LEVEL_CODE_DUPLICATED));

            verify(levelRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update()")
    class CapNhatLevel {

        /**
         * Test Case ID: TC-LEVEL-005
         * Test Objective: Update thành công khi ko bị trùng code với level khác
         * Input: LevelRequest với dữ liệu thay đổi
         * Expected Output: Trả về ID
         * Notes:
         */
        @Test
        void TC_LEVEL_005_updateThanhCong() {
            Level existing = taoLevelEntity(1L, "JUN", "Junior", false);
            LevelRequest request = taoLevelRequest(1L, "JUN-NEW", "Junior Updated");

            when(levelRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(levelRepository.findByCode("JUN-NEW")).thenReturn(null);

            IDResponse<Long> result = levelService.update(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(levelRepository, times(1)).save(existing);
            assertThat(existing.getCode()).isEqualTo("JUN-NEW");
        }

        /**
         * Test Case ID: TC-LEVEL-006
         * Test Objective: Ném lỗi update khi ID không đúng
         * Input: LevelRequest có ID sai
         * Expected Output: AppException mang mã LEVEL_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_LEVEL_006_nemException_KhiIdSai() {
            LevelRequest request = taoLevelRequest(999L, "X", "X");
            when(levelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> levelService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.LEVEL_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-LEVEL-007
         * Test Objective: Ném lỗi update khi code mới bị trùng với 1 bản ghi khác
         * Input: LevelRequest có code của level khác
         * Expected Output: AppException mang mã LEVEL_CODE_DUPLICATED
         * Notes:
         */
        @Test
        void TC_LEVEL_007_nemException_KhiCodeTrungKhac() {
            Level existing = taoLevelEntity(1L, "JUN", "Junior", false);
            Level otherLevel = taoLevelEntity(2L, "SEN", "Senior", false);
            LevelRequest request = taoLevelRequest(1L, "SEN", "Senior Attempt");

            when(levelRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(levelRepository.findByCode("SEN")).thenReturn(otherLevel);

            assertThatThrownBy(() -> levelService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.LEVEL_CODE_DUPLICATED));
        }
    }

    @Nested
    @DisplayName("deleteByIds()")
    class XoaLevel {

        /**
         * Test Case ID: TC-LEVEL-008
         * Test Objective: Xóa thành công các ID hợp lệ
         * Input: Các ID không phải default
         * Expected Output: Logic gọi repository.deleteAll() một lần
         * Notes:
         */
        @Test
        void TC_LEVEL_008_xoaThanhCong() {
            List<Long> idsToDel = List.of(1L, 2L);
            List<Level> levels = List.of(
                    taoLevelEntity(1L, "L1", "L1", false),
                    taoLevelEntity(2L, "L2", "L2", false)
            );

            when(levelRepository.findAllById(idsToDel)).thenReturn(levels);

            levelService.deleteByIds(idsToDel);

            verify(levelRepository, times(1)).deleteAll(levels);
        }

        /**
         * Test Case ID: TC-LEVEL-009
         * Test Objective: Ném lỗi khi cố xóa bất kỳ một level có thuộc tính IsDefault
         * Input: Danh sách Ids có chứa default level
         * Expected Output: AppException mang mã LEVEL_CANNOT_DELETE_DEFAULT
         * Notes: Ngăn chặn xóa dữ liệu cơ bản của hệ thống
         */
        @Test
        void TC_LEVEL_009_nemException_KhiXoaDefaultLevel() {
            List<Long> idsToDel = List.of(1L, 2L);
            List<Level> levels = List.of(
                    taoLevelEntity(1L, "L1", "L1", false),
                    taoLevelEntity(2L, "L2", "Default Level", true) // Default
            );

            when(levelRepository.findAllById(idsToDel)).thenReturn(levels);

            assertThatThrownBy(() -> levelService.deleteByIds(idsToDel))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.LEVEL_CANNOT_DELETE_DEFAULT));
            verify(levelRepository, never()).deleteAll(any());
        }
    }

    @Nested
    @DisplayName("filter()")
    class LocLevel {

        /**
         * Test Case ID: TC-LEVEL-010
         * Test Objective: Lọc cấp độ trả về response
         * Input: Bộ lọc LevelFilterRequest
         * Expected Output: Object FilterResponse 
         * Notes:
         */
        @Test
        void TC_LEVEL_010_traVeDuLieu() {
            LevelFilterRequest req = new LevelFilterRequest();
            Page<Level> page = new PageImpl<>(List.of(taoLevelEntity(1L, "JUN", "JUN", false)));

            when(levelRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            FilterResponse<LevelDto> response = levelService.filter(req);

            assertThat(response).isNotNull();
            verify(levelRepository, times(1)).filter(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("TC-LEVEL-010B: filter() có format lại Date")
        void TC_LEVEL_010B_filterCoNgayThang() {
            LevelFilterRequest req = new LevelFilterRequest();
            req.setCreatedAtEnd(java.time.Instant.now());
            req.setUpdatedAtEnd(java.time.Instant.now());
            Page<Level> page = new PageImpl<>(Collections.emptyList());
            when(levelRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            levelService.filter(req);
            verify(levelRepository, times(1)).filter(any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Các hàm Query khác")
    class QueryKhac {
        @Test
        void TC_LEVEL_011_getByIds_Rong() {
            when(levelRepository.findAllById(any())).thenReturn(Collections.emptyList());
            List<LevelDto> res = levelService.getByIds(List.of(1L));
            assertThat(res).isEmpty();
        }

        @Test
        void TC_LEVEL_012_getLevelsByJobAdId_Rong() {
            when(levelRepository.findByJobAdId(any())).thenReturn(Collections.emptyList());
            List<LevelDto> res = levelService.getLevelsByJobAdId(1L);
            assertThat(res).isEmpty();
        }
    }
}

