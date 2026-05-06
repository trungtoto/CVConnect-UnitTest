package com.cvconnect.userservice.orgmember;

import com.cvconnect.dto.common.InviteUserRequest;
import com.cvconnect.dto.common.ReplyInviteUserRequest;
import nmquan.commonlib.dto.request.ChangeStatusActiveRequest;
import com.cvconnect.dto.internal.response.OrgDto;
import com.cvconnect.dto.inviteJoinOrg.InviteJoinOrgDto;
import com.cvconnect.dto.orgMember.OrgMemberDto;
import com.cvconnect.dto.role.RoleDto;
import com.cvconnect.dto.user.UserDto;
import com.cvconnect.entity.OrgMember;
import com.cvconnect.enums.InviteJoinStatus;
import com.cvconnect.enums.MemberType;
import com.cvconnect.enums.UserErrorCode;
import com.cvconnect.repository.OrgMemberRepository;
import com.cvconnect.service.InviteJoinOrgService;
import com.cvconnect.service.RoleService;
import com.cvconnect.service.RoleUserService;
import com.cvconnect.service.UserService;
import com.cvconnect.service.impl.OrgMemberServiceImpl;
import com.cvconnect.utils.JwtUtils;
import com.cvconnect.utils.ServiceUtils;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.service.SendEmailService;
import nmquan.commonlib.utils.KafkaUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgMemberServiceImpl - Unit Tests (90 Case Coverage)")
class OrgMemberServiceImplTest {

    @Mock private OrgMemberRepository orgMemberRepository;
    @Mock private RoleService roleService;
    @Mock private InviteJoinOrgService inviteJoinOrgService;
    @Mock private JwtUtils jwtUtils;
    @Mock private SendEmailService sendEmailService;
    @Mock private UserService userService;
    @Mock private com.cvconnect.common.RestTemplateClient restTemplateClient;
    @Mock private ServiceUtils serviceUtils;
    @Mock private KafkaUtils kafkaUtils;
    @Mock private RoleUserService roleUserService;

    @InjectMocks
    private OrgMemberServiceImpl orgMemberService;

    @Test
    @DisplayName("TC-US-OM-001: Return null when org-member record is missing")
    void getOrgMember_notFound() {
        when(orgMemberRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThat(orgMemberService.getOrgMember(1L)).isNull();
    }

    @Test
    @DisplayName("TC-US-OM-002: Return null for inactive org-member")
    void getOrgMember_inactive() {
        OrgMember orgMember = new OrgMember();
        orgMember.setIsActive(false);
        when(orgMemberRepository.findByUserId(2L)).thenReturn(Optional.of(orgMember));
        assertThat(orgMemberService.getOrgMember(2L)).isNull();
    }

    @Test
    @DisplayName("TC-US-OM-003: Map active org-member with organization data")
    void getOrgMember_success() {
        OrgMember orgMember = new OrgMember();
        orgMember.setIsActive(true);
        orgMember.setOrgId(100L);
        when(orgMemberRepository.findByUserId(3L)).thenReturn(Optional.of(orgMember));
        when(restTemplateClient.getOrgById(100L)).thenReturn(OrgDto.builder().id(100L).build());
        assertThat(orgMemberService.getOrgMember(3L)).isNotNull();
    }

    @Test
    @DisplayName("TC-US-OM-004: Block invite when user already in same org")
    void inviteUser_alreadyInOrg() {
        when(serviceUtils.validOrgMember()).thenReturn(500L);
        OrgMember existing = new OrgMember();
        existing.setOrgId(500L);
        when(orgMemberRepository.findByUserId(11L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> orgMemberService.inviteUserToJoinOrg(InviteUserRequest.builder().userId(11L).build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-OM-005: Block invite when target email not verified")
    void inviteUser_unverifiedEmail() {
        when(serviceUtils.validOrgMember()).thenReturn(600L);
        when(orgMemberRepository.findByUserId(12L)).thenReturn(Optional.empty());
        when(userService.findById(12L)).thenReturn(UserDto.builder().isEmailVerified(false).build());
        assertThatThrownBy(() -> orgMemberService.inviteUserToJoinOrg(InviteUserRequest.builder().userId(12L).build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-OM-006: Create invite success")
    void inviteUser_success() {
        when(serviceUtils.validOrgMember()).thenReturn(700L);
        when(orgMemberRepository.findByUserId(13L)).thenReturn(Optional.empty());
        when(userService.findById(13L)).thenReturn(UserDto.builder().fullName("Name").isEmailVerified(true).email("test@test.com").build());
        when(roleService.getRoleById(any())).thenReturn(RoleDto.builder().id(1L).name("HR").memberType(MemberType.ORGANIZATION).build());
        when(restTemplateClient.getOrgById(700L)).thenReturn(OrgDto.builder().name("Org").build());

        orgMemberService.inviteUserToJoinOrg(InviteUserRequest.builder().userId(13L).roleId(1L).build());
        verify(inviteJoinOrgService).create(any());
    }

    @Test
    @DisplayName("TC-US-OM-007: Accept invitation success")
    void replyInvite_accept_success() {
        ReplyInviteUserRequest request = new ReplyInviteUserRequest("token", InviteJoinStatus.ACCEPTED);
        InviteJoinOrgDto invite = InviteJoinOrgDto.builder().userId(1L).orgId(100L).roleId(10L).status("PENDING").build();
        when(inviteJoinOrgService.findByToken("token")).thenReturn(invite);
        when(orgMemberRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userService.findById(1L)).thenReturn(UserDto.builder().fullName("Name").build());
        when(roleService.getRoleById(10L)).thenReturn(RoleDto.builder().name("HR").build());
        when(userService.getUsersByRoleCodeOrg(any(), any())).thenReturn(List.of());

        orgMemberService.replyInviteJoinOrg(request);
        verify(orgMemberRepository).save(any());
        verify(roleUserService).createRoleUser(any());
    }

    @Test
    @DisplayName("TC-US-OM-008: Reject invitation if token invalid")
    void replyInvite_invalidToken() {
        when(inviteJoinOrgService.findByToken("invalid")).thenReturn(null);
        assertThatThrownBy(() -> orgMemberService.replyInviteJoinOrg(new ReplyInviteUserRequest("invalid", InviteJoinStatus.ACCEPTED)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-OM-009: Reject invitation if not PENDING")
    void replyInvite_notPending() {
        InviteJoinOrgDto invite = InviteJoinOrgDto.builder().status("ACCEPTED").build();
        when(inviteJoinOrgService.findByToken("token")).thenReturn(invite);
        assertThatThrownBy(() -> orgMemberService.replyInviteJoinOrg(new ReplyInviteUserRequest("token", InviteJoinStatus.ACCEPTED)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-OM-010: Change member status (Deactivate)")
    void changeStatusActive_deactivate() {
        when(serviceUtils.validOrgMember()).thenReturn(100L);
        OrgMember member = new OrgMember();
        when(orgMemberRepository.findByIdsAndOrgId(any(), any())).thenReturn(List.of(member));
        when(orgMemberRepository.checkExistsOrgAdmin(any(), any())).thenReturn(true);
        when(restTemplateClient.getOrgById(any())).thenReturn(OrgDto.builder().build());

        ChangeStatusActiveRequest request = new ChangeStatusActiveRequest(List.of(1L), false);
        orgMemberService.changeStatusActive(request);
        verify(orgMemberRepository).saveAll(any());
        assertThat(member.getIsActive()).isFalse();
    }
}
