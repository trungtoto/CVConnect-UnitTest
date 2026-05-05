package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: PositionServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.PositionServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - create()
 *   - update()
 *   - changeStatusActive()
 *   - deleteByIds()
 *   - detail()
 *   - filter()
 *   - findById()
 *   - getPositionMapByIds()
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.department.DepartmentDto;
import com.cvconnect.dto.position.PositionDto;
import com.cvconnect.dto.position.PositionFilterRequest;
import com.cvconnect.dto.position.PositionRequest;
import com.cvconnect.dto.positionProcess.PositionProcessDto;
import com.cvconnect.dto.positionProcess.PositionProcessRequest;
import com.cvconnect.dto.processType.ProcessTypeDto;
import com.cvconnect.entity.Position;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.PositionRepository;
import com.cvconnect.service.DepartmentService;
import com.cvconnect.service.PositionProcessService;
import com.cvconnect.service.ProcessTypeService;
import com.cvconnect.service.impl.PositionServiceImpl;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.exception.CommonErrorCode;
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
@DisplayName("PositionServiceImpl - Unit Tests")
public class PositionServiceImplTest {

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private PositionProcessService positionProcessService;
    @Mock
    private ProcessTypeService processTypeService;
    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private PositionServiceImpl positionService;

    // Helpers
    private Position taoPositionEntity(Long id, String code, String name, Long deptId) {
        Position p = new Position();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        p.setDepartmentId(deptId);
        return p;
    }

    private ProcessTypeDto taoProcessTypeDto(String code) {
        ProcessTypeDto pt = new ProcessTypeDto();
        pt.setCode(code);
        return pt;
    }

    private DepartmentDto taoDepartmentDto(Long id) {
        DepartmentDto d = new DepartmentDto();
        d.setId(id);
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class TaoMoiPosition {

        /**
         * Test Case ID: TC-POS-001
         * Test Objective: Tạo Position thành công với quy trình hợp lệ (APPLY -> ONBOARD)
         * Input: PositionRequest hợp lệ, department tồn tại, code không trùng
         * Expected Output: ID của bản ghi mới
         * Notes:
         */
        @Test
        void TC_POS_001_taoThanhCong() {
            // Giả lập Input
            List<PositionProcessRequest> processes = List.of(
                    PositionProcessRequest.builder().processTypeId(10L).build(),
                    PositionProcessRequest.builder().processTypeId(20L).build()
            );
            PositionRequest request = PositionRequest.builder()
                    .departmentId(1L).code("DEV").name("Developer").positionProcess(processes)
                    .build();

            // Mock dependencies
            when(departmentService.detail(1L)).thenReturn(taoDepartmentDto(1L));
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 1L)).thenReturn(false);
            when(processTypeService.detail(10L)).thenReturn(taoProcessTypeDto("APPLY"));
            when(processTypeService.detail(20L)).thenReturn(taoProcessTypeDto("ONBOARD"));
            
            when(positionRepository.save(any(Position.class))).thenAnswer(inv -> {
                Position p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            IDResponse<Long> result = positionService.create(request);

            assertThat(result.getId()).isEqualTo(100L);
            verify(positionRepository, times(1)).save(any(Position.class));
            verify(positionProcessService, times(1)).create(any());
        }

        /**
         * Test Case ID: TC-POS-002
         * Test Objective: Lỗi Access Denied khi không tìm thấy department
         * Input: DepartmentId sai
         * Expected Output: AppException với ACCESS_DENIED
         * Notes: Không insert DB
         */
        @Test
        void TC_POS_002_nemException_KhiDeptKhongTonTai() {
            PositionRequest request = PositionRequest.builder().departmentId(99L).build();
            when(departmentService.detail(99L)).thenReturn(null);

            assertThatThrownBy(() -> positionService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-POS-003
         * Test Objective: Lỗi duplicate code trong cùng department
         * Input: Code đã tồn tại
         * Expected Output: AppException với POSITION_CODE_DUPLICATED
         * Notes:
         */
        @Test
        void TC_POS_003_nemException_KhiTrungCode() {
            PositionRequest request = PositionRequest.builder().departmentId(1L).code("DEV").build();
            when(departmentService.detail(1L)).thenReturn(taoDepartmentDto(1L));
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 1L)).thenReturn(true);

            assertThatThrownBy(() -> positionService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.POSITION_CODE_DUPLICATED));
        }

        /**
         * Test Case ID: TC-POS-004
         * Test Objective: Bắt buộc Process đầu tiên phải là APPLY
         * Input: Process đầu tiên khác APPLY
         * Expected Output: AppException (FIRST_PROCESS_MUST_BE_APPLY)
         * Notes:
         */
        @Test
        void TC_POS_004_nemException_KhiFirstKhacApply() {
            List<PositionProcessRequest> processes = List.of(
                    PositionProcessRequest.builder().processTypeId(10L).build()
            );
            PositionRequest request = PositionRequest.builder()
                    .departmentId(1L).code("DEV").positionProcess(processes).build();

            when(departmentService.detail(1L)).thenReturn(taoDepartmentDto(1L));
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 1L)).thenReturn(false);
            when(processTypeService.detail(10L)).thenReturn(taoProcessTypeDto("INTERVIEW"));

            assertThatThrownBy(() -> positionService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.FIRST_PROCESS_MUST_BE_APPLY));
        }

        /**
         * Test Case ID: TC-POS-005
         * Test Objective: Bắt buộc Process cuối cùng phải là ONBOARD
         * Input: Process cuối khác ONBOARD
         * Expected Output: AppException (LAST_PROCESS_MUST_BE_ONBOARD)
         * Notes:
         */
        @Test
        void TC_POS_005_nemException_KhiLastKhacOnboard() {
            List<PositionProcessRequest> processes = List.of(
                    PositionProcessRequest.builder().processTypeId(10L).build(),
                    PositionProcessRequest.builder().processTypeId(20L).build()
            );
            PositionRequest request = PositionRequest.builder()
                    .departmentId(1L).code("DEV").positionProcess(processes).build();

            when(departmentService.detail(1L)).thenReturn(taoDepartmentDto(1L));
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 1L)).thenReturn(false);
            when(processTypeService.detail(10L)).thenReturn(taoProcessTypeDto("APPLY"));
            when(processTypeService.detail(20L)).thenReturn(taoProcessTypeDto("INTERVIEW"));

            assertThatThrownBy(() -> positionService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.LAST_PROCESS_MUST_BE_ONBOARD));
        }

        /**
         * Test Case ID: TC-POS-005B
         * Test Objective: Tạo thành công khi list PositionProcess null (thỏa mãn nhánh if(nonNull) == false)
         * Input: PositionProcess list = null
         * Expected Output: Không ném lỗi phần process, trả về ID
         * Notes:
         */
        @Test
        void TC_POS_005B_taoThanhCong_KhiProcessNull() {
            PositionRequest request = PositionRequest.builder()
                    .departmentId(1L).code("DEV").name("Developer").positionProcess(null)
                    .build();

            when(departmentService.detail(1L)).thenReturn(taoDepartmentDto(1L));
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 1L)).thenReturn(false);
            when(positionRepository.save(any(Position.class))).thenAnswer(inv -> {
                Position p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            IDResponse<Long> result = positionService.create(request);

            assertThat(result.getId()).isEqualTo(100L);
            verify(positionRepository, times(1)).save(any(Position.class));
            verify(positionProcessService, never()).create(any()); // không lưu process
        }
    }

    @Nested
    @DisplayName("update()")
    class CapNhatPosition {

        /**
         * Test Case ID: TC-POS-006
         * Test Objective: Cập nhật thành công trong điều kiện bình thường
         * Input: Request update thông tin valid
         * Notes: Kiểm tra cập nhật listProcess (xóa cũ, thêm người xài mới), update record
         * Notes:
         */
        @Test
        void TC_POS_006_updateThanhCong() {
            Long orgId = 123L;
            List<PositionProcessRequest> processes = List.of(
                    PositionProcessRequest.builder().processTypeId(10L).build(),
                    PositionProcessRequest.builder().processTypeId(20L).build()
            );
            PositionRequest request = PositionRequest.builder()
                    .id(1L).departmentId(10L).code("DEV").positionProcess(processes)
                    .build();

            Position existing = taoPositionEntity(1L, "DEV", "Old Dev", 10L);

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentService.detail(10L)).thenReturn(taoDepartmentDto(10L));
            when(positionRepository.findByIdAndOrgId(1L, orgId)).thenReturn(existing);
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 10L)).thenReturn(false);
            
            // Xử lý Check Process
            when(positionProcessService.getByPositionId(1L)).thenReturn(Collections.emptyList());
            when(processTypeService.detail(10L)).thenReturn(taoProcessTypeDto("APPLY"));
            when(processTypeService.detail(20L)).thenReturn(taoProcessTypeDto("ONBOARD"));

            IDResponse<Long> result = positionService.update(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(positionRepository, times(1)).save(any(Position.class));
            verify(positionProcessService, times(1)).deleteByIds(any());
            verify(positionProcessService, times(1)).create(any());
        }

        /**
         * Test Case ID: TC-POS-007
         * Test Objective: Update ném lỗi nếu PositionNotFound
         * Input: Update request với id không tồn tại / org sai
         * Expected Output: POSITION_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_POS_007_nemException_KhiKhongTimThayPosition() {
            Long orgId = 123L;
            PositionRequest request = PositionRequest.builder().id(99L).departmentId(10L).build();
            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentService.detail(10L)).thenReturn(taoDepartmentDto(10L));
            when(positionRepository.findByIdAndOrgId(99L, orgId)).thenReturn(null);

            assertThatThrownBy(() -> positionService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.POSITION_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-POS-008
         * Test Objective: Update ném lỗi duplicate code trong cùng dept khác position id
         * Input: Update đổi code sang "QA" nhưng QA đã tồn tại
         * Expected Output: POSITION_CODE_DUPLICATED
         * Notes:
         */
        @Test
        void TC_POS_008_nemException_KhiTrungCode() {
            Long orgId = 123L;
            PositionRequest request = PositionRequest.builder().id(1L).departmentId(10L).code("QA").build();
            Position existing = taoPositionEntity(1L, "DEV", "Old Dev", 10L);

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentService.detail(10L)).thenReturn(taoDepartmentDto(10L));
            when(positionRepository.findByIdAndOrgId(1L, orgId)).thenReturn(existing);
            when(positionRepository.existsByCodeAndDepartmentId("QA", 10L)).thenReturn(true);

            assertThatThrownBy(() -> positionService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.POSITION_CODE_DUPLICATED));
        }

        /**
         * Test Case ID: TC-POS-008B
         * Test Objective: Update ném lỗi Access Denied do departmenDto null
         * Input: Department Id sai
         * Expected Output: ACCESS_DENIED
         * Notes:
         */
        @Test
        void TC_POS_008B_nemException_KhiDeptNotFound() {
            PositionRequest request = PositionRequest.builder().departmentId(99L).build();
            when(restTemplateClient.validOrgMember()).thenReturn(123L);
            when(departmentService.detail(99L)).thenReturn(null);

            assertThatThrownBy(() -> positionService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }

        /**
         * Test Case ID: TC-POS-008C
         * Test Objective: Update thành công dù existsByCode=true nhưng lại chính là code cũ của position này
         * Input: code giữ nguyên
         * Expected Output: update bình thường
         * Notes: nhánh `if(existsByCode && !position.getCode().equals(reqCode))` = false
         */
        @Test
        void TC_POS_008C_updateThanhCong_KhiGiuNguyenCode() {
            Long orgId = 123L;
            PositionRequest request = PositionRequest.builder().id(1L).departmentId(10L).code("DEV").build();
            Position existing = taoPositionEntity(1L, "DEV", "Old Dev", 10L);

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentService.detail(10L)).thenReturn(taoDepartmentDto(10L));
            when(positionRepository.findByIdAndOrgId(1L, orgId)).thenReturn(existing);
            // Có check exists bằng true (code này có người xài), nhưng chính là bản thân (DEV -> DEV)
            when(positionRepository.existsByCodeAndDepartmentId("DEV", 10L)).thenReturn(true);
            when(positionProcessService.getByPositionId(1L)).thenReturn(Collections.emptyList());

            positionService.update(request);

            verify(positionRepository, times(1)).save(existing);
        }
    }

    @Nested
    @DisplayName("changeStatusActive()")
    class TrangThaiPosition {

        /**
         * Test Case ID: TC-POS-009
         * Test Objective: Thay đổi Active cho nhiều ids
         * Input: Danh sách ids hợp lệ
         * Expected Output: Gọi DB saveAll lưu trạng thái update
         * Notes:
         */
        @Test
        void TC_POS_009_changeStatusThanhCong() {
            ChangeStatusActiveRequest request = ChangeStatusActiveRequest.builder().ids(List.of(1L)).active(true).build();
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(positionRepository.findByIdsAndOrgId(List.of(1L), 100L)).thenReturn(List.of(taoPositionEntity(1L, "A", "A", 1L)));

            positionService.changeStatusActive(request);
            verify(positionRepository, times(1)).saveAll(any());
        }

        /**
         * Test Case ID: TC-POS-010
         * Test Objective: Ném lỗi nếu có ID sai
         * Input: ds Ids bị thiếu 1 id trong DB
         * Expected Output: ACCESS_DENIED
         * Notes:
         */
        @Test
        void TC_POS_010_nemException_KhiLengthKhongBang() {
            ChangeStatusActiveRequest request = ChangeStatusActiveRequest.builder().ids(List.of(1L, 2L)).active(true).build();
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(positionRepository.findByIdsAndOrgId(List.of(1L, 2L), 100L)).thenReturn(List.of(taoPositionEntity(1L, "A", "A", 1L)));

            assertThatThrownBy(() -> positionService.changeStatusActive(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("deleteByIds()")
    class XoaPosition {

        /**
         * Test Case ID: TC-POS-011
         * Test Objective: Xóa position thành công
         * Input: Danh sách Ids
         * Expected Output: Trả về ID của position vừa tạo.entity tương ứng
         * Notes:
         */
        @Test
        void TC_POS_011_deleteThanhCong() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            List<Position> list = List.of(taoPositionEntity(1L, "A", "A", 10L));
            when(positionRepository.findByIdsAndOrgId(List.of(1L), 100L)).thenReturn(list);

            positionService.deleteByIds(List.of(1L));
            verify(positionRepository, times(1)).deleteAll(list);
        }

        /**
         * Test Case ID: TC-POS-012
         * Test Objective: Xóa lỗi nếu size ids sai
         * Input: ds Ids ko match trong repository
         * Expected Output: ACCESS_DENIED
         * Notes:
         */
        @Test
        void TC_POS_012_nemException_KhiXoaHutId() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(positionRepository.findByIdsAndOrgId(List.of(1L), 100L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> positionService.deleteByIds(List.of(1L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("Các hàm Query (detail, filter, findById, mapByIds)")
    class TruyVanPosition {

        /**
         * Test Case ID: TC-POS-013
         * Test Objective: detail() trả về info và list processes
         * Notes: Đảm bảo mapping từ Entity -> DTO chính xác.
         * Expected Output: Trả về DTO có listProcess đầy đủ.        * Notes:
         */
        @Test
        void TC_POS_013_detailThanhCong() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(positionRepository.findByIdAndOrgId(1L, 100L)).thenReturn(taoPositionEntity(1L, "A", "A", 10L));
            PositionProcessDto processDto = new PositionProcessDto();
            when(positionProcessService.getByPositionId(1L)).thenReturn(List.of(processDto));

            PositionDto result = positionService.detail(1L);

            assertThat(result).isNotNull();
            assertThat(result.getListProcess()).hasSize(1);
        }

        /**
         * Test Case ID: TC-POS-013B
         * Test Objective: detail() ném lỗi khi không tìm thấy Position (nhánh if isNull == true)
         * Input: Id sai
         * Expected Output: POSITION_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_POS_013B_nemException_KhiKhongTimThay() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            when(positionRepository.findByIdAndOrgId(99L, 100L)).thenReturn(null);

            assertThatThrownBy(() -> positionService.detail(99L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.POSITION_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-POS-014
         * Test Objective: filter() trả data page
         * Input: PositionFilterRequest
         * Expected Output: FilterResponse data
         * Notes:
         */
        @Test
        void TC_POS_014_filterThanhCong() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            Page<Position> page = new PageImpl<>(List.of(taoPositionEntity(1L, "A", "A", 10L)));
            when(positionRepository.filter(any(), any(Pageable.class))).thenReturn((Page)page);

            FilterResponse<PositionDto> response = positionService.filter(new PositionFilterRequest());
            assertThat(response).isNotNull();
        }

        /**
         * Test Case ID: TC-POS-014B
         * Test Objective: filter() có format lại Date (nhánh if getCreatedAtEnd != null && getUpdatedAtEnd != null)
         * Input: request chứa ngày
         * Expected Output: format lại ngày thành End Of Day
         * Notes: test C2 Coverage
         */
        @Test
        void TC_POS_014B_filterCoNgayThang() {
            when(restTemplateClient.validOrgMember()).thenReturn(100L);
            Page<Position> page = new PageImpl<>(Collections.emptyList());
            when(positionRepository.filter(any(), any(Pageable.class))).thenReturn((Page)page);

            PositionFilterRequest req = new PositionFilterRequest();
            req.setCreatedAtEnd(java.time.Instant.now());
            req.setUpdatedAtEnd(java.time.Instant.now());

            positionService.filter(req);

            verify(positionRepository, times(1)).filter(any(), any(Pageable.class));
            // Äảm bảo không ném exception trong các utils
        }

        /**
         * Test Case ID: TC-POS-015
         * Test Objective: getPositionMapByIds trả mảng rỗng nếu đầu vào rỗng
         * Input: list null / empty
         * Expected Output: Map size = 0
         * Notes:
         */
        @Test
        void TC_POS_015_mapByIdsRong() {
            Map<Long, PositionDto> result = positionService.getPositionMapByIds(Collections.emptyList());
            assertThat(result).isEmpty();
        }

        /**
         * Test Case ID: TC-POS-016
         * Test Objective: getPositionMapByIds trả map data
         * Input: list Ids
         * Expected Output: Map key = id
         * Notes:
         */
        @Test
        void TC_POS_016_mapByIdsCoData() {
            List<Position> list = List.of(taoPositionEntity(1L, "A", "A", 1L), taoPositionEntity(2L, "B", "B", 1L));
            when(positionRepository.findAllById(List.of(1L, 2L))).thenReturn(list);

            Map<Long, PositionDto> map = positionService.getPositionMapByIds(List.of(1L, 2L));
            assertThat(map).hasSize(2).containsKeys(1L, 2L);
        }

        /**
         * Test Case ID: TC-POS-017
         * Test Objective: findById ném null khi optional.isEmpty
         * Input: DB không tồn tại
         * Expected Output: null
         * Notes: C2 Coverage
         */
        @Test
        void TC_POS_017_findByIdRong() {
            when(positionRepository.findById(99L)).thenReturn(Optional.empty());
            PositionDto ret = positionService.findById(99L);
            assertThat(ret).isNull();
        }

        /**
         * Test Case ID: TC-POS-018
         * Test Objective: getPositionMapByIds trả mảng rỗng nếu repository tìm ra list empty
         * Input: id tồn tại nhưng repository ko trả kết quả
         * Expected Output: Map == empty
         * Notes: C2 Coverage (nhánh if ObjectUtils.isEmpty(positions))
         */
        @Test
        void TC_POS_018_mapByIdsDBEmpty() {
            when(positionRepository.findAllById(List.of(1L, 2L))).thenReturn(Collections.emptyList());
            Map<Long, PositionDto> map = positionService.getPositionMapByIds(List.of(1L, 2L));
            assertThat(map).isEmpty();
        }
    }
}

