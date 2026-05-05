package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CareerServiceImplTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.service.impl.CareerServiceImpl}
 *
 * CÁC PHƯƠNG THỨC ĐƯỢC TEST:
 *   - deleteByIds()
 *   - getCareerDetail()
 *   - create()
 *   - update()
 *   - filter()
 * ============================================================
 */

import com.cvconnect.dto.career.CareerDto;
import com.cvconnect.dto.career.CareerFilterRequest;
import com.cvconnect.dto.career.CareerRequest;
import com.cvconnect.entity.Careers;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.CareerRepository;
import com.cvconnect.service.impl.CareerServiceImpl;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("CareerServiceImpl - Unit Tests")
public class CareerServiceImplTest {

    @Mock
    private CareerRepository careerRepository;

    @InjectMocks
    private CareerServiceImpl careerService;

    private Careers taoCareerEntity(Long careerId, String careerCode, String careerName) {
        Careers career = new Careers();
        career.setId(careerId);
        career.setCode(careerCode);
        career.setName(careerName);
        return career;
    }

    private CareerRequest taoCareerRequest(Long careerId, String careerCode, String careerName) {
        return CareerRequest.builder()
                .id(careerId)
                .code(careerCode)
                .name(careerName)
                .build();
    }

    @Nested
    @DisplayName("deleteByIds()")
    class XoaCareerTheoIds {

        /**
         * Test Case ID: TC-CAREER-001
         * Test Objective: Xóa thành công khi danh sách ids hợp lệ
         * Input: Danh sách ids hợp lệ (không rỗng, không null)
         * Expected Output: Gọi repository.deleteAllById() đúng 1 lần với danh sách ids truyền vào
         * Notes: Kiểm tra thao tác ghi (xóa) DB.
         */
        @Test
        void TC_CAREER_001_xoaThanhCong_KhiIdsHopLe() {
            List<Long> danhSachIdsCanXoa = List.of(1L, 2L, 3L);
            careerService.deleteByIds(danhSachIdsCanXoa);
            verify(careerRepository, times(1)).deleteAllById(danhSachIdsCanXoa);
        }

        /**
         * Test Case ID: TC-CAREER-002
         * Test Objective: Không gọi DB khi ids = null
         * Input: ids = null
         * Expected Output: Bỏ qua thao tác xóa, không throw exception
         * Notes:
         */
        @Test
        void TC_CAREER_002_boQua_KhiIdsLaNull() {
            careerService.deleteByIds(null);
            verify(careerRepository, never()).deleteAllById(any());
        }

        /**
         * Test Case ID: TC-CAREER-003
         * Test Objective: Không gọi DB khi ids rỗng
         * Input: Danh sách ids rỗng
         * Expected Output: Bỏ qua thao tác xóa
         * Notes:
         */
        @Test
        void TC_CAREER_003_boQua_KhiIdsDanhSachRong() {
            careerService.deleteByIds(Collections.emptyList());
            verify(careerRepository, never()).deleteAllById(any());
        }
    }

    @Nested
    @DisplayName("getCareerDetail()")
    class LayChiTietCareer {

        /**
         * Test Case ID: TC-CAREER-004
         * Test Objective: Trả về CareerDto khi tìm thấy career
         * Input: careerId hợp lệ
         * Expected Output: Đối tượng CareerDto
         * Notes:
         */
        @Test
        void TC_CAREER_004_traVeCareerDto_KhiTimThay() {
            Long careerId = 1L;
            Careers careerTrongDB = taoCareerEntity(careerId, "IT", "Information Technology");
            when(careerRepository.findById(careerId)).thenReturn(Optional.of(careerTrongDB));

            CareerDto ketQua = careerService.getCareerDetail(careerId);

            assertThat(ketQua).isNotNull();
            verify(careerRepository, times(1)).findById(careerId);
        }

        /**
         * Test Case ID: TC-CAREER-005
         * Test Objective: Ném exception khi không tìm thấy career
         * Input: careerId không tồn tại
         * Expected Output: AppException với mã lỗi CAREER_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_CAREER_005_nemException_KhiKhongTimThay() {
            Long careerIdKhongTonTai = 999L;
            when(careerRepository.findById(careerIdKhongTonTai)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerService.getCareerDetail(careerIdKhongTonTai))
                    .isInstanceOf(AppException.class)
                    .satisfies(exception -> {
                        AppException appException = (AppException) exception;
                        assertThat(appException.getErrorCode())
                                .isEqualTo(CoreErrorCode.CAREER_NOT_FOUND);
                    });
            verify(careerRepository, times(1)).findById(careerIdKhongTonTai);
        }
    }

    @Nested
    @DisplayName("create()")
    class TaoMoiCareer {

        /**
         * Test Case ID: TC-CAREER-006
         * Test Objective: Tạo career thành công khi code chưa tồn tại
         * Input: CareerRequest hợp lệ (code mới)
         * Expected Output: ID của career vừa được tạo
         * Notes: Kiểm tra thao tác ghi DB bằng ArgumentCaptor.
         */
        @Test
        void TC_CAREER_006_taoThanhCong_KhiCodeChuaTonTai() {
            CareerRequest duLieuDauVao = taoCareerRequest(null, "FIN", "Finance");

            when(careerRepository.findAllByCodeIn(Collections.singletonList("FIN")))
                    .thenReturn(Collections.emptyList());

            when(careerRepository.save(any(Careers.class))).thenAnswer(invocation -> {
                Careers entityDuocLuu = invocation.getArgument(0);
                entityDuocLuu.setId(10L);
                return entityDuocLuu;
            });

            IDResponse<Long> ketQua = careerService.create(duLieuDauVao);

            assertThat(ketQua).isNotNull();
            assertThat(ketQua.getId()).isEqualTo(10L);

            ArgumentCaptor<Careers> careerCaptor = ArgumentCaptor.forClass(Careers.class);
            verify(careerRepository, times(1)).save(careerCaptor.capture());

            Careers entityDaLuu = careerCaptor.getValue();
            assertThat(entityDaLuu.getCode()).isEqualTo("FIN");
            assertThat(entityDaLuu.getName()).isEqualTo("Finance");
        }

        /**
         * Test Case ID: TC-CAREER-007
         * Test Objective: Ném lỗi khi code đã tồn tại
         * Input: CareerRequest với code đã có trong DB
         * Expected Output: AppException với mã lỗi CAREER_CODE_EXISTS
         * Notes: Không thực hiện ghi DB
         */
        @Test
        void TC_CAREER_007_nemException_KhiCodeDaTonTai() {
            CareerRequest duLieuDauVao = taoCareerRequest(null, "IT", "Information Technology");
            Careers careerDaTonTaiTrongDB = taoCareerEntity(1L, "IT", "IT");

            when(careerRepository.findAllByCodeIn(Collections.singletonList("IT")))
                    .thenReturn(List.of(careerDaTonTaiTrongDB));

            assertThatThrownBy(() -> careerService.create(duLieuDauVao))
                    .isInstanceOf(AppException.class)
                    .satisfies(exception -> {
                        AppException appException = (AppException) exception;
                        assertThat(appException.getErrorCode())
                                .isEqualTo(CoreErrorCode.CAREER_CODE_EXISTS);
                    });

            verify(careerRepository, never()).save(any(Careers.class));
        }
    }

    @Nested
    @DisplayName("update()")
    class CapNhatCareer {

        /**
         * Test Case ID: TC-CAREER-008
         * Test Objective: Update thành công với code và name mới
         * Input: CareerRequest hợp lệ (id đang tồn tại, code mới không trùng lặp)
         * Expected Output: ID của career được update
         * Notes: Kiểm tra thao tác cập nhật DB bằng ArgumentCaptor.
         */
        @Test
        void TC_CAREER_008_updateThanhCong_KhiIdTonTaiVaCodeKhongTrung() {
            Long careerId = 1L;
            CareerRequest duLieuCapNhat = taoCareerRequest(careerId, "IT-UPDATED", "IT Updated Name");
            Careers careerHienTaiTrongDB = taoCareerEntity(careerId, "IT", "Information Technology");

            when(careerRepository.findById(careerId)).thenReturn(Optional.of(careerHienTaiTrongDB));
            when(careerRepository.findAllByCodeIn(Collections.singletonList("IT-UPDATED")))
                    .thenReturn(Collections.emptyList());
            when(careerRepository.save(any(Careers.class))).thenReturn(careerHienTaiTrongDB);

            IDResponse<Long> ketQua = careerService.update(duLieuCapNhat);

            assertThat(ketQua).isNotNull();
            assertThat(ketQua.getId()).isEqualTo(careerId);

            ArgumentCaptor<Careers> careerCaptor = ArgumentCaptor.forClass(Careers.class);
            verify(careerRepository, times(1)).save(careerCaptor.capture());

            Careers entityDaCapNhat = careerCaptor.getValue();
            assertThat(entityDaCapNhat.getCode()).isEqualTo("IT-UPDATED");
            assertThat(entityDaCapNhat.getName()).isEqualTo("IT Updated Name");
        }

        /**
         * Test Case ID: TC-CAREER-009
         * Test Objective: Update thành công khi giữ nguyên code
         * Input: CareerRequest hợp lệ (code giữ nguyên)
         * Expected Output: ID của career được update
         * Notes:
         */
        @Test
        void TC_CAREER_009_updateThanhCong_KhiGiuNguyenCode() {
            Long careerId = 1L;
            CareerRequest duLieuCapNhat = taoCareerRequest(careerId, "IT", "IT Renamed Only");
            Careers careerHienTaiTrongDB = taoCareerEntity(careerId, "IT", "Information Technology");

            when(careerRepository.findById(careerId)).thenReturn(Optional.of(careerHienTaiTrongDB));
            when(careerRepository.findAllByCodeIn(Collections.singletonList("IT")))
                    .thenReturn(List.of(careerHienTaiTrongDB));
            when(careerRepository.save(any(Careers.class))).thenReturn(careerHienTaiTrongDB);

            IDResponse<Long> ketQua = careerService.update(duLieuCapNhat);

            assertThat(ketQua.getId()).isEqualTo(careerId);
            verify(careerRepository, times(1)).save(any(Careers.class));
        }

        /**
         * Test Case ID: TC-CAREER-010
         * Test Objective: Ném lỗi khi id không tồn tại
         * Input: CareerRequest với id không có thật
         * Expected Output: AppException với mã lỗi CAREER_NOT_FOUND
         * Notes:
         */
        @Test
        void TC_CAREER_010_nemException_KhiCareerIdKhongTonTai() {
            Long careerIdKhongTonTai = 999L;
            CareerRequest duLieuCapNhat = taoCareerRequest(careerIdKhongTonTai, "IT", "IT");
            when(careerRepository.findById(careerIdKhongTonTai)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerService.update(duLieuCapNhat))
                    .isInstanceOf(AppException.class)
                    .satisfies(exception -> {
                        AppException appException = (AppException) exception;
                        assertThat(appException.getErrorCode())
                                .isEqualTo(CoreErrorCode.CAREER_NOT_FOUND);
                    });

            verify(careerRepository, never()).save(any(Careers.class));
        }

        /**
         * Test Case ID: TC-CAREER-011
         * Test Objective: Ném lỗi khi code đã được sử dụng bởi career khác
         * Input: CareerRequest với code bị trùng với id khác
         * Expected Output: AppException với mã lỗi CAREER_CODE_EXISTS
         * Notes:
         */
        @Test
        void TC_CAREER_011_nemException_KhiCodeMoiThuocCareerKhac() {
            Long careerIdDangUpdate = 1L;
            Long careerIdKhac = 2L;
            CareerRequest duLieuCapNhat = taoCareerRequest(careerIdDangUpdate, "FIN", "Finance");

            Careers careerDangUpdate = taoCareerEntity(careerIdDangUpdate, "IT", "IT");
            Careers careerKhacDungCodeFIN = taoCareerEntity(careerIdKhac, "FIN", "Finance");

            when(careerRepository.findById(careerIdDangUpdate)).thenReturn(Optional.of(careerDangUpdate));
            when(careerRepository.findAllByCodeIn(Collections.singletonList("FIN")))
                    .thenReturn(List.of(careerKhacDungCodeFIN));

            assertThatThrownBy(() -> careerService.update(duLieuCapNhat))
                    .isInstanceOf(AppException.class)
                    .satisfies(exception -> {
                        AppException appException = (AppException) exception;
                        assertThat(appException.getErrorCode())
                                .isEqualTo(CoreErrorCode.CAREER_CODE_EXISTS);
                    });

            verify(careerRepository, never()).save(any(Careers.class));
        }
    }

    @Nested
    @DisplayName("filter()")
    class LocDanhSachCareer {

        /**
         * Test Case ID: TC-CAREER-012
         * Test Objective: Trả về FilterResponse khi có dữ liệu
         * Input: Bộ lọc CareerFilterRequest
         * Expected Output: Đối tượng FilterResponse chứa dữ liệu
         * Notes:
         */
        @Test
        void TC_CAREER_012_traVeDuLieu_KhiDBCoKetQua() {
            CareerFilterRequest boLocTimKiem = new CareerFilterRequest();
            Careers careerTrongDB = taoCareerEntity(1L, "IT", "IT");
            Page<Careers> trangKetQua = new PageImpl<>(List.of(careerTrongDB));

            when(careerRepository.filter(any(CareerFilterRequest.class), any(Pageable.class)))
                    .thenReturn(trangKetQua);

            FilterResponse<CareerDto> ketQua = careerService.filter(boLocTimKiem);

            assertThat(ketQua).isNotNull();
            verify(careerRepository, times(1)).filter(eq(boLocTimKiem), any(Pageable.class));
        }

        /**
         * Test Case ID: TC-CAREER-013
         * Test Objective: Trả về FilterResponse rỗng khi không có kết quả
         * Input: Bộ lọc CareerFilterRequest
         * Expected Output: Đối tượng FilterResponse rỗng
         * Notes:
         */
        @Test
        void TC_CAREER_013_traVeRong_KhiKhongCoKetQuaPhuHop() {
            CareerFilterRequest boLocTimKiem = new CareerFilterRequest();
            Page<Careers> trangKetQuaRong = new PageImpl<>(Collections.emptyList());

            when(careerRepository.filter(any(CareerFilterRequest.class), any(Pageable.class)))
                    .thenReturn(trangKetQuaRong);

            FilterResponse<CareerDto> ketQua = careerService.filter(boLocTimKiem);

            assertThat(ketQua).isNotNull();
            verify(careerRepository, times(1)).filter(eq(boLocTimKiem), any(Pageable.class));
        }

        @Test
        @DisplayName("TC-CAREER-014: filter() có format lại Date")
        void TC_CAREER_014_filterCoNgayThang() {
            CareerFilterRequest req = new CareerFilterRequest();
            req.setCreatedAtEnd(java.time.Instant.now());
            req.setUpdatedAtEnd(java.time.Instant.now());
            Page<Careers> page = new PageImpl<>(Collections.emptyList());
            when(careerRepository.filter(any(), any(Pageable.class))).thenReturn(page);

            careerService.filter(req);
            verify(careerRepository, times(1)).filter(any(), any(Pageable.class));
        }
    }
}

