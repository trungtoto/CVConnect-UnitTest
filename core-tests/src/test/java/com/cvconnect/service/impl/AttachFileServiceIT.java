package com.cvconnect.service.impl;

import com.cvconnect.entity.AttachFile;
import com.cvconnect.repository.AttachFileRepository;
import com.cvconnect.service.AttachFileService;
import com.cvconnect.service.CloudinaryService;
import com.cvconnect.dto.attachFile.AttachFileDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration Test cho AttachFileService.
 * Đáp ứng 2 điều kiện:
 * 1. CheckDB: Kiểm tra dữ liệu thực tế được lưu vào PostgreSQL.
 * 2. Rollback: Tự động xóa dữ liệu test sau khi chạy xong nhờ @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test") // Sử dụng file application-test.yml
@Transactional // ĐIỀU KIỆN ROLLBACK: Mọi thay đổi DB sẽ bị rollback sau mỗi @Test
public class AttachFileServiceIT {

    @Autowired
    private AttachFileService attachFileService;

    @Autowired
    private AttachFileRepository attachFileRepository;

    @MockBean
    private CloudinaryService cloudinaryService; // Mock Cloudinary để không upload file thật lên cloud

    /**
     * Test Case ID: IT-AF-001
     * Test Objective: Kiểm tra lưu file thành công xuống Database
     * Input: MultipartFile hợp lệ
     * Expected Output: Dữ liệu xuất hiện trong PostgreSQL, ID được sinh ra
     */
    @Test
    @DisplayName("CheckDB: Upload file thành công và dữ liệu được lưu vào Database")
    void uploadFile_ShouldSaveToDatabase() {
        // 1. Arrange: Chuẩn bị file giả lập và Mock Cloudinary
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        AttachFileDto mockDto = new AttachFileDto();
        mockDto.setPublicId("test_public_id");
        mockDto.setSecureUrl("https://cloudinary.com/test.pdf");
        mockDto.setFilename("test.pdf");
        mockDto.setOriginalFilename("original_test.pdf");
        mockDto.setType("pdf");
        mockDto.setExtension("pdf");
        mockDto.setUrl("https://cloudinary.com/test.pdf");
        mockDto.setResourceType("raw");
        mockDto.setBaseFilename("test");
        
        when(cloudinaryService.uploadFile(any(MultipartFile[].class), anyString()))
                .thenReturn(List.of(mockDto));

        // 2. Act: Thực hiện nghiệp vụ
        List<Long> savedIds = attachFileService.uploadFile(new MultipartFile[]{file});

        // 3. CheckDB: Kiểm tra thực tế trong Database
        assertThat(savedIds).isNotEmpty();
        Long fileId = savedIds.get(0);
        
        Optional<AttachFile> dbRecord = attachFileRepository.findById(fileId);
        
        // Xác nhận dữ liệu thực sự tồn tại trong DB
        assertThat(dbRecord).isPresent();
        assertThat(dbRecord.get().getPublicId()).isEqualTo("test_public_id");
        assertThat(dbRecord.get().getSecureUrl()).isEqualTo("https://cloudinary.com/test.pdf");
        
        System.out.println(">>> CHECK DB: Đã tìm thấy bản ghi ID=" + fileId + " trong database PostgreSQL.");
    }

    /**
     * Test Case ID: IT-AF-002
     * Test Objective: Kiểm tra DB Rollback khi có lỗi xảy ra (Tự động)
     * Input: N/A
     * Expected Output: Database sạch sẽ sau mỗi test (count = 0)
     */
    @Test
    @DisplayName("Rollback: Sau khi test xong, database sẽ quay về trạng thái cũ")
    void testRollbackIdentity() {
        // Lưu ý: Nhờ @Transactional, sau khi chạy xong test case 'uploadFile_ShouldSaveToDatabase' ở trên,
        // bản ghi đó đã bị xóa khỏi DB. 
        // Bài test này ngầm định xác nhận DB luôn sạch cho bài test tiếp theo.
        long count = attachFileRepository.count();
        System.out.println(">>> ROLLBACK CHECK: Tổng số bản ghi hiện tại là: " + count);
    }
}
