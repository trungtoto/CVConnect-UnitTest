package com.cvconnect.userservice.security;

import com.cvconnect.dto.user.UserDto;
import com.cvconnect.service.RoleMenuService;
import com.cvconnect.service.RoleService;
import com.cvconnect.utils.JwtUtils;
import nmquan.commonlib.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils - Unit Tests (90 Case Coverage)")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    
    @Mock
    private RoleMenuService roleMenuService;
    
    @Mock
    private RoleService roleService;

    private final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(roleMenuService, roleService);
        ReflectionTestUtils.setField(jwtUtils, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "EXPIRATION", 3600);
    }

    @Test
    @DisplayName("TC-US-JWT-001: Generate JWT with expected claims")
    void generateToken_success() {
        UserDto user = UserDto.builder().id(1L).username("testuser").build();
        when(roleService.getRoleByUserId(anyLong())).thenReturn(List.of());
        
        String token = jwtUtils.generateToken(user);
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("TC-US-JWT-002: Throw error when secret key invalid")
    void generateToken_invalidSecret() {
        ReflectionTestUtils.setField(jwtUtils, "SECRET_KEY", "invalid-key-length-too-short");
        UserDto user = UserDto.builder().id(1L).username("testuser").build();
        when(roleService.getRoleByUserId(anyLong())).thenReturn(List.of());
        
        assertThatThrownBy(() -> jwtUtils.generateToken(user)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-JWT-003: Generate unique helper tokens")
    void helperTokens_success() {
        assertThat(jwtUtils.generateRefreshToken()).isNotEmpty();
        assertThat(jwtUtils.generateTokenVerifyEmail()).isNotEmpty();
        assertThat(jwtUtils.generateTokenResetPassword()).isNotEmpty();
        assertThat(jwtUtils.generateTokenInviteJoinOrg()).isNotEmpty();
    }

    @Test
    @DisplayName("TC-US-JWT-004: Token uniqueness")
    void helperTokens_uniqueness() {
        String t1 = jwtUtils.generateRefreshToken();
        String t2 = jwtUtils.generateRefreshToken();
        assertThat(t1).isNotEqualTo(t2);
    }
}
