package com.cvconnect.utils;

/**
 * ============================================================
 * FILE: CoreServiceUtilsTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link com.cvconnect.utils.CoreServiceUtils}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - Validate file ảnh / tài liệu
 *   - Validate dữ liệu email thủ công
 *   - Chuyển đổi chuỗi và thời gian (toSnakeCase, timezone)
 * ============================================================
 */

import com.cvconnect.utils.CoreServiceUtils;
import nmquan.commonlib.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CoreServiceUtils - Unit Tests")
public class CoreServiceUtilsTest {

    /**
     * Test Case ID: TC-UTIL-001
     * Test Objective: Validate ảnh lỗi khi thiếu content type.
     * Input: MockMultipartFile contentType=null.
     * Expected Output: Ném AppException.
     * Notes: Negative validation branch.
     */
    @Test
    @DisplayName("validateImageFileInput: throw when content type is null")
    void validateImageFileInput_nullContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "image.bin", null, new byte[]{1});

        assertThrows(AppException.class, () -> CoreServiceUtils.validateImageFileInput(file));
    }

    /**
     * Test Case ID: TC-UTIL-002
     * Test Objective: Validate ảnh lỗi khi content type không hợp lệ.
     * Input: file image/gif.
     * Expected Output: Ném AppException.
     * Notes: Unsupported mime type branch.
     */
    @Test
    @DisplayName("validateImageFileInput: throw when content type is invalid")
    void validateImageFileInput_invalidContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "image.gif", "image/gif", new byte[]{1});

        assertThrows(AppException.class, () -> CoreServiceUtils.validateImageFileInput(file));
    }

    /**
     * Test Case ID: TC-UTIL-003
     * Test Objective: Validate ảnh pass với mime type hợp lệ.
     * Input: image/jpeg và image/png.
     * Expected Output: Không ném exception.
     * Notes: Parameterized happy path.
     */
    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png"})
    @DisplayName("validateImageFileInput: accept jpeg and png")
    void validateImageFileInput_validTypes_shouldPass(String contentType) {
        MockMultipartFile file = new MockMultipartFile("file", "image", contentType, new byte[]{1});

        assertDoesNotThrow(() -> CoreServiceUtils.validateImageFileInput(file));
    }

    /**
     * Test Case ID: TC-UTIL-004
     * Test Objective: Validate tài liệu lỗi khi thiếu content type.
     * Input: file contentType=null.
     * Expected Output: Ném AppException.
     * Notes: Negative validation branch.
     */
    @Test
    @DisplayName("validateDocumentFileInput: throw when content type is null")
    void validateDocumentFileInput_nullContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.bin", null, new byte[]{1});

        assertThrows(AppException.class, () -> CoreServiceUtils.validateDocumentFileInput(file));
    }

    /**
     * Test Case ID: TC-UTIL-005
     * Test Objective: Validate tài liệu lỗi khi mime type không được hỗ trợ.
     * Input: text/plain.
     * Expected Output: Ném AppException.
     * Notes: Unsupported document mime.
     */
    @Test
    @DisplayName("validateDocumentFileInput: throw when content type is invalid")
    void validateDocumentFileInput_invalidContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", new byte[]{1});

        assertThrows(AppException.class, () -> CoreServiceUtils.validateDocumentFileInput(file));
    }

        /**
         * Test Case ID: TC-UTIL-006
         * Test Objective: Validate tài liệu pass với mime type hợp lệ.
         * Input: pdf/doc/docx.
         * Expected Output: Không ném exception.
         * Notes: Parameterized positive path.
         */
        @ParameterizedTest
    @ValueSource(strings = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    })
    @DisplayName("validateDocumentFileInput: accept pdf/doc/docx")
    void validateDocumentFileInput_validTypes_shouldPass(String contentType) {
        MockMultipartFile file = new MockMultipartFile("file", "doc", contentType, new byte[]{1});

        assertDoesNotThrow(() -> CoreServiceUtils.validateDocumentFileInput(file));
    }

    /**
     * Test Case ID: TC-UTIL-007
     * Test Objective: Validate manual email lỗi khi subject rỗng.
     * Input: subject="", template="template".
     * Expected Output: Ném AppException.
     * Notes: Required field validation.
     */
    @Test
    @DisplayName("validateManualEmail: throw when subject is empty")
    void validateManualEmail_emptySubject_shouldThrow() {
        assertThrows(AppException.class, () -> CoreServiceUtils.validateManualEmail("", "template"));
    }

    /**
     * Test Case ID: TC-UTIL-008
     * Test Objective: Validate manual email lỗi khi template rỗng.
     * Input: subject hợp lệ, template="".
     * Expected Output: Ném AppException.
     * Notes: Required field validation.
     */
    @Test
    @DisplayName("validateManualEmail: throw when template is empty")
    void validateManualEmail_emptyTemplate_shouldThrow() {
        assertThrows(AppException.class, () -> CoreServiceUtils.validateManualEmail("subject", ""));
    }

    /**
     * Test Case ID: TC-UTIL-009
     * Test Objective: Validate manual email pass khi dữ liệu hợp lệ.
     * Input: subject và template đều có nội dung.
     * Expected Output: Không ném exception.
     * Notes: Positive path.
     */
    @Test
    @DisplayName("validateManualEmail: pass when subject and template are valid")
    void validateManualEmail_validInput_shouldPass() {
        assertDoesNotThrow(() -> CoreServiceUtils.validateManualEmail("subject", "template"));
    }

    /**
     * Test Case ID: TC-UTIL-010
     * Test Objective: toSnakeCase xử lý null input.
     * Input: null.
     * Expected Output: null.
     * Notes: Null-safe branch.
     */
    @Test
    @DisplayName("toSnakeCase: return input when null")
    void toSnakeCase_null_shouldReturnNull() {
        assertThat(CoreServiceUtils.toSnakeCase(null)).isNull();
    }

    /**
     * Test Case ID: TC-UTIL-011
     * Test Objective: toSnakeCase xử lý chuỗi rỗng.
     * Input: "".
     * Expected Output: "".
     * Notes: Empty input branch.
     */
    @Test
    @DisplayName("toSnakeCase: return input when empty")
    void toSnakeCase_empty_shouldReturnEmpty() {
        assertThat(CoreServiceUtils.toSnakeCase("")).isEmpty();
    }

    /**
     * Test Case ID: TC-UTIL-012
     * Test Objective: toSnakeCase chuyển camelCase sang snake_case.
     * Input: "jobAdId".
     * Expected Output: "job_ad_id".
     * Notes: Core conversion logic.
     */
    @Test
    @DisplayName("toSnakeCase: convert camelCase to snake_case")
    void toSnakeCase_camelCase_shouldConvert() {
        assertThat(CoreServiceUtils.toSnakeCase("jobAdId")).isEqualTo("job_ad_id");
    }

    /**
     * Test Case ID: TC-UTIL-013
     * Test Objective: Convert LocalDateTime sang Instant theo timezone truyền vào.
     * Input: LocalDateTime + ZoneId cụ thể.
     * Expected Output: Instant đúng mốc UTC tương ứng.
     * Notes: Timezone conversion branch.
     */
    @Test
    @DisplayName("convertLocalDateTimeToInstant: convert across time zones")
    void convertLocalDateTimeToInstant_shouldConvertAcrossZones() {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 4, 15, 10, 30);
        Instant result = CoreServiceUtils.convertLocalDateTimeToInstant(
                localDateTime,
                ZoneId.of("Asia/Ho_Chi_Minh"),
                ZoneId.of("UTC")
        );

        assertThat(result).isEqualTo(Instant.parse("2026-04-15T03:30:00Z"));
    }
}

