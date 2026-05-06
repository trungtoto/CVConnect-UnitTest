package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: CalendarCandidateInfoServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Map lịch và Ứng Viên
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - create: map data và lưu
 *   - getByCalendarIdAndCandidateInfoId: Rỗng return null, tồn tại return Object mapping
 * ============================================================
 */

import com.cvconnect.dto.calendar.CalendarCandidateInfoDto;
import com.cvconnect.entity.CalendarCandidateInfo;
import com.cvconnect.repository.CalendarCandidateInfoRepository;
import com.cvconnect.service.impl.CalendarCandidateInfoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarCandidateInfoServiceImpl - Unit Tests (C2 Branch Coverage)")
public class CalendarCandidateInfoServiceImplTest {

    @Mock
    private CalendarCandidateInfoRepository repository;

    @InjectMocks
    private CalendarCandidateInfoServiceImpl service;

    /**
     * Test Case ID: TC-CCI-001
     * Test Objective: Validate TC_CCI_001_create behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-CCI-001: Hàm Create map dto to entity")
    void TC_CCI_001_create() {
        CalendarCandidateInfoDto dto = new CalendarCandidateInfoDto();
        dto.setCalendarId(5L);
        service.create(List.of(dto));
        verify(repository).saveAll(any());
    }

    /**
     * Test Case ID: TC-CCI-002
     * Test Objective: Validate TC_CCI_002_getNull behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-CCI-002: Trả về null khi Get không có dữ liệu")
    void TC_CCI_002_getNull() {
        when(repository.findByCalendarIdAndCandidateInfoId(1L, 2L)).thenReturn(null);
        assertThat(service.getByCalendarIdAndCandidateInfoId(1L, 2L)).isNull();
    }

    /**
     * Test Case ID: TC-CCI-003
     * Test Objective: Validate TC_CCI_003_getSuccess behavior.
     * Input: Dữ liệu mock theo ngữ cảnh test case.
     * Expected Output: Kết quả đúng theo assert/verify của test case.
     * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
     */
    @Test
    @DisplayName("TC-CCI-003: Trả về Object map thành công")
    void TC_CCI_003_getSuccess() {
        CalendarCandidateInfo entity = new CalendarCandidateInfo();
        entity.setCalendarId(1L);
        entity.setCandidateInfoId(2L);
        when(repository.findByCalendarIdAndCandidateInfoId(1L, 2L)).thenReturn(entity);
        
        CalendarCandidateInfoDto dto = service.getByCalendarIdAndCandidateInfoId(1L, 2L);
        assertThat(dto).isNotNull();
        assertThat(dto.getCalendarId()).isEqualTo(1L);
    }
}


