package com.cvconnect.common;

/**
 * ============================================================
 * FILE: RestTemplateClientTest.java
 * MODULE: core-service
 * MỤC ĐÍCH: Unit test cho {@link RestTemplateClient}
 *
 * BAO PHỦ CÁC LUỒNG CHÍNH:
 *   - Mapping endpoint nội bộ user/notify service
 *   - Branch dữ liệu null và dữ liệu hợp lệ
 *   - Delegation sang RestTemplateService
 * ============================================================
 */

import com.cvconnect.dto.dashboard.admin.DashboardFilter;
import com.cvconnect.dto.failedRollback.FailedRollbackUpdateAccountStatus;
import com.cvconnect.dto.internal.response.ConversationDto;
import com.cvconnect.dto.internal.response.EmailConfigDto;
import com.cvconnect.dto.internal.response.EmailTemplateDto;
import com.cvconnect.dto.internal.response.UserDto;
import com.cvconnect.dto.jobAdCandidate.MyConversationWithFilter;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import nmquan.commonlib.dto.response.FilterResponse;
import nmquan.commonlib.dto.response.Response;
import nmquan.commonlib.service.RestTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestTemplateClient - Unit Tests")
public class RestTemplateClientTest {

    @Mock
    private RestTemplateService restTemplateService;

    @InjectMocks
    private RestTemplateClient restTemplateClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(restTemplateClient, "SERVER_USER_SERVICE", "http://user");
        ReflectionTestUtils.setField(restTemplateClient, "SERVER_NOTIFY_SERVICE", "http://notify");
    }

    /**
     * Test Case ID: TC-RTC-001
     * Test Objective: Xử lý guard clause khi userId null.
     * Input: getUser(null).
     * Expected Output: Trả về null, không gọi RestTemplateService.
     * Notes: Branch null input.
     */
    @Test
    @DisplayName("getUser: return null when userId is null")
    void getUser_nullUserId_shouldReturnNull() {
        UserDto result = restTemplateClient.getUser(null);

        assertThat(result).isNull();
        verifyNoInteractions(restTemplateService);
    }

    /**
     * Test Case ID: TC-RTC-002
     * Test Objective: Kiểm tra gọi đúng endpoint lấy user theo id.
     * Input: userId=1L, mock GET trả UserDto.
     * Expected Output: Trả đúng object data từ response.
     * Notes: Delegation sang RestTemplateService.
     */
    @Test
    @DisplayName("getUser: call GET endpoint and return data")
    void getUser_shouldReturnData() {
        UserDto data = new UserDto();
        when(restTemplateService.getMethodRestTemplate(
                eq("http://user/user/internal/get-by-id/{id}"),
                any(ParameterizedTypeReference.class),
                eq(1L)
        )).thenReturn(Response.<UserDto>builder().data(data).build());

        UserDto result = restTemplateClient.getUser(1L);

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-003
     * Test Objective: Lấy email template theo id.
     * Input: templateId=5L.
     * Expected Output: Trả đúng EmailTemplateDto từ response.
     * Notes: Verify mapping URL notify service.
     */
    @Test
    @DisplayName("getEmailTemplateById: call GET endpoint and return data")
    void getEmailTemplateById_shouldReturnData() {
        EmailTemplateDto data = new EmailTemplateDto();
        when(restTemplateService.getMethodRestTemplate(
                eq("http://notify/email-template/internal/get-by-id/{id}"),
                any(ParameterizedTypeReference.class),
                eq(5L)
        )).thenReturn(Response.<EmailTemplateDto>builder().data(data).build());

        EmailTemplateDto result = restTemplateClient.getEmailTemplateById(5L);

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-004
     * Test Objective: Kiểm tra checkOrgUserRole gọi endpoint nội bộ đúng tham số.
     * Input: userId, roleCode, orgId hợp lệ.
     * Expected Output: Trả về true theo mock response.
     * Notes: Bao phủ endpoint có nhiều path params.
     */
    @Test
    @DisplayName("checkOrgUserRole: call GET endpoint and return data")
    void checkOrgUserRole_shouldReturnData() {
        when(restTemplateService.getMethodRestTemplate(
                eq("http://user/user/internal/check-org-user-role/{userId}/{roleCode}/{orgId}"),
                any(ParameterizedTypeReference.class),
                eq(1L),
                eq("HR"),
                eq(10L)
        )).thenReturn(Response.<Boolean>builder().data(true).build());

        Boolean result = restTemplateClient.checkOrgUserRole(1L, "HR", 10L);

        assertThat(result).isTrue();
    }

    /**
     * Test Case ID: TC-RTC-005
     * Test Objective: Lấy danh sách email template theo org.
     * Input: orgId=2L.
     * Expected Output: Trả về list EmailTemplateDto.
     * Notes: Kiểm tra endpoint notify by org.
     */
    @Test
    @DisplayName("getEmailTemplateByOrgId: call GET endpoint and return data")
    void getEmailTemplateByOrgId_shouldReturnData() {
        List<EmailTemplateDto> data = List.of(new EmailTemplateDto());
        when(restTemplateService.getMethodRestTemplate(
                eq("http://notify/email-template/internal/get-by-org-id/{orgId}"),
                any(ParameterizedTypeReference.class),
                eq(2L)
        )).thenReturn(Response.<List<EmailTemplateDto>>builder().data(data).build());

        List<EmailTemplateDto> result = restTemplateClient.getEmailTemplateByOrgId(2L);

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-006
     * Test Objective: Lấy email config theo org hiện tại.
     * Input: Không có tham số.
     * Expected Output: Trả về EmailConfigDto.
     * Notes: Endpoint context-based.
     */
    @Test
    @DisplayName("getEmailConfigByOrg: call GET endpoint and return data")
    void getEmailConfigByOrg_shouldReturnData() {
        EmailConfigDto data = new EmailConfigDto();
        when(restTemplateService.getMethodRestTemplate(
                eq("http://notify/email-config/internal/get-by-org"),
                any(ParameterizedTypeReference.class)
        )).thenReturn(Response.<EmailConfigDto>builder().data(data).build());

        EmailConfigDto result = restTemplateClient.getEmailConfigByOrg();

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-007
     * Test Objective: Xác thực org member hiện tại.
     * Input: Không có tham số.
     * Expected Output: Trả orgId hợp lệ.
     * Notes: Dùng trong nhiều flow service.
     */
    @Test
    @DisplayName("validOrgMember: call GET endpoint and return orgId")
    void validOrgMember_shouldReturnData() {
        when(restTemplateService.getMethodRestTemplate(
                eq("http://user/org-member/internal/valid-org-member"),
                any(ParameterizedTypeReference.class)
        )).thenReturn(Response.<Long>builder().data(100L).build());

        Long result = restTemplateClient.validOrgMember();

        assertThat(result).isEqualTo(100L);
    }

    /**
     * Test Case ID: TC-RTC-008
     * Test Objective: Lấy map user theo danh sách ids.
     * Input: List userIds.
     * Expected Output: Trả map id -> UserDto.
     * Notes: POST body mapping.
     */
    @Test
    @DisplayName("getUsersByIds: call POST endpoint and return map")
    void getUsersByIds_shouldReturnData() {
        List<Long> userIds = List.of(1L, 2L);
        Map<Long, UserDto> data = Map.of(1L, new UserDto());
        when(restTemplateService.postMethodRestTemplate(
                eq("http://user/user/internal/get-by-ids"),
                any(ParameterizedTypeReference.class),
                eq(userIds)
        )).thenReturn(Response.<Map<Long, UserDto>>builder().data(data).build());

        Map<Long, UserDto> result = restTemplateClient.getUsersByIds(userIds);

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-009
     * Test Objective: Kiểm tra danh sách user có thuộc org hay không.
     * Input: List userIds.
     * Expected Output: Trả boolean theo response.
     * Notes: Endpoint authz nội bộ.
     */
    @Test
    @DisplayName("checkOrgMember: call POST endpoint and return data")
    void checkOrgMember_shouldReturnData() {
        List<Long> userIds = List.of(1L, 2L);
        when(restTemplateService.postMethodRestTemplate(
                eq("http://user/org-member/internal/check-org-member"),
                any(ParameterizedTypeReference.class),
                eq(userIds)
        )).thenReturn(Response.<Boolean>builder().data(true).build());

        Boolean result = restTemplateClient.checkOrgMember(userIds);

        assertThat(result).isTrue();
    }

    /**
     * Test Case ID: TC-RTC-010
     * Test Objective: Lấy conversation chưa đọc.
     * Input: Không có tham số.
     * Expected Output: Trả list ConversationDto.
     * Notes: Endpoint notify conversation-unread.
     */
    @Test
    @DisplayName("getConversationUnread: call GET endpoint and return data")
    void getConversationUnread_shouldReturnData() {
        List<ConversationDto> data = List.of(new ConversationDto());
        when(restTemplateService.getMethodRestTemplate(
                eq("http://notify/conversation/internal/conversation-unread"),
                any(ParameterizedTypeReference.class)
        )).thenReturn(Response.<List<ConversationDto>>builder().data(data).build());

        List<ConversationDto> result = restTemplateClient.getConversationUnread();

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-011
     * Test Objective: Lấy danh sách hội thoại của user hiện tại.
     * Input: Không có tham số.
     * Expected Output: Trả list ConversationDto.
     * Notes: Delegation GET my-conversations.
     */
    @Test
    @DisplayName("getMyConversations: call GET endpoint and return data")
    void getMyConversations_shouldReturnData() {
        List<ConversationDto> data = List.of(new ConversationDto());
        when(restTemplateService.getMethodRestTemplate(
                eq("http://notify/conversation/internal/my-conversations"),
                any(ParameterizedTypeReference.class)
        )).thenReturn(Response.<List<ConversationDto>>builder().data(data).build());

        List<ConversationDto> result = restTemplateClient.getMyConversations();

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-012
     * Test Objective: Lấy hội thoại có filter.
     * Input: MyConversationWithFilter request.
     * Expected Output: Trả FilterResponse<ConversationDto>.
     * Notes: Kiểm tra endpoint POST filtered.
     */
    @Test
    @DisplayName("getMyConversationsWithFilter: call POST endpoint and return data")
    void getMyConversationsWithFilter_shouldReturnData() {
        MyConversationWithFilter filter = new MyConversationWithFilter();
        FilterResponse<ConversationDto> data = FilterResponse.<ConversationDto>builder()
                .data(List.of(new ConversationDto()))
                .build();
        when(restTemplateService.postMethodRestTemplate(
                eq("http://notify/conversation/internal/my-conversations-filtered"),
                any(ParameterizedTypeReference.class),
                eq(filter)
        )).thenReturn(Response.<FilterResponse<ConversationDto>>builder().data(data).build());

        FilterResponse<ConversationDto> result = restTemplateClient.getMyConversationsWithFilter(filter);

        assertThat(result).isSameAs(data);
    }

    /**
     * Test Case ID: TC-RTC-013
     * Test Objective: Cập nhật trạng thái account theo orgIds.
     * Input: ChangeStatusActiveRequest.
     * Expected Output: Gọi đúng endpoint POST với payload tương ứng.
     * Notes: Không kiểm tra response body.
     */
    @Test
    @DisplayName("updateAccountStatusByOrgIds: call POST endpoint")
    void updateAccountStatusByOrgIds_shouldCallPostEndpoint() {
        ChangeStatusActiveRequest request = ChangeStatusActiveRequest.builder()
                .ids(List.of(1L, 2L))
                .active(true)
                .build();

        restTemplateClient.updateAccountStatusByOrgIds(request);

        verify(restTemplateService).postMethodRestTemplate(
                eq("http://user/org-member/internal/update-account-status-by-org-ids"),
                any(ParameterizedTypeReference.class),
                eq(request)
        );
    }

    /**
     * Test Case ID: TC-RTC-014
     * Test Objective: Rollback cập nhật trạng thái account theo orgIds.
     * Input: FailedRollbackUpdateAccountStatus.
     * Expected Output: Gọi endpoint rollback đúng URL/payload.
     * Notes: Dùng cho flow failed rollback.
     */
    @Test
    @DisplayName("rollbackUpdateAccountStatusByOrgIds: call POST endpoint")
    void rollbackUpdateAccountStatusByOrgIds_shouldCallPostEndpoint() {
        FailedRollbackUpdateAccountStatus request = FailedRollbackUpdateAccountStatus.builder()
                .orgIds(List.of(1L))
                .active(false)
                .build();

        restTemplateClient.rollbackUpdateAccountStatusByOrgIds(request);

        verify(restTemplateService).postMethodRestTemplate(
                eq("http://user/org-member/internal/rollback-update-account-status-by-org-ids"),
                any(ParameterizedTypeReference.class),
                eq(request)
        );
    }

    /**
     * Test Case ID: TC-RTC-015
     * Test Objective: Lấy số lượng ứng viên mới.
     * Input: DashboardFilter.
     * Expected Output: Trả về số lượng Long từ response.
     * Notes: Endpoint thống kê nội bộ.
     */
    @Test
    @DisplayName("numberOfNewCandidate: call POST endpoint and return data")
    void numberOfNewCandidate_shouldReturnData() {
        DashboardFilter filter = new DashboardFilter();
        when(restTemplateService.postMethodRestTemplate(
                eq("http://user/candidate/internal/number-of-new-candidate"),
                any(ParameterizedTypeReference.class),
                eq(filter)
        )).thenReturn(Response.<Long>builder().data(8L).build());

        Long result = restTemplateClient.numberOfNewCandidate(filter);

        assertThat(result).isEqualTo(8L);
    }

    /**
     * Test Case ID: TC-RTC-016
     * Test Objective: Lấy danh sách user theo roleCode và orgId.
     * Input: roleCode, orgId.
     * Expected Output: Trả về list UserDto theo response.
     * Notes: Endpoint GET có path params.
     */
    @Test
    @DisplayName("getUserByRoleCodeOrg: call GET endpoint and return data")
    void getUserByRoleCodeOrg_shouldReturnData() {
        List<UserDto> data = List.of(new UserDto());
        when(restTemplateService.getMethodRestTemplate(
                eq("http://user/user/internal/get-by-role-code-org-id/{roleCode}/{orgId}"),
                any(ParameterizedTypeReference.class),
                eq("HR"),
                eq(99L)
        )).thenReturn(Response.<List<UserDto>>builder().data(data).build());

        List<UserDto> result = restTemplateClient.getUserByRoleCodeOrg("HR", 99L);

        assertThat(result).isSameAs(data);
    }
}
