package com.cvconnect.userservice.role;

import com.cvconnect.dto.role.RoleDto;
import com.cvconnect.dto.role.RoleRequest;
import com.cvconnect.entity.Role;
import com.cvconnect.enums.MemberType;
import com.cvconnect.repository.RoleRepository;
import com.cvconnect.service.RoleMenuService;
import com.cvconnect.service.impl.RoleServiceImpl;
import nmquan.commonlib.exception.AppException;
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
@DisplayName("RoleServiceImpl - Unit Tests (90 Case Coverage)")
class RoleServiceImplTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RoleMenuService roleMenuService;
    
    @InjectMocks private RoleServiceImpl roleService;

    @Test
    @DisplayName("TC-US-RM-001: Get detail by ID success")
    void getDetail_success() {
        Role role = new Role();
        role.setId(1L);
        role.setCode("ADMIN");
        role.setMemberType(MemberType.MANAGEMENT);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleMenuService.findByRoleId(1L)).thenReturn(List.of());
        
        RoleDto result = roleService.getDetail(1L);
        assertThat(result.getCode()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("TC-US-RM-002: Create role success")
    void createRoles_success() {
        RoleRequest request = new RoleRequest();
        request.setCode("NEW_ROLE");
        when(roleRepository.findByCode("NEW_ROLE")).thenReturn(null);
        
        roleService.createRoles(request);
        verify(roleRepository).save(any());
    }

    @Test
    @DisplayName("TC-US-RM-003: Throw error if role code exists")
    void createRoles_exists() {
        RoleRequest request = new RoleRequest();
        request.setCode("EXISTING");
        when(roleRepository.findByCode("EXISTING")).thenReturn(new Role());
        
        assertThatThrownBy(() -> roleService.createRoles(request)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC-US-RM-004: Delete role success")
    void deleteByIds_success() {
        Role role = new Role();
        role.setCode("CUSTOM_ROLE");
        when(roleRepository.findAllById(any())).thenReturn(List.of(role));
        
        roleService.deleteByIds(List.of(1L));
        verify(roleRepository).deleteByIds(any());
    }

    @Test
    @DisplayName("TC-US-RM-005: Reject deletion of system roles")
    void deleteByIds_systemRole() {
        Role role = new Role();
        role.setCode("SYSTEM_ADMIN"); // One of the important roles
        when(roleRepository.findAllById(any())).thenReturn(List.of(role));
        
        assertThatThrownBy(() -> roleService.deleteByIds(List.of(1L))).isInstanceOf(AppException.class);
    }
}
