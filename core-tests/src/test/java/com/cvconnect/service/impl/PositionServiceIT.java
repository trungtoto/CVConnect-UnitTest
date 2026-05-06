package com.cvconnect.service.impl;

import com.cvconnect.dto.position.PositionRequest;
import com.cvconnect.entity.Position;
import com.cvconnect.entity.Department;
import com.cvconnect.repository.PositionRepository;
import com.cvconnect.repository.DepartmentRepository;
import com.cvconnect.service.PositionService;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
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
 * Integration Test cho PositionService.
 * Đáp ứng 2 điều kiện:
 * 1. CheckDB: Kiểm tra dữ liệu thực tế được lưu vào PostgreSQL.
 * 2. Rollback: Tự động xóa dữ liệu test sau khi chạy xong nhờ @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PositionServiceIT {

    @Autowired
    private PositionService positionService;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Test Case ID: IT-POS-001
     * Test Objective: Tạo Vị trí mới liên kết với Phòng ban và kiểm tra DB
     * Input: PositionRequest hợp lệ kèm departmentId
     * Expected Output: Bản ghi tồn tại trong bảng 'position'
     */
    @Test
    @DisplayName("CheckDB: Tạo Vị trí mới thành công")
    void IT_POS_001_createPosition_ShouldSaveToDB() {
        Department dept = new Department();
        dept.setCode("DEPT_01");
        dept.setName("Engineering");
        dept.setOrgId(1L);
        dept = departmentRepository.save(dept);

        PositionRequest request = PositionRequest.builder()
                .code("DEV_JAVA")
                .name("Senior Java Developer")
                .departmentId(dept.getId())
                .build();

        IDResponse<Long> response = positionService.create(request);

        assertThat(response.getId()).isNotNull();
        Optional<Position> dbRecord = positionRepository.findById(response.getId());
        assertThat(dbRecord).isPresent();
        assertThat(dbRecord.get().getCode()).isEqualTo("DEV_JAVA");
        assertThat(dbRecord.get().getDepartmentId()).isEqualTo(dept.getId());
    }

    /**
     * Test Case ID: IT-POS-002
     * Test Objective: Thay đổi trạng thái Vị trí và xác nhận DB
     * Input: Position ID và trạng thái mới
     * Expected Output: Cột 'is_active' được cập nhật đúng
     */
    @Test
    @DisplayName("CheckDB: Thay đổi trạng thái Vị trí")
    void IT_POS_002_changeStatus_ShouldUpdateDB() {
        Position pos = new Position();
        pos.setCode("TEST_POS");
        pos.setName("Test Position");
        pos.setDepartmentId(99L);
        pos.setIsActive(true);
        pos = positionRepository.save(pos);

        positionService.changeStatusActive(ChangeStatusActiveRequest.builder()
                .ids(List.of(pos.getId()))
                .active(false)
                .build());

        Position updated = positionRepository.findById(pos.getId()).orElseThrow(() -> new RuntimeException("Not found"));
        assertThat(updated.getIsActive()).isFalse();
    }
}
