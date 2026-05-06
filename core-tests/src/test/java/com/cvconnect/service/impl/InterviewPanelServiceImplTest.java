package com.cvconnect.service.impl;

/**
 * ============================================================
 * FILE: InterviewPanelServiceImplTest.java
 * MODULE: core-service
 * PURPOSE: Unit test cho hệ thống Phân Quyền/Người Phỏng Vấn (InterviewPanel)
 *
 * BAO PHỦ CÁC LUỒNG CẤP 2 (Branch Coverage):
 *   - getByCalendarId: mảng rỗng (UserIds.isEmpty) vs get được Object Users List.
 *   - create: Chuyển đổi và lưu repository.
 *   - existByJobAdIdAndUserId: Lấy boolean return.
 * ============================================================
 */

import com.cvconnect.common.RestTemplateClient;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.interviewPanel.InterviewPanelDto;
import com.cvconnect.entity.InterviewPanel;
import com.cvconnect.repository.InterviewPanelRepository;
import com.cvconnect.service.impl.InterviewPanelServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPanelServiceImpl - Unit Tests (C2 Branch Coverage)")
public class InterviewPanelServiceImplTest {

    @Mock
    private InterviewPanelRepository interviewPanelRepository;

    @Mock
    private RestTemplateClient restTemplateClient;

    @InjectMocks
    private InterviewPanelServiceImpl interviewPanelService;

    @Nested
    @DisplayName("1. create() Test")
    class CreateTest {
        /**
         * Test Case ID: TC-PANEL-001
         * Test Objective: Validate TC_PANEL_001_create behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-PANEL-001: Lưu danh sách người tham gia phỏng vấn")
        void TC_PANEL_001_create() {
            InterviewPanelDto dto = new InterviewPanelDto();
            dto.setInterviewerId(5L);

            interviewPanelService.create(List.of(dto));

            verify(interviewPanelRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("2. getByCalendarId() Branch Coverage")
    class GetByCalendarIdTest {
        /**
         * Test Case ID: TC-PANEL-002
         * Test Objective: Validate TC_PANEL_002_getEmpty behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-PANEL-002: Trả về danh sách rỗng nếu không tìm thấy người PV")
        void TC_PANEL_002_getEmpty() {
            when(interviewPanelRepository.findByCalendarId(10L)).thenReturn(Collections.emptyList());

            List<UserDto> result = interviewPanelService.getByCalendarId(10L);

            assertThat(result).isEmpty();
        }

        /**
         * Test Case ID: TC-PANEL-003
         * Test Objective: Validate TC_PANEL_003_getUsersSuccess behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-PANEL-003: Lấy danh sách thông tin Users thành công")
        void TC_PANEL_003_getUsersSuccess() {
            InterviewPanel p1 = new InterviewPanel();
            p1.setInterviewerId(100L);
            when(interviewPanelRepository.findByCalendarId(20L)).thenReturn(List.of(p1));

            UserDto mockUser = new UserDto();
            mockUser.setId(100L);
            when(restTemplateClient.getUsersByIds(List.of(100L))).thenReturn(Map.of(100L, mockUser));

            List<UserDto> result = interviewPanelService.getByCalendarId(20L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("3. existByJobAdIdAndUserId() Test")
    class ExistTest {
        /**
         * Test Case ID: TC-PANEL-004
         * Test Objective: Validate TC_PANEL_004_existMethod behavior.
         * Input: Dữ liệu mock theo ngữ cảnh test case.
         * Expected Output: Kết quả đúng theo assert/verify của test case.
         * Notes: Auto-generated comment theo chuẩn RestTemplateClientTest.
         */
        @Test
        @DisplayName("TC-PANEL-004: Validate boolean tồn tại Panel")
        void TC_PANEL_004_existMethod() {
            when(interviewPanelRepository.existsByJobAdIdAndInterviewerId(50L, 90L)).thenReturn(true);

            Boolean exist = interviewPanelService.existByJobAdIdAndUserId(50L, 90L);

            assertThat(exist).isTrue();
        }
    }
}


