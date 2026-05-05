package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: ProcessTypeServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.ProcessTypeServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - detail()
 *   - changeProcessType()
 *   - getAllProcessType()
 * ============================================================
 */

import com.cvconnect.dto.processType.ProcessTypeDto;
import com.cvconnect.dto.processType.ProcessTypeRequest;
import com.cvconnect.entity.ProcessType;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.ProcessTypeRepository;
import com.cvconnect.service.impl.ProcessTypeServiceImpl;
import nmquan.commonlib.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessTypeServiceImpl - Unit Tests")
public class ProcessTypeServiceImplTest {

    @Mock
    private ProcessTypeRepository processTypeRepository;

    @InjectMocks
    private ProcessTypeServiceImpl processTypeService;

    private ProcessType taoProcessType(Long id, String code, Boolean isDefault) {
        ProcessType pt = new ProcessType();
        pt.setId(id);
        pt.setCode(code);
        pt.setName("Process " + code);
        pt.setIsDefault(isDefault);
        pt.setSortOrder(1);
        return pt;
    }

    private ProcessTypeRequest taoProcessTypeRequest(Long id, String code) {
        ProcessTypeRequest req = new ProcessTypeRequest();
        req.setId(id);
        req.setCode(code);
        req.setName("Process " + code);
        req.setSortOrder(1);
        return req;
    }

    @Nested
    @DisplayName("detail()")
    class ChiTietProcessType {

        /**
         * Test Case ID: TC-PROC-001
         * Test Objective: Detail trả về DTO khi id tồn tại (nhánh != null)
         * Input: Id hợp lệ
         * Expected Output: ProcessTypeDto
         * Notes: C2 Coverage
         */
        @Test
        void TC_PROC_001_traVeDto_KhiTonTai() {
            ProcessType pt = taoProcessType(1L, "APPLY", true);
            when(processTypeRepository.findById(1L)).thenReturn(Optional.of(pt));

            ProcessTypeDto result = processTypeService.detail(1L);
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("APPLY");
        }

        /**
         * Test Case ID: TC-PROC-002
         * Test Objective: Detail trả về null khi Optional empty (nhánh == null)
         * Input: Id không tồn tại
         * Expected Output: null
         * Notes: C2 Coverage
         */
        @Test
        void TC_PROC_002_traVeNull_KhiKhongTonTai() {
            when(processTypeRepository.findById(99L)).thenReturn(Optional.empty());

            ProcessTypeDto result = processTypeService.detail(99L);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("changeProcessType()")
    class ThayDoiProcessType {

        /**
         * Test Case ID: TC-PROC-003
         * Test Objective: Ném lỗi khi cố tình xóa Default process
         * Input: List request thiếu id của default process (để nó bị quy vào diện logic xoá deleteList)
         * Expected Output: AppException (PROCESS_TYPE_CANNOT_DELETE_DEFAULT)
         * Notes: Test nhánh kiểm tra không cho phép xóa process mặc định
         */
        @Test
        void TC_PROC_003_nemException_KhiXoaDefaultProcess() {
            // Trong DB có 1 process default là id 1
            ProcessType ptDefault = taoProcessType(1L, "APPLY", true);
            when(processTypeRepository.findAll()).thenReturn(List.of(ptDefault));

            // Request ko chứa id 1, nghĩa là đang xúi giục repository xóa id 1
            List<ProcessTypeRequest> request = List.of(taoProcessTypeRequest(2L, "INTERVIEW"));

            assertThatThrownBy(() -> processTypeService.changeProcessType(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.PROCESS_TYPE_CANNOT_DELETE_DEFAULT));
            
            verify(processTypeRepository, never()).deleteAllById(any());
        }

        /**
         * Test Case ID: TC-PROC-004
         * Test Objective: Insert thêm throws lỗi quá trình duplicated
         * Input: Thêm 1 process mới (id = null) có trùng Code với Process đang có trong DB
         * Expected Output: PROCESS_TYPE_CODE_DUPLICATED
         * Notes: Test nhánh duplicated trong quá trình Insert (filter r.getId() == null)
         */
        @Test
        void TC_PROC_004_nemException_KhiInsertThemBiTrungCode() {
            ProcessType ptExist = taoProcessType(1L, "APPLY", false);
            when(processTypeRepository.findAll()).thenReturn(List.of(ptExist));

            // Có Id 1 để khỏi bị xóa mất + thêm mới r.getId = null bị trùng Code "APPLY"
            List<ProcessTypeRequest> request = List.of(
                    taoProcessTypeRequest(1L, "APPLY"),
                    taoProcessTypeRequest(null, "APPLY")
            );

            assertThatThrownBy(() -> processTypeService.changeProcessType(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.PROCESS_TYPE_CODE_DUPLICATED));
        }

        /**
         * Test Case ID: TC-PROC-005
         * Test Objective: Delete bản ghi non-default và insert bản ghi mới thỏa mãn
         * Input: Request chứa 1 cái mới (nhánh insert valid) ko chứa 1 cái cũ ko phải default (xoá old valid)
         * Expected Output: Gọi saveAll các Insert
         * Notes: C2 Coverage
         */
        @Test
        void TC_PROC_005_xoaVaInsertThanhCong() {
            // DB hiện tại có id 1 non-default
            ProcessType ptOld = taoProcessType(1L, "OLD", false);
            when(processTypeRepository.findAll()).thenReturn(java.util.List.of(ptOld)); // Dùng List array thông thường để service dễ modify

            // Request không có id 1 (xóa OLD), nhưng có id null (insert NEW)
            List<ProcessTypeRequest> request = List.of(
                    taoProcessTypeRequest(null, "NEW_PROCESS")
            );

            processTypeService.changeProcessType(request);

            // Xác nhận xóa DB id 1
            verify(processTypeRepository, times(1)).deleteAllById(List.of(1L));
            // Service gọi saveAll 2 lần: 1 lần cho insert, 1 lần cho update (list rỗng, nhưng vẫn gọi)
            verify(processTypeRepository, times(2)).saveAll(any());
        }

        /**
         * Test Case ID: TC-PROC-006
         * Test Objective: Cập nhật lỗi do đổi thành code đã bị trùng (nhưng khác chính nó)
         * Input: Request đổi code sang một code existing
         * Expected Output: PROCESS_TYPE_CODE_DUPLICATED
         * Notes: Nhánh lỗi Update khi codeExist.contains = true
         */
        @Test
        void TC_PROC_006_nemException_KhiUpdateBiTrungCodeTheoBanGhiKhac() {
            ProcessType p1 = taoProcessType(1L, "APPLY", false);
            ProcessType p2 = taoProcessType(2L, "ONBOARD", false);
            when(processTypeRepository.findAll()).thenReturn(List.of(p1, p2));

            // request kêu giữ p1, đồng thời đè p2 đổi sang mã APPLY
            List<ProcessTypeRequest> request = List.of(
                    taoProcessTypeRequest(1L, "APPLY"),
                    taoProcessTypeRequest(2L, "APPLY")
            );

            assertThatThrownBy(() -> processTypeService.changeProcessType(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException)ex).getErrorCode()).isEqualTo(CoreErrorCode.PROCESS_TYPE_CODE_DUPLICATED));
        }

        /**
         * Test Case ID: TC-PROC-007
         * Test Objective: Cập nhật thành công thông thường
         * Input: Request update lại thông tin
         * Expected Output: DB update chuẩn chỉ
         * Notes: C2 Coverage
         */
        @Test
        void TC_PROC_007_cappNhatValidThanhCong() {
            ProcessType p1 = taoProcessType(1L, "APPLY", false);
            when(processTypeRepository.findAll()).thenReturn(List.of(p1));

            // Update mã thành APPLY-NEW
            List<ProcessTypeRequest> request = List.of(taoProcessTypeRequest(1L, "APPLY-NEW"));

            processTypeService.changeProcessType(request);

            verify(processTypeRepository, times(1)).deleteAllById(Collections.emptyList());
            verify(processTypeRepository, times(2)).saveAll(any()); // Insert & Update (dù rỗng cũng call saveAll list rỗng)
        }
    }

    @Nested
    @DisplayName("getAllProcessType()")
    class LayTatCa {

        /**
         * Test Case ID: TC-PROC-008
         * Test Objective: getAll lấy danh sách sắp xếp theo sortOrder
         * Input: DB có mảng lộn xộn các process type
         * Expected Output: List DTO được xếp đúng sortOrder
         * Notes:
         */
        @Test
        void TC_PROC_008_traVeDanhSach_KhiLayTatCa() {
            ProcessType p1 = taoProcessType(1L, "P1", false);
            p1.setSortOrder(2);
            ProcessType p2 = taoProcessType(2L, "P2", true);
            p2.setSortOrder(1);

            when(processTypeRepository.findAll()).thenReturn(List.of(p1, p2));

            List<ProcessTypeDto> result = processTypeService.getAllProcessType();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("P2"); // sortOrder 1 nhảy lên đầu
            assertThat(result.get(1).getCode()).isEqualTo("P1");
        }
    }
}

