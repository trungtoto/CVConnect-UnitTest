package com.cvconnect.service.impl;

import com.cvconnect.dto.org.OrganizationRequest;
import com.cvconnect.entity.Organization;
import com.cvconnect.repository.OrgRepository;
import com.cvconnect.service.OrgService;
import com.cvconnect.service.AttachFileService;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import nmquan.commonlib.dto.response.IDResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration Test cho OrgService.
 * Đáp ứng 2 điều kiện:
 * 1. CheckDB: Kiểm tra dữ liệu thực tế được lưu vào PostgreSQL.
 * 2. Rollback: Tự động xóa dữ liệu test sau khi chạy xong nhờ @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrgServiceIT {

    @Autowired
    private OrgService orgService;

    @Autowired
    private OrgRepository orgRepository;

    @MockBean
    private AttachFileService attachFileService;

    /**
     * Test Case ID: IT-ORG-001
     * Test Objective: Tạo Tổ chức mới và kiểm tra lưu DB thành công
     * Input: OrganizationRequest hợp lệ
     * Expected Output: Bản ghi tồn tại trong bảng 'organization'
     */
    @Test
    @DisplayName("CheckDB: Tạo Tổ chức mới thành công")
    void IT_ORG_001_createOrg_ShouldSaveToDB() {
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Global Tech Corp")
                .description("A leading tech company")
                .website("https://globaltech.com")
                .build();

        when(attachFileService.uploadFile(any())).thenReturn(List.of());

        IDResponse<Long> response = orgService.createOrg(request, null);

        assertThat(response.getId()).isNotNull();
        Optional<Organization> dbRecord = orgRepository.findById(response.getId());
        assertThat(dbRecord).isPresent();
        assertThat(dbRecord.get().getName()).isEqualTo("Global Tech Corp");
    }

    /**
     * Test Case ID: IT-ORG-002
     * Test Objective: Kiểm tra thay đổi trạng thái Active của Tổ chức
     * Input: Org ID và trạng thái mới
     * Expected Output: Cột 'is_active' trong DB được cập nhật đúng
     */
    @Test
    @DisplayName("CheckDB: Thay đổi trạng thái hoạt động của Tổ chức")
    void IT_ORG_002_changeStatus_ShouldUpdateDB() {
        Organization org = new Organization();
        org.setName("Status Test Org");
        org.setIsActive(false);
        org = orgRepository.save(org);

        orgService.changeStatusActive(ChangeStatusActiveRequest.builder()
                .ids(List.of(org.getId()))
                .active(true)
                .build());

        Organization updated = orgRepository.findById(org.getId()).orElseThrow(() -> new RuntimeException("Not found"));
        assertThat(updated.getIsActive()).isTrue();
    }
}
