package com.cvconnect.service.impl;

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.*;
import com.cvconnect.entity.EmailTemplate;
import com.cvconnect.repository.EmailTemplateRepository;
import com.cvconnect.service.EmailTemplatePlaceholderService;
import com.cvconnect.service.PlaceholderService;
import nmquan.commonlib.dto.response.IDResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test for EmailTemplateServiceImpl
 * Coverage target: > 50%
 */
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailTemplateServiceImplTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;
    @Mock
    private EmailTemplatePlaceholderService emailTemplatePlaceholderService;
    @Mock
    private PlaceholderService placeholderService;
    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private EmailTemplateServiceImpl emailTemplateService;

    private EmailTemplateRequest validRequest;
    private Long mockOrgId = 1L;

    @BeforeEach
    void setUp() {
        validRequest = new EmailTemplateRequest();
        validRequest.setId(1L);
        validRequest.setCode("WELCOME");
        validRequest.setName("Welcome Email");
        validRequest.setSubject("Welcome to CVConnect");
        validRequest.setBody("Hello ${name}");
        validRequest.setPlaceholderIds(List.of(101L));
    }

    // --- CREATE TESTS ---

    @Test
    @DisplayName("TC-NTF-ET-001: Create Success with all fields")
    void create_Success() {
        // [Given] Setup mock behavior
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(false);
        when(placeholderService.getByIds(anyList())).thenReturn(List.of(new PlaceholderDto()));

        // [When] Execute business logic
        IDResponse<Long> response = emailTemplateService.create(validRequest);

        // [Then] Verify results and CheckDB interaction
        assertThat(response).isNotNull();
        verify(emailTemplateRepository, times(1)).save(any(EmailTemplate.class));
        verify(emailTemplatePlaceholderService, times(1)).create(anyList());
    }

    @Test
    @DisplayName("TC-NTF-ET-002: Create Fail due to duplicate code")
    void create_Fail_DuplicateCode() {
        // [Given] Mock existing code
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(true);

        // [When & Then] Verify exception and Rollback assurance (implicit in Unit Test)
        assertThatThrownBy(() -> emailTemplateService.create(validRequest))
                .isInstanceOf(AppException.class);
        verify(emailTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-NTF-ET-004: Create Fail due to placeholder not found")
    void create_Fail_PlaceholderNotFound() {
        // [Given]
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(false);
        when(placeholderService.getByIds(anyList())).thenReturn(Collections.emptyList()); // Empty but requested 1

        // [When & Then]
        assertThatThrownBy(() -> emailTemplateService.create(validRequest))
                .isInstanceOf(AppException.class);
    }

    // --- UPDATE TESTS ---

    @Test
    @DisplayName("TC-NTF-ET-005: Update Success - Modify fields")
    void update_Success() {
        // [Given]
        EmailTemplate existing = new EmailTemplate();
        existing.setId(1L);
        existing.setOrgId(mockOrgId);
        existing.setCode("OLD_CODE");

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(false);
        when(placeholderService.getByEmailTemplateId(anyLong())).thenReturn(Collections.emptyList());
        when(placeholderService.getByIds(anyList())).thenReturn(List.of(new PlaceholderDto()));

        // [When]
        IDResponse<Long> response = emailTemplateService.update(validRequest);

        // [Then] Verify DB access
        assertThat(response.getId()).isEqualTo(1L);
        verify(emailTemplateRepository).save(existing);
    }

    @Test
    @DisplayName("TC-NTF-ET-007: Update Fail - OrgId mismatch")
    void update_Fail_OrgMismatch() {
        // [Given] Template belongs to Org 2, but User belongs to Org 1
        EmailTemplate otherOrgTemplate = new EmailTemplate();
        otherOrgTemplate.setOrgId(2L);

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(otherOrgTemplate));

        // [When & Then]
        assertThatThrownBy(() -> emailTemplateService.update(validRequest))
                .isInstanceOf(AppException.class);
    }

    // --- DETAIL & DELETE TESTS ---

    @Test
    @DisplayName("TC-NTF-ET-013: Detail Success")
    void detail_Success() {
        // [Given]
        EmailTemplate entity = new EmailTemplate();
        entity.setId(1L);
        entity.setName("Template A");

        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        when(placeholderService.getByEmailTemplateId(anyLong())).thenReturn(List.of(new PlaceholderDto()));

        // [When]
        EmailTemplateDto dto = emailTemplateService.detail(1L);

        // [Then]
        assertThat(dto.getName()).isEqualTo("Template A");
        assertThat(dto.getPlaceholders()).hasSize(1);
    }

    @Test
    @DisplayName("TC-NTF-ET-003: Create Success with empty placeholders")
    void create_Success_EmptyPlaceholders() {
        validRequest.setPlaceholderIds(null);
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(false);

        IDResponse<Long> response = emailTemplateService.create(validRequest);

        assertThat(response).isNotNull();
        verify(emailTemplatePlaceholderService, never()).create(anyList());
    }

    @Test
    @DisplayName("TC-NTF-ET-008: Update Fail - Duplicate code with other template")
    void update_Fail_DuplicateCode() {
        EmailTemplate existing = new EmailTemplate();
        existing.setId(1L);
        existing.setOrgId(mockOrgId);
        existing.setCode("OLD_CODE");

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(emailTemplateRepository.existsByCodeAndOrgId(eq("WELCOME"), eq(mockOrgId))).thenReturn(true);

        assertThatThrownBy(() -> emailTemplateService.update(validRequest))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-NTF-ET-011: Update Sync Placeholders - Remove old ones")
    void update_Sync_RemoveOld() {
        EmailTemplate existing = new EmailTemplate();
        existing.setId(1L);
        existing.setOrgId(mockOrgId);
        existing.setCode("WELCOME");

        validRequest.setPlaceholderIds(Collections.emptyList()); // Request no placeholders
        PlaceholderDto oldP = new PlaceholderDto();
        oldP.setId(101L);

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(placeholderService.getByEmailTemplateId(anyLong())).thenReturn(List.of(oldP));

        emailTemplateService.update(validRequest);

        verify(emailTemplatePlaceholderService).deleteByEmailTemplateIdAndPlaceholderIds(eq(1L), anyList());
    }

    @Test
    @DisplayName("TC-NTF-ET-016: Delete Fail - Count mismatch")
    void delete_Fail_CountMismatch() {
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findByIdsAndOrgId(anyList(), anyLong())).thenReturn(List.of(new EmailTemplate())); // Found 1, requested 2

        assertThatThrownBy(() -> emailTemplateService.delete(List.of(1L, 2L)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-NTF-ET-020: ChangeStatus Success")
    void changeStatusActive_Success() {
        ChangeStatusActiveRequest request = new ChangeStatusActiveRequest();
        request.setIds(List.of(1L));
        request.setActive(true);

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.findByIdsAndOrgId(anyList(), anyLong())).thenReturn(List.of(new EmailTemplate()));

        emailTemplateService.changeStatusActive(request);

        verify(emailTemplateRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-NTF-ET-023: Preview Success")
    void previewEmail_Success() {
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        // Mock getById
        EmailTemplate entity = new EmailTemplate();
        entity.setOrgId(mockOrgId);
        entity.setBody("Hello");
        when(emailTemplateRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        when(placeholderService.getByEmailTemplateId(anyLong())).thenReturn(Collections.emptyList());
        
        when(restTemplateClient.previewEmail(anyString(), anyList(), any(), anyBoolean())).thenReturn("Preview Body");

        EmailTemplateDto result = emailTemplateService.previewEmail(1L, new com.cvconnect.dto.internal.request.DataReplacePlaceholder());

        assertThat(result.getBodyPreview()).isEqualTo("Preview Body");
    }

    @Test
    @DisplayName("TC-NTF-ET-026: Preview Without Template Success")
    void previewEmailWithoutTemplate_Success() {
        PreviewEmailWithoutTemplate request = new PreviewEmailWithoutTemplate();
        request.setBody("Body");
        request.setPlaceholderCodes(List.of("CODE"));
        
        when(restTemplateClient.previewEmail(anyString(), anyList(), any(), anyBoolean())).thenReturn("Preview");

        EmailTemplateDto result = emailTemplateService.previewEmail(request);

        assertThat(result.getBodyPreview()).isEqualTo("Preview");
    }

    @ParameterizedTest
    @DisplayName("TC-NTF-ET-031-040: Create Validation with various codes")
    @ValueSource(strings = {"WELCOME", "OFFER", "REJECT", "INTERVIEW", "REMIND", "PROMOTION", "SURVEY", "NEWS", "ALERTS", "SYSTEM"})
    void create_Validation_Codes(String code) {
        validRequest.setCode(code);
        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.existsByCodeAndOrgId(anyString(), anyLong())).thenReturn(false);
        when(placeholderService.getByIds(anyList())).thenReturn(List.of(new PlaceholderDto()));

        IDResponse<Long> response = emailTemplateService.create(validRequest);
        assertThat(response).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("TC-NTF-ET-041-050: Filter with various date ranges")
    @CsvSource({
        "2024-01-01T00:00:00Z, 2024-01-02T00:00:00Z",
        "2024-02-01T00:00:00Z, 2024-02-02T00:00:00Z",
        "2024-03-01T00:00:00Z, ",
        ", 2024-04-01T00:00:00Z",
        "2024-05-01T00:00:00Z, 2024-05-31T23:59:59Z",
        "2024-06-01T10:00:00Z, 2024-06-01T11:00:00Z",
        "2023-12-31T23:59:59Z, 2024-01-01T00:00:00Z",
        "2024-07-01T00:00:00Z, 2024-07-01T23:59:59Z",
        "2024-08-01T00:00:00Z, 2024-08-02T00:00:00Z",
        "2024-09-01T00:00:00Z, 2024-09-15T00:00:00Z"
    })
    void filter_DateRanges(String start, String end) {
        EmailTemplateFilterRequest request = new EmailTemplateFilterRequest();
        if(start != null) request.setCreatedAtStart(java.time.Instant.parse(start));
        if(end != null) request.setCreatedAtEnd(java.time.Instant.parse(end));

        when(restTemplateClient.validOrgMember()).thenReturn(mockOrgId);
        when(emailTemplateRepository.filter(any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        emailTemplateService.filter(request);
        verify(emailTemplateRepository).filter(any(), any());
    }
}
