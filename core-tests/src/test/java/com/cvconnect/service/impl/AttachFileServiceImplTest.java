package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: AttachFileServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống quản lý Upload / Download File
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - uploadFile: Nhánh username = null (Anonymous) vs username = User
 *   - getAttachFiles: Nhánh ids.size() != attachFiles.size() (Ném lỗi)
 *   - download: Nhánh bắt Content-Types (pdf, doc, docx, png, jpg, mime bất kỳ)
 *   - getDownloadUrl: Nhánh URL build bằng Cloudinary và fallback Private
 * ============================================================
 */

import com.cloudinary.Cloudinary;
import com.cloudinary.Url;
import com.cvconnect.constant.Constants;
import com.cvconnect.dto.attachFile.AttachFileDto;
import com.cvconnect.dto.attachFile.DownloadFileDto;
import com.cvconnect.entity.AttachFile;
import com.cvconnect.enums.CoreErrorCode;
import com.cvconnect.repository.AttachFileRepository;
import com.cvconnect.service.CloudinaryService;
import com.cvconnect.service.impl.AttachFileServiceImpl;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.utils.WebUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachFileServiceImpl - Unit Tests (C2 Branch Coverage)")
class AttachFileServiceImplTest {

    @Mock private AttachFileRepository attachFileRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private AttachFileServiceImpl attachFileService;

    // We instantiate a real Cloudinary to bypass its public config fields NPE
    private Cloudinary cloudinary = new Cloudinary(Map.of(
            "cloud_name", "testCloud",
            "api_key", "testKey",
            "api_secret", "testSecret"
    ));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attachFileService, "cloudinary", cloudinary);
    }

    @Nested
    @DisplayName("1. Branch Coverage: uploadFile()")
    class UploadFileTest {
        /**
         * Test Case ID: TC-ATT-001
         * Test Objective: Validate TC_ATT_001_upload_AnonymousUser behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-001: Upload File với username = null (=> Gán mặc định Anonymous)")
        void TC_ATT_001_upload_AnonymousUser() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                webUtils.when(WebUtils::getCurrentUsername).thenReturn(null);

                MultipartFile[] files = new MultipartFile[1];
                AttachFileDto dto = new AttachFileDto(); dto.setId(100L);
                when(cloudinaryService.uploadFile(eq(files), eq(Constants.RoleCode.ANONYMOUS))).thenReturn(List.of(dto));

                List<Long> result = attachFileService.uploadFile(files);

                assertThat(result).containsExactly(100L);
            }
        }

        /**
         * Test Case ID: TC-ATT-002
         * Test Objective: Validate TC_ATT_002_upload_ValidUser behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-002: Upload File với username có thực")
        void TC_ATT_002_upload_ValidUser() {
            try (MockedStatic<WebUtils> webUtils = Mockito.mockStatic(WebUtils.class)) {
                webUtils.when(WebUtils::getCurrentUsername).thenReturn("hr_master");

                MultipartFile[] files = new MultipartFile[1];
                AttachFileDto dto = new AttachFileDto(); dto.setId(200L);
                when(cloudinaryService.uploadFile(eq(files), eq("hr_master"))).thenReturn(List.of(dto));

                List<Long> result = attachFileService.uploadFile(files);

                assertThat(result).containsExactly(200L);
            }
        }
    }

    @Nested
    @DisplayName("2. Branch Coverage: getAttachFiles()")
    class GetAttachFilesTest {
        /**
         * Test Case ID: TC-ATT-003
         * Test Objective: Validate TC_ATT_003_sizeMismatch behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-003: Throw Exception khi số lượng ID query khác dữ liệu DB (Size Mismatch)")
        void TC_ATT_003_sizeMismatch() {
            List<Long> inputIds = List.of(1L, 2L);
            AttachFile attachFile = new AttachFile(); attachFile.setId(1L);
            when(attachFileRepository.findAllById(inputIds)).thenReturn(List.of(attachFile)); // Chỉ lấy được 1, còn id=2 biến mất

            assertThatThrownBy(() -> attachFileService.getAttachFiles(inputIds))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ATTACH_FILE_NOT_FOUND));
        }

        /**
         * Test Case ID: TC-ATT-008
         * Test Objective: Validate TC_ATT_008_emptyList behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-008: Xử lý danh sách rỗng trong getAttachFiles")
        void TC_ATT_008_emptyList() {
            List<AttachFileDto> result = attachFileService.getAttachFiles(List.of());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("4. Branch Coverage: deleteByIds()")
    class DeleteTest {
        /**
         * Test Case ID: TC-ATT-006
         * Test Objective: Validate TC_ATT_006_deleteByIds_Success behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-006: Xoá danh sách file thành công")
        void TC_ATT_006_deleteByIds_Success() {
            AttachFile f1 = new AttachFile(); f1.setId(1L); f1.setPublicId("pub1");
            AttachFile f2 = new AttachFile(); f2.setId(2L); f2.setPublicId("pub2");
            List<Long> ids = List.of(1L, 2L);
            when(attachFileRepository.findAllById(ids)).thenReturn(List.of(f1, f2));

            attachFileService.deleteByIds(ids);

            verify(attachFileRepository).deleteAll(anyList());
            verify(cloudinaryService).deleteByPublicIds(anyList());
        }

        /**
         * Test Case ID: TC-ATT-007
         * Test Objective: Validate TC_ATT_007_deleteByIds_NotFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-007: Thất bại khi ID không tồn tại")
        void TC_ATT_007_deleteByIds_NotFound() {
            List<Long> ids = List.of(1L, 2L);
            when(attachFileRepository.findAllById(ids)).thenReturn(List.of()); // Không tìm thấy

            assertThatThrownBy(() -> attachFileService.deleteByIds(ids))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.ATTACH_FILE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("3. Branch Coverage: download() & Types Resolving")
    class DownloadMimeTypeTest {
        /**
         * Test Case ID: TC-ATT-004
         * Test Objective: Validate TC_ATT_004_download_MimeTypeSwitches behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-004: Download file và phân giải ContentType (pdf, doc, image, mime backup)")
        void TC_ATT_004_download_MimeTypeSwitches() {
            AttachFile f = new AttachFile(); 
            f.setId(99L); f.setOriginalFilename("test.pdf"); f.setExtension("pdf"); f.setSecureUrl("http://img/c.pdf");
            when(attachFileRepository.findById(99L)).thenReturn(Optional.of(f));
            
            // Mock RestTemplate download raw stream via getDownloadUrl
            ResponseEntity<byte[]> res = new ResponseEntity<>(new byte[]{1,2,3}, HttpStatus.OK);
            when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                .thenReturn(res);

            DownloadFileDto dtoPdf = attachFileService.download(99L);
            assertThat(dtoPdf.getContentType()).isEqualTo("application/pdf");

            // Extension docx
            f.setExtension("docx");
            DownloadFileDto dtoDocx = attachFileService.download(99L);
            assertThat(dtoDocx.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            // Extension png
            f.setExtension("png");
            DownloadFileDto dtoPng = attachFileService.download(99L);
            assertThat(dtoPng.getContentType()).isEqualTo("image/png");

            // Extension doc
            f.setExtension("doc");
            DownloadFileDto dtoDoc = attachFileService.download(99L);
            assertThat(dtoDoc.getContentType()).isEqualTo("application/msword");

            // Extension jpg
            f.setExtension("jpg");
            DownloadFileDto dtoJpg = attachFileService.download(99L);
            assertThat(dtoJpg.getContentType()).isEqualTo("image/jpeg");

            // Extension null (Fallback)
            f.setExtension(null);
            DownloadFileDto dtoNull = attachFileService.download(99L);
            assertThat(dtoNull.getContentType()).isEqualTo("application/octet-stream");
        }

        /**
         * Test Case ID: TC-ATT-005
         * Test Objective: Validate TC_ATT_005_download_RestClientException behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-005: Download file thất bại vì không lấy được data (response body null và không có private URL)")
        void TC_ATT_005_download_RestClientException() {
            AttachFile f = new AttachFile(); 
            f.setId(10L); f.setPublicId(null); f.setSecureUrl("http://url"); f.setExtension("pdf");
            when(attachFileRepository.findById(10L)).thenReturn(Optional.of(f));
            
            // Trả về response body = null => fetchFirstAvailable sẽ không lấy được dữ liệu
            // và fetchPrivateDownloadUrl cũng trả về null (vì publicId = null)
            // => download() sẽ ném AppException(DOWNLOAD_FILE_FAILED)
            ResponseEntity<byte[]> nullBodyResponse = new ResponseEntity<>(null, HttpStatus.OK);
            when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                .thenReturn(nullBodyResponse);

            assertThatThrownBy(() -> attachFileService.download(10L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DOWNLOAD_FILE_FAILED));
        }

        /**
         * Test Case ID: TC-ATT-009
         * Test Objective: Validate TC_ATT_009_download_FirstCandidateFails_SecondSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-009: Download thử URL đầu thất bại, URL sau thành công")
        void TC_ATT_009_download_FirstCandidateFails_SecondSuccess() {
            AttachFile f = new AttachFile();
            f.setId(120L);
            f.setOriginalFilename("cv.pdf");
            f.setExtension("pdf");
            f.setPublicId(null);
            f.setSecureUrl("https://res.cloudinary.com/demo/image/upload/v1/cv.pdf");
            when(attachFileRepository.findById(120L)).thenReturn(Optional.of(f));

            when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR))
                    .thenReturn(new ResponseEntity<>(new byte[]{7, 8, 9}, HttpStatus.OK));

            DownloadFileDto dto = attachFileService.download(120L);

            assertThat(dto.getAttachFileId()).isEqualTo(120L);
            assertThat(dto.getContentType()).isEqualTo("application/pdf");
            assertThat(dto.getByteArrayInputStream()).isNotNull();
        }

        /**
         * Test Case ID: TC-ATT-010
         * Test Objective: Validate TC_ATT_010_download_PrivateEndpointFallbackSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-010: Download fallback private endpoint thành công")
        void TC_ATT_010_download_PrivateEndpointFallbackSuccess() {
            AttachFile f = new AttachFile();
            f.setId(121L);
            f.setOriginalFilename("offer.docx");
            f.setExtension("docx");
            f.setPublicId("private/doc-121");
            f.setType("private");
            f.setResourceType("raw");
            f.setSecureUrl(null);
            when(attachFileRepository.findById(121L)).thenReturn(Optional.of(f));

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(Map.of("download_url", "https://private-file-url"), HttpStatus.OK));

            when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                    .thenAnswer(invocation -> {
                        java.net.URI uri = invocation.getArgument(0);
                        if ("https://private-file-url".equals(uri.toString())) {
                            return new ResponseEntity<>(new byte[]{1, 2, 3}, HttpStatus.OK);
                        }
                        throw new RestClientException("candidate url failed");
                    });

            DownloadFileDto dto = attachFileService.download(121L);

            assertThat(dto.getAttachFileId()).isEqualTo(121L);
            assertThat(dto.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }

            /**
             * Test Case ID: TC-ATT-015
             * Test Objective: Validate TC_ATT_015_download_PrivateUrlResolved_ButPrivateGetFails behavior.
             * Input: Dữ liệu mock theo ngữ cảnh test case.
             * Expected Output: Kết quả đúng theo assert/verify của test case.
             * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
             */
            @Test
            @DisplayName("TC-ATT-015: Download thất bại khi fallback private URL có nhưng GET private vẫn lỗi")
            void TC_ATT_015_download_PrivateUrlResolved_ButPrivateGetFails() {
                AttachFile f = new AttachFile();
                f.setId(122L);
                f.setOriginalFilename("private.pdf");
                f.setExtension("pdf");
                f.setPublicId("private/f122");
                f.setType("private");
                f.setResourceType("raw");
                f.setSecureUrl(null);
                when(attachFileRepository.findById(122L)).thenReturn(Optional.of(f));

                when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(Map.of("download_url", "https://private-fallback-url"), HttpStatus.OK));

                when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                    .thenThrow(new RestClientException("all candidates failed"));

                assertThatThrownBy(() -> attachFileService.download(122L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DOWNLOAD_FILE_FAILED));
            }
    }

    @Nested
    @DisplayName("5. Branch Coverage: getDownloadUrl()")
    class GetDownloadUrlTest {

        /**
         * Test Case ID: TC-ATT-011
         * Test Objective: Validate TC_ATT_011_getDownloadUrl_DocumentRawUrl behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-011: getDownloadUrl ưu tiên raw URL cho file document")
        void TC_ATT_011_getDownloadUrl_DocumentRawUrl() {
            AttachFile f = new AttachFile();
            f.setId(130L);
            f.setExtension("pdf");
            f.setSecureUrl("https://res.cloudinary.com/demo/image/upload/v1/sample.pdf");
            when(attachFileRepository.findById(130L)).thenReturn(Optional.of(f));

            String result = attachFileService.getDownloadUrl(130L);

            assertThat(result).isEqualTo("https://res.cloudinary.com/demo/raw/upload/v1/sample.pdf");
        }

        /**
         * Test Case ID: TC-ATT-012
         * Test Objective: Validate TC_ATT_012_getDownloadUrl_NoUrlFound behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-012: getDownloadUrl ném lỗi khi không có URL hợp lệ")
        void TC_ATT_012_getDownloadUrl_NoUrlFound() {
            AttachFile f = new AttachFile();
            f.setId(131L);
            f.setPublicId(null);
            f.setSecureUrl(null);
            when(attachFileRepository.findById(131L)).thenReturn(Optional.of(f));

            assertThatThrownBy(() -> attachFileService.getDownloadUrl(131L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DOWNLOAD_FILE_FAILED));
        }

        /**
         * Test Case ID: TC-ATT-013
         * Test Objective: Validate TC_ATT_013_getDownloadUrl_PrivateFallbackUrlKey behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-013: getDownloadUrl fallback private API với key 'url'")
        void TC_ATT_013_getDownloadUrl_PrivateFallbackUrlKey() {
            AttachFile f = new AttachFile();
            f.setId(132L);
            f.setPublicId("private/f132");
            f.setResourceType("raw");
            f.setType("private");
            f.setExtension("pdf");
            f.setSecureUrl(null);
            when(attachFileRepository.findById(132L)).thenReturn(Optional.of(f));

            Cloudinary spyCloudinary = Mockito.spy(new Cloudinary(Map.of(
                    "cloud_name", "testCloud",
                    "api_key", "testKey",
                    "api_secret", "testSecret"
            )));
            Url mockedUrl = Mockito.mock(Url.class);
            doReturn(mockedUrl).when(spyCloudinary).url();
            when(mockedUrl.resourceType(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.type(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.secure(true)).thenReturn(mockedUrl);
            when(mockedUrl.signed(true)).thenReturn(mockedUrl);
            when(mockedUrl.format(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.generate(anyString())).thenReturn(null);
            ReflectionTestUtils.setField(attachFileService, "cloudinary", spyCloudinary);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(Map.of("url", "https://private-url-from-url-key"), HttpStatus.OK));

            String url = attachFileService.getDownloadUrl(132L);

            assertThat(url).isEqualTo("https://private-url-from-url-key");
        }

        /**
         * Test Case ID: TC-ATT-014
         * Test Objective: Validate TC_ATT_014_getDownloadUrl_PrivateRetryAcrossEndpoints behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-014: getDownloadUrl retry endpoint/resourceType rồi lấy được private URL")
        void TC_ATT_014_getDownloadUrl_PrivateRetryAcrossEndpoints() {
            AttachFile f = new AttachFile();
            f.setId(133L);
            f.setPublicId("private/f133");
            f.setResourceType("raw");
            f.setType("private");
            f.setExtension("png");
            f.setSecureUrl(null);
            when(attachFileRepository.findById(133L)).thenReturn(Optional.of(f));

            Cloudinary spyCloudinary = Mockito.spy(new Cloudinary(Map.of(
                    "cloud_name", "testCloud",
                    "api_key", "testKey",
                    "api_secret", "testSecret"
            )));
            Url mockedUrl = Mockito.mock(Url.class);
            doReturn(mockedUrl).when(spyCloudinary).url();
            when(mockedUrl.resourceType(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.type(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.secure(true)).thenReturn(mockedUrl);
            when(mockedUrl.signed(true)).thenReturn(mockedUrl);
            when(mockedUrl.format(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.generate(anyString())).thenReturn(null);
            ReflectionTestUtils.setField(attachFileService, "cloudinary", spyCloudinary);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenAnswer(invocation -> {
                        String apiUrl = invocation.getArgument(0);
                        if (apiUrl.contains("/image/download")) {
                            throw new RestClientException("image download failed");
                        }
                        if (apiUrl.contains("/image/private_download")) {
                            return new ResponseEntity<>(Map.of(), HttpStatus.OK);
                        }
                        if (apiUrl.contains("/raw/download")) {
                            return new ResponseEntity<>(Map.of("message", "no url"), HttpStatus.OK);
                        }
                        if (apiUrl.contains("/raw/private_download")) {
                            return new ResponseEntity<>(Map.of("download_url", "https://private-url-final"), HttpStatus.OK);
                        }
                        return new ResponseEntity<>(Map.of(), HttpStatus.BAD_REQUEST);
                    });

            String url = attachFileService.getDownloadUrl(133L);

            assertThat(url).isEqualTo("https://private-url-final");
        }

        /**
         * Test Case ID: TC-ATT-016
         * Test Objective: Validate TC_ATT_016_getDownloadUrl_NonDocumentKeepsSecureUrl behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-016: getDownloadUrl với non-document giữ nguyên secureUrl image/upload")
        void TC_ATT_016_getDownloadUrl_NonDocumentKeepsSecureUrl() {
            AttachFile f = new AttachFile();
            f.setId(134L);
            f.setPublicId("images/f134");
            f.setExtension("jpg");
            f.setSecureUrl("https://res.cloudinary.com/demo/image/upload/v1/f134.jpg");
            when(attachFileRepository.findById(134L)).thenReturn(Optional.of(f));

            Cloudinary spyCloudinary = Mockito.spy(new Cloudinary(Map.of(
                    "cloud_name", "testCloud",
                    "api_key", "testKey",
                    "api_secret", "testSecret"
            )));
            Url mockedUrl = Mockito.mock(Url.class);
            doReturn(mockedUrl).when(spyCloudinary).url();
            when(mockedUrl.resourceType(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.type(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.secure(true)).thenReturn(mockedUrl);
            when(mockedUrl.signed(true)).thenReturn(mockedUrl);
            when(mockedUrl.format(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.generate(anyString())).thenReturn(null);
            ReflectionTestUtils.setField(attachFileService, "cloudinary", spyCloudinary);

            String url = attachFileService.getDownloadUrl(134L);

            assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/f134.jpg");
        }

        /**
         * Test Case ID: TC-ATT-017
         * Test Objective: Validate TC_ATT_017_getDownloadUrl_MissingCloudinaryConfig behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-017: getDownloadUrl thất bại khi thiếu Cloudinary config cho private fallback")
        void TC_ATT_017_getDownloadUrl_MissingCloudinaryConfig() {
            AttachFile f = new AttachFile();
            f.setId(135L);
            f.setPublicId("private/f135");
            f.setResourceType("raw");
            f.setType("private");
            f.setExtension("pdf");
            f.setSecureUrl(null);
            when(attachFileRepository.findById(135L)).thenReturn(Optional.of(f));

                Cloudinary noSecretCloudinary = Mockito.spy(new Cloudinary(Map.of(
                    "cloud_name", "testCloud",
                    "api_key", "testKey",
                    "api_secret", ""
                )));
            Url mockedUrl = Mockito.mock(Url.class);
            doReturn(mockedUrl).when(noSecretCloudinary).url();
            when(mockedUrl.resourceType(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.type(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.secure(true)).thenReturn(mockedUrl);
            when(mockedUrl.signed(true)).thenReturn(mockedUrl);
            when(mockedUrl.format(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.generate(anyString())).thenReturn(null);
            ReflectionTestUtils.setField(attachFileService, "cloudinary", noSecretCloudinary);

            assertThatThrownBy(() -> attachFileService.getDownloadUrl(135L))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(CoreErrorCode.DOWNLOAD_FILE_FAILED));

            verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        }

        /**
         * Test Case ID: TC-ATT-018
         * Test Objective: Validate TC_ATT_018_getDownloadUrl_PrivateFallback_GuessedResourceType behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-ATT-018: getDownloadUrl private fallback với guessed resourceType và default delivery upload")
        void TC_ATT_018_getDownloadUrl_PrivateFallback_GuessedResourceType() {
            AttachFile f = new AttachFile();
            f.setId(136L);
            f.setPublicId("private/f136");
            f.setResourceType(null);
            f.setType(null);
            f.setExtension("jpg");
            f.setSecureUrl(null);
            when(attachFileRepository.findById(136L)).thenReturn(Optional.of(f));

            Cloudinary spyCloudinary = Mockito.spy(new Cloudinary(Map.of(
                    "cloud_name", "testCloud",
                    "api_key", "testKey",
                    "api_secret", "testSecret"
            )));
            Url mockedUrl = Mockito.mock(Url.class);
            doReturn(mockedUrl).when(spyCloudinary).url();
            when(mockedUrl.resourceType(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.type(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.secure(true)).thenReturn(mockedUrl);
            when(mockedUrl.signed(true)).thenReturn(mockedUrl);
            when(mockedUrl.format(anyString())).thenReturn(mockedUrl);
            when(mockedUrl.generate(anyString())).thenReturn(null);
            ReflectionTestUtils.setField(attachFileService, "cloudinary", spyCloudinary);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenAnswer(invocation -> {
                        String apiUrl = invocation.getArgument(0);
                        HttpEntity<?> entity = invocation.getArgument(2);
                        String body = String.valueOf(entity.getBody());
                        if (apiUrl.contains("/image/download") && body.contains("type=[upload]")) {
                            return new ResponseEntity<>(Map.of("url", "https://guessed-image-download-url"), HttpStatus.OK);
                        }
                        return new ResponseEntity<>(Map.of(), HttpStatus.OK);
                    });

            String url = attachFileService.getDownloadUrl(136L);

            assertThat(url).isEqualTo("https://guessed-image-download-url");
        }
    }
}

