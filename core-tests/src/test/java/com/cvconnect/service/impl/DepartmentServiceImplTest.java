package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: DepartmentServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.DepartmentServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - create()
 *   - detail()
 *   - changeStatusActive()
 *   - update()
 *   - deleteByIds()
 *   - filter()
 *   - findById()
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.department.DepartmentDto;
import com.cvconnect.dto.department.DepartmentFilterRequest;
import com.cvconnect.dto.department.DepartmentRequest;
import com.cvconnect.entity.Department;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.DepartmentRepository;
import com.cvconnect.service.impl.DepartmentServiceImpl;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
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
@DisplayName("DepartmentServiceImpl - Unit Tests")
public class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department taoDepartmentEntity(Long id, String code, String name, Long orgId) {
        Department d = new Department();
        d.setId(id);
        d.setCode(code);
        d.setName(name);
        d.setOrgId(orgId);
        return d;
    }

    private DepartmentRequest taoDepartmentRequest(Long id, String code, String name) {
        return DepartmentRequest.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class TaoMoiDepartment {

        /**
         * Test Case ID: TC-DEPT-001
         * Test Objective: Tạo department thành công
         * Input: DepartmentRequest hợp lệ, code chưa tồn tại trong tổ chức đó
         * Expected Output: Trả về ID của department vừa tạo
         * Notes: Kiểm tra việc verifyOrgMember và lưu DB
         */
        @Test
        void TC_DEPT_001_taoThanhCong_KhiCodeChuaTonTai() {
            DepartmentRequest request = taoDepartmentRequest(null, "HR", "Human Resources");
            Long orgId = 100L;

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.existsByCodeAndOrgId("HR", orgId)).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
                Department d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            IDResponse<Long> result = departmentService.create(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(departmentRepository, times(1)).save(any(Department.class));
        }

        /**
         * Test Case ID: TC-DEPT-002
         * Test Objective: Ném lỗi khi code đã tồn tại trong tổ chức
         * Input: DepartmentRequest với code bị trùng
         * Expected Output: AppException với mã DEPARTMENT_CODE_DUPLICATED
         * Notes: Không gọi thao tác lưu DB
         */
        @Test
        void TC_DEPT_002_nemException_KhiCodeDaTonTai() {
            DepartmentRequest request = taoDepartmentRequest(null, "HR", "Human Resources");
            Long orgId = 100L;

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.existsByCodeAndOrgId("HR", orgId)).thenReturn(true);

            assertThatThrownBy(() -> departmentService.create(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.DEPARTMENT_CODE_DUPLICATED));

            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("detail()")
    class ChiTietDepartment {

        /**
         * Test Case ID: TC-DEPT-003
         * Test Objective: Lấy chi tiết department thành công
         * Input: ID của department hợp lệ
         * Expected Output: Trả về DepartmentDto
         * Notes:
         */
        @Test
        void TC_DEPT_003_traVeDto_KhiTimThay() {
            Long orgId = 100L;
            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            Department existing = taoDepartmentEntity(1L, "HR", "HR", orgId);
            when(departmentRepository.findByIdAndOrgId(1L, orgId)).thenReturn(Optional.of(existing));

            DepartmentDto result = departmentService.detail(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("HR");
        }

        /**
         * Test Case ID: TC-DEPT-004
         * Test Objective: Trả về null khi department không tồn tại
         * Input: ID không tồn tại
         * Expected Output: Trả về null
         * Notes:
         */
        @Test
        void TC_DEPT_004_traVeNull_KhiKhongTimThay() {
            Long orgId = 100L;
            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdAndOrgId(999L, orgId)).thenReturn(Optional.empty());

            DepartmentDto result = departmentService.detail(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("changeStatusActive()")
    class ThayDoiTrangThai {

        /**
         * Test Case ID: TC-DEPT-005
         * Test Objective: Cập nhật trạng thái active thành công cho danh sách departments
         * Input: ChangeStatusActiveRequest với danh sách ID hợp lệ
         * Expected Output: Không lỗi, gọi repository.saveAll()
         * Notes:
         */
        @Test
        void TC_DEPT_005_capNhatThanhCong() {
            Long orgId = 100L;
            ChangeStatusActiveRequest request = ChangeStatusActiveRequest.builder()
                    .ids(List.of(1L, 2L))
                    .active(true)
                    .build();
            List<Department> list = List.of(
                    taoDepartmentEntity(1L, "D1", "D1", orgId),
                    taoDepartmentEntity(2L, "D2", "D2", orgId)
            );

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdsAndOrgId(request.getIds(), orgId)).thenReturn(list);

            departmentService.changeStatusActive(request);

            verify(departmentRepository, times(1)).saveAll(list);
            assertThat(list.get(0).getIsActive()).isTrue();
        }

        /**
         * Test Case ID: TC-DEPT-006
         * Test Objective: Ném lỗi nếu số lượng ID tìm thấy không khớp số lượng ID yêu cầu
         * Input: Có ID không tồn tại hoặc thuộc org khác
         * Expected Output: AppException với mã DEPARTMENT_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_DEPT_006_nemException_KhiKhongTimThayDuIds() {
            Long orgId = 100L;
            ChangeStatusActiveRequest request = ChangeStatusActiveRequest.builder()
                    .ids(List.of(1L, 999L))
                    .active(true)
                    .build();

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdsAndOrgId(request.getIds(), orgId))
                    .thenReturn(List.of(taoDepartmentEntity(1L, "D1", "D1", orgId)));

            assertThatThrownBy(() -> departmentService.changeStatusActive(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.DEPARTMENT_NOT_FOUND));

            verify(departmentRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("update()")
    class CapNhatDepartment {

        /**
         * Test Case ID: TC-DEPT-007
         * Test Objective: Cập nhật thành công khi code mới không trùng lặp
         * Input: DepartmentRequest hợp lệ
         * Expected Output: Trả về ID đã cập nhật
         * Notes:
         */
        @Test
        void TC_DEPT_007_updateThanhCong_KhiCodeHopLe() {
            Long orgId = 100L;
            DepartmentRequest request = taoDepartmentRequest(1L, "HR-NEW", "HR New");
            Department existing = taoDepartmentEntity(1L, "HR", "HR", orgId);

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdAndOrgId(1L, orgId)).thenReturn(Optional.of(existing));
            when(departmentRepository.existsByCodeAndOrgId("HR-NEW", orgId)).thenReturn(false);

            IDResponse<Long> result = departmentService.update(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(departmentRepository, times(1)).save(any(Department.class));
        }

        /**
         * Test Case ID: TC-DEPT-008
         * Test Objective: Ném lỗi khi ID cần update không tồn tại trong DB của tổ chức sinh ra ID
         * Input: DepartmentRequest với ID sai
         * Expected Output: AppException với mã lỗi DEPARTMENT_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_DEPT_008_nemException_KhiIdKhongTonTai() {
            Long orgId = 100L;
            DepartmentRequest request = taoDepartmentRequest(999L, "HR-NEW", "HR New");

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdAndOrgId(999L, orgId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.update(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.DEPARTMENT_NOT_FOUND));

            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteByIds()")
    class XoaDepartment {

        /**
         * Test Case ID: TC-DEPT-009
         * Test Objective: Xóa thành công khi các IDs hợp lệ và thuộc đúng org
         * Input: Danh sách IDs hợp lệ
         * Expected Output: Gọi repository.deleteAll() với danh sách entities tìm được
         * Notes:
         */
        @Test
        void TC_DEPT_009_xoaThanhCong() {
            Long orgId = 100L;
            List<Long> idsToDel = List.of(1L, 2L);
            List<Department> list = List.of(
                    taoDepartmentEntity(1L, "D1", "D1", orgId),
                    taoDepartmentEntity(2L, "D2", "D2", orgId)
            );

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdsAndOrgId(idsToDel, orgId)).thenReturn(list);

            departmentService.deleteByIds(idsToDel);

            verify(departmentRepository, times(1)).deleteAll(list);
        }

        /**
         * Test Case ID: TC-DEPT-010
         * Test Objective: Ném lỗi khi có ID tìm thấy ít hơn số lượng cần xóa
         * Input: Danh sách IDs cần xóa chứa ID sai
         * Expected Output: AppException với mã DEPARTMENT_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_DEPT_010_nemException_KhiIdKhongTonTai() {
            Long orgId = 100L;
            List<Long> idsToDel = List.of(1L, 999L);

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdsAndOrgId(idsToDel, orgId))
                    .thenReturn(List.of(taoDepartmentEntity(1L, "D1", "D1", orgId)));

            assertThatThrownBy(() -> departmentService.deleteByIds(idsToDel))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(CoreErrorCode.DEPARTMENT_NOT_FOUND));

            verify(departmentRepository, never()).deleteAll(any());
        }
    }

    @Nested
    @DisplayName("filter()")
    class LocDepartment {

        /**
         * Test Case ID: TC-DEPT-011
         * Test Objective: Lọc trả về dữ liệu đúng chuẩn
         * Input: Bộ lọc DepartmentFilterRequest
         * Expected Output: FilterResponse chứa danh sách DepartmentDto
         * Notes:
         */
        @Test
        void TC_DEPT_011_traVeDuLieu_KhiCoKetQua() {
            Long orgId = 100L;
            DepartmentFilterRequest req = new DepartmentFilterRequest();

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            Page<Department> page = new PageImpl<>(List.of(taoDepartmentEntity(1L, "HR", "HR", orgId)));
            when(departmentRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            FilterResponse<DepartmentDto> result = departmentService.filter(req);

            assertThat(result).isNotNull();
            assertThat(req.getOrgId()).isEqualTo(orgId);
            verify(departmentRepository, times(1)).filter(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("TC-DEPT-011B: filter() có format lại Date")
        void TC_DEPT_011B_filterCoNgayThang() {
            Long orgId = 100L;
            DepartmentFilterRequest req = new DepartmentFilterRequest();
            req.setCreatedAtEnd(java.time.Instant.now());
            req.setUpdatedAtEnd(java.time.Instant.now());

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            Page<Department> page = new PageImpl<>(Collections.emptyList());
            when(departmentRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            departmentService.filter(req);
            verify(departmentRepository, times(1)).filter(any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findById() và Update edge cases")
    class FindByIdAndEdgeCases {
        @Test
        void TC_DEPT_012B_findByIdRong() {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());
            DepartmentDto result = departmentService.findById(999L);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TC-DEPT-013: update() thành công khi giữ nguyên code")
        void TC_DEPT_013_updateThanhCong_KhiGiuNguyenCode() {
            Long orgId = 100L;
            Department existing = taoDepartmentEntity(1L, "HR", "HR", orgId);
            DepartmentRequest request = taoDepartmentRequest(1L, "HR", "HR New Name");

            when(restTemplateClient.validOrgMember()).thenReturn(orgId);
            when(departmentRepository.findByIdAndOrgId(1L, orgId)).thenReturn(Optional.of(existing));
            when(departmentRepository.existsByCodeAndOrgId("HR", orgId)).thenReturn(true);

            departmentService.update(request);
            verify(departmentRepository).save(existing);
        }
    }
}

