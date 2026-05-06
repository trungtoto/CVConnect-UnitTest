package com.cvconnect.service.impl;

import com.cvconnect.dto.career.CareerRequest;
import com.cvconnect.entity.Careers;
import com.cvconnect.repository.CareerRepository;
import com.cvconnect.service.CareerService;
import nmquan.commonlib.dto.response.IDResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  * ============================================================
 * FILE: CareerServiceIT.java
 * MODULE: core-service
 * MỤC ĐÍCH: Integration Test cho CareerService
 * ĐIỀU KIỆN: CheckDB (PostgreSQL) & Tự động Rollback
 * ============================================================
 * Integration Test cho CareerService.
 * Đáp ứng 2 điều kiện:
 * 1. CheckDB: Kiểm tra dữ liệu thực tế được lưu vào PostgreSQL.
 * 2. Rollback: Tự động xóa dữ liệu test sau khi chạy xong nhờ @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CareerServiceIT {

    @Autowired
    private CareerService careerService;

    @Autowired
    private CareerRepository careerRepository;

    /**
     * Test Case ID: IT-CR-001
     * Test Objective: Tạo Career mới và kiểm tra trong Database
     * Input: CareerRequest hợp lệ
     * Expected Output: Bản ghi được lưu vào bảng 'careers'
     */
    @Test
    @DisplayName("CheckDB: Tạo Career mới thành công")
    void IT_CR_001_createCareer_ShouldSaveToDB() {
        CareerRequest request = CareerRequest.builder()
                .code("IT_DEV")
                .name("Software Development")
                .build();

        IDResponse<Long> response = careerService.create(request);

        assertThat(response.getId()).isNotNull();
        Optional<Careers> dbRecord = careerRepository.findById(response.getId());
        assertThat(dbRecord).isPresent();
        assertThat(dbRecord.get().getCode()).isEqualTo("IT_DEV");
    }

    /**
     * Test Case ID: IT-CR-002
     * Test Objective: Cập nhật thông tin Career trong Database
     * Input: CareerRequest với ID đã tồn tại
     * Expected Output: Dữ liệu trong Database được thay đổi đúng
     */
    @Test
    @DisplayName("CheckDB: Cập nhật Career hiện có")
    void IT_CR_002_updateCareer_ShouldModifyDB() {
        // Tạo sẵn data
        Careers career = new Careers();
        career.setCode("OLD_CODE");
        career.setName("Old Name");
        career = careerRepository.save(career);

        CareerRequest updateRequest = CareerRequest.builder()
                .id(career.getId())
                .code("NEW_CODE")
                .name("New Name")
                .build();

        careerService.update(updateRequest);

        Careers updated = careerRepository.findById(career.getId()).orElseThrow(() -> new RuntimeException("Not found"));
        assertThat(updated.getCode()).isEqualTo("NEW_CODE");
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    /**
     * Test Case ID: IT-CR-003
     * Test Objective: Xóa nhiều Career và xác nhận DB đã trống
     * Input: Danh sách IDs cần xóa
     * Expected Output: Không tìm thấy bản ghi trong DB sau khi xóa
     */
    @Test
    @DisplayName("CheckDB: Xóa Career theo danh sách IDs")
    void IT_CR_003_deleteCareers_ShouldRemoveFromDB() {
        Careers c1 = new Careers();
        c1.setCode("C1");
        c1.setName("Career 1");
        c1 = careerRepository.save(c1);

        Careers c2 = new Careers();
        c2.setCode("C2");
        c2.setName("Career 2");
        c2 = careerRepository.save(c2);
        
        List<Long> ids = List.of(c1.getId(), c2.getId());

        careerService.deleteByIds(ids);

        assertThat(careerRepository.findAllById(ids)).isEmpty();
    }
}
