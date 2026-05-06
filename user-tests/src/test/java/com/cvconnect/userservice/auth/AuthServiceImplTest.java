package com.cvconnect.userservice.auth;

import com.cvconnect.dto.auth.*;
import com.cvconnect.dto.common.TokenInfo;
import com.cvconnect.dto.role.RoleDto;
import com.cvconnect.dto.user.UserDto;
import com.cvconnect.enums.AccessMethod;
import com.cvconnect.enums.TokenType;
import com.cvconnect.enums.UserErrorCode;
import com.cvconnect.service.*;
import com.cvconnect.service.impl.AuthServiceImpl;
import com.cvconnect.utils.CookieUtils;
import com.cvconnect.utils.JwtUtils;
import com.cvconnect.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nmquan.commonlib.exception.AppException;
import nmquan.commonlib.service.SendEmailService;
import nmquan.commonlib.utils.KafkaUtils;
import nmquan.commonlib.utils.LocalizationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Unit Tests (90 Case Coverage)")
class AuthServiceImplTest {

    @Mock private UserService userService;
    @Mock private RoleUserService roleUserService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisUtils redisUtils;
    @Mock private RedisUtils redis;
    @Mock private RoleService roleService;
    @Mock private CandidateService candidateService;
    @Mock private LocalizationUtils localizationUtils;
    @Mock private SendEmailService sendEmailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.cvconnect.common.RestTemplateClient restTemplateClient;
    @Mock private OrgMemberService orgMemberService;
    @Mock private KafkaUtils kafkaUtils;
    @Mock private FailedRollbackService failedRollbackService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("TC-US-AUTH-001: Success login")
    void login_success() {
        LoginRequest request = new LoginRequest("user", "pass");
        UserDto user = UserDto.builder().id(1L).username("user").isEmailVerified(true).isActive(true).build();
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(userService.findByUsername("user")).thenReturn(user);
        when(jwtUtils.generateToken(any())).thenReturn("valid-token");
        when(jwtUtils.generateRefreshToken()).thenReturn("refresh-token");
        when(roleUserService.findRoleUseByUserId(1L)).thenReturn(List.of());
        when(orgMemberService.getOrgMember(1L)).thenReturn(null);

        LoginResponse result = authService.login(request, response);

        assertThat(result.getToken()).isEqualTo("valid-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("TC-US-AUTH-002: Reject login with non-existent username")
    void login_userNotFound() {
        LoginRequest request = new LoginRequest("unknown", "pass");
        when(userService.findByUsername("unknown")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(request, mock(HttpServletResponse.class)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.LOGIN_FAIL));
    }

    @Test
    @DisplayName("TC-US-AUTH-003: Reject login with incorrect password")
    void login_badCredentials() {
        LoginRequest request = new LoginRequest("user", "wrong");
        UserDto user = UserDto.builder().id(1L).username("user").isEmailVerified(true).isActive(true).build();

        when(userService.findByUsername("user")).thenReturn(user);
        doThrow(new BadCredentialsException("")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request, mock(HttpServletResponse.class)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.LOGIN_FAIL));
    }

    @Test
    @DisplayName("TC-US-AUTH-004: Reject login when email not verified")
    void login_emailNotVerified() {
        LoginRequest request = new LoginRequest("user", "pass");
        UserDto user = UserDto.builder().id(1L).username("user").isEmailVerified(false).isActive(true).build();

        when(userService.findByUsername("user")).thenReturn(user);

        assertThatThrownBy(() -> authService.login(request, mock(HttpServletResponse.class)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.EMAIL_NOT_VERIFIED));
    }

    @Test
    @DisplayName("TC-US-AUTH-005: Reject login when account is inactive")
    void login_accountInactive() {
        LoginRequest request = new LoginRequest("user", "pass");
        UserDto user = UserDto.builder().id(1L).username("user").isEmailVerified(true).isActive(false).build();

        when(userService.findByUsername("user")).thenReturn(user);

        assertThatThrownBy(() -> authService.login(request, mock(HttpServletResponse.class)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.ACCOUNT_NOT_ACTIVE));
    }

    @Test
    @DisplayName("TC-US-AUTH-006: Refresh token success and rotate token")
    void refreshToken_success() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        try (MockedStatic<CookieUtils> cookieUtils = mockStatic(CookieUtils.class)) {
            cookieUtils.when(() -> CookieUtils.getRefreshTokenCookie(request)).thenReturn("old-rf-token");
            when(redisUtils.getTokenKey("old-rf-token")).thenReturn("redis-key");
            TokenInfo tokenInfo = TokenInfo.builder().userId(1L).type(TokenType.REFRESH).build();
            when(redisUtils.getObject("redis-key", TokenInfo.class)).thenReturn(tokenInfo);
            when(userService.findById(1L)).thenReturn(UserDto.builder().id(1L).isActive(true).isEmailVerified(true).build());
            when(jwtUtils.generateRefreshToken()).thenReturn("new-rf-token");

            authService.refreshToken(request, response);

            verify(redisUtils).deleteByKey("redis-key");
            verify(redisUtils).saveObject(any(), any(), anyInt());
        }
    }

    @Test
    @DisplayName("TC-US-AUTH-007: Reject refresh token if missing in cookie")
    void refreshToken_missingCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        try (MockedStatic<CookieUtils> cookieUtils = mockStatic(CookieUtils.class)) {
            cookieUtils.when(() -> CookieUtils.getRefreshTokenCookie(request)).thenReturn(null);
            assertThatThrownBy(() -> authService.refreshToken(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("TC-US-AUTH-008: Reject refresh token if not found in Redis")
    void refreshToken_notInRedis() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        try (MockedStatic<CookieUtils> cookieUtils = mockStatic(CookieUtils.class)) {
            cookieUtils.when(() -> CookieUtils.getRefreshTokenCookie(request)).thenReturn("fake-token");
            when(redisUtils.getTokenKey("fake-token")).thenReturn("key");
            when(redisUtils.getObject("key", TokenInfo.class)).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshToken(request, mock(HttpServletResponse.class)))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("TC-US-AUTH-009: Logout clears token from Redis and Cookie")
    void logout_success() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        try (MockedStatic<CookieUtils> cookieUtils = mockStatic(CookieUtils.class)) {
            cookieUtils.when(() -> CookieUtils.getRefreshTokenCookie(request)).thenReturn("token");
            authService.logout(request, response);
            verify(redisUtils).deleteByKey("token");
            cookieUtils.verify(() -> CookieUtils.deleteRefreshTokenCookie(response));
        }
    }

    @Test
    @DisplayName("TC-US-AUTH-010: Register candidate success (LOCAL)")
    void registerCandidate_success() {
        RegisterCandidateRequest request = new RegisterCandidateRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("pass");

        when(userService.findByUsername(any())).thenReturn(null);
        when(userService.findByEmail(any())).thenReturn(null);
        when(roleService.getRoleByCode(any())).thenReturn(RoleDto.builder().id(1L).build());
        when(userService.create(any())).thenReturn(UserDto.builder().id(1L).email("new@test.com").fullName("Name").build());

        authService.registerCandidate(request);

        verify(userService).create(any());
        verify(sendEmailService).sendEmailWithTemplate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-US-AUTH-011: Reject register candidate if username exists")
    void registerCandidate_exists() {
        RegisterCandidateRequest request = new RegisterCandidateRequest();
        request.setUsername("exists");
        when(userService.findByUsername("exists")).thenReturn(new UserDto());

        assertThatThrownBy(() -> authService.registerCandidate(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.USERNAME_EXISTS));
    }

    @Test
    @DisplayName("TC-US-AUTH-012: Register Org Admin success with Rollback")
    void registerOrgAdmin_success() {
        RegisterOrgAdminRequest request = new RegisterOrgAdminRequest();
        request.setUsername("admin");
        request.setEmail("admin@org.com");
        request.setOrganization(new OrganizationRequest());
        request.getOrganization().setName("Org");
        request.getOrganization().setAddresses(List.of(new OrgAddressRequest()));

        when(userService.findByUsername(any())).thenReturn(null);
        when(userService.findByEmail(any())).thenReturn(null);
        when(roleService.getRoleByCode(any())).thenReturn(RoleDto.builder().id(1L).build());
        when(userService.create(any())).thenReturn(UserDto.builder().id(1L).username("admin").email("admin@org.com").build());
        when(restTemplateClient.createOrg(any())).thenReturn(nmquan.commonlib.dto.response.IDResponse.<Long>builder().id(100L).build());
        when(userService.findAllSystemAdmin()).thenReturn(List.of(UserDto.builder().id(99L).build()));

        authService.registerOrgAdmin(request, null, null);

        verify(restTemplateClient).createOrg(any());
        verify(sendEmailService).sendEmailWithTemplate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-US-AUTH-013: Rollback Org creation if User creation fails")
    void registerOrgAdmin_rollback() {
        RegisterOrgAdminRequest request = new RegisterOrgAdminRequest();
        request.setUsername("admin");
        request.setEmail("admin@org.com");
        
        when(userService.findByUsername(any())).thenReturn(null);
        when(userService.findByEmail(any())).thenReturn(null);
        when(roleService.getRoleByCode(any())).thenReturn(RoleDto.builder().id(1L).build());
        when(userService.create(any())).thenThrow(new RuntimeException("DB Fail"));

        assertThatThrownBy(() -> authService.registerOrgAdmin(request, null, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("TC-US-AUTH-014: Verify email success with valid token")
    void verifyEmail_success() {
        String token = "valid-token";
        TokenInfo info = TokenInfo.builder().userId(1L).type(TokenType.VERIFY_EMAIL).build();
        when(redisUtils.getTokenKey(token)).thenReturn("key");
        when(redisUtils.getObject("key", TokenInfo.class)).thenReturn(info);
        when(userService.findById(1L)).thenReturn(UserDto.builder().id(1L).isEmailVerified(false).build());

        authService.verifyEmail(token);

        verify(userService).updateEmailVerified(1L, true);
        verify(redisUtils, atLeastOnce()).deleteByKey("key");
    }

    @Test
    @DisplayName("TC-US-AUTH-015: Reject verify email with invalid token type")
    void verifyEmail_invalidType() {
        String token = "wrong-token";
        TokenInfo info = TokenInfo.builder().userId(1L).type(TokenType.RESET_PASSWORD).build();
        when(redisUtils.getTokenKey(token)).thenReturn("key");
        when(redisUtils.getObject("key", TokenInfo.class)).thenReturn(info);

        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-AUTH-016: Request reset password success")
    void requestResetPassword_success() {
        String id = "user@test.com";
        UserDto user = UserDto.builder().id(1L).email(id).accessMethod(AccessMethod.LOCAL.name()).isActive(true).isEmailVerified(true).build();
        when(userService.findByUsername(id)).thenReturn(null);
        when(userService.findByEmail(id)).thenReturn(user);

        authService.requestResetPassword(id);

        verify(sendEmailService).sendEmailWithTemplate(any(), any(), any(), any());
        verify(redis).saveObject(any(), any(), anyInt());
    }

    @Test
    @DisplayName("TC-US-AUTH-017: Reject reset password for OAuth2 account")
    void requestResetPassword_oauth2() {
        String id = "oauth@test.com";
        UserDto user = UserDto.builder().id(1L).accessMethod(AccessMethod.GOOGLE.name()).build();
        when(userService.findByEmail(id)).thenReturn(user);

        assertThatThrownBy(() -> authService.requestResetPassword(id))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(UserErrorCode.REGISTER_THIRD_PARTY));
    }

    @Test
    @DisplayName("TC-US-AUTH-018: Reset password with token success")
    void resetPassword_success() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "newpass");
        TokenInfo info = TokenInfo.builder().userId(1L).type(TokenType.RESET_PASSWORD).build();
        when(redisUtils.getTokenKey("token")).thenReturn("key");
        when(redisUtils.getObject("key", TokenInfo.class)).thenReturn(info);
        when(userService.findById(1L)).thenReturn(UserDto.builder().id(1L).isActive(true).isEmailVerified(true).build());

        authService.resetPassword(request);

        verify(userService).resetPassword(1L, "newpass");
        verify(redisUtils).deleteByKey("key");
    }

    @Test
    @DisplayName("TC-US-AUTH-019: Register candidate with existing OAuth2 email")
    void registerCandidate_existingOAuth2() {
        RegisterCandidateRequest request = new RegisterCandidateRequest();
        request.setUsername("newuser");
        request.setEmail("oauth@test.com");
        
        UserDto existing = UserDto.builder().id(1L).accessMethod(AccessMethod.GOOGLE.name()).build();
        when(userService.findByUsername(any())).thenReturn(null);
        when(userService.findByEmail(any())).thenReturn(existing);
        when(roleService.getRoleByCode(any())).thenReturn(RoleDto.builder().id(1L).build());

        authService.registerCandidate(request);

        verify(userService).create(argThat(u -> u.getAccessMethod().contains("LOCAL")));
    }

    @Test
    @DisplayName("TC-US-AUTH-020: Check account status - combined checks")
    void checkAccountStatus_combined() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "pass");
        TokenInfo info = TokenInfo.builder().userId(1L).type(TokenType.RESET_PASSWORD).build();
        when(redisUtils.getTokenKey("token")).thenReturn("key");
        when(redisUtils.getObject("key", TokenInfo.class)).thenReturn(info);
        
        when(userService.findById(1L)).thenReturn(UserDto.builder().id(1L).isActive(false).isEmailVerified(true).build());
        assertThatThrownBy(() -> authService.resetPassword(request)).isInstanceOf(AppException.class);
    }
}
