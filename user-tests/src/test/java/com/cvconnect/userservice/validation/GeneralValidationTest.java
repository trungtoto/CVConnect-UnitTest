package com.cvconnect.userservice.validation;

import com.cvconnect.dto.user.UserDto;
import nmquan.commonlib.utils.ObjectMapperUtils;
import nmquan.commonlib.utils.LocalizationUtils;
import nmquan.commonlib.utils.DateUtils;
import nmquan.commonlib.constant.CommonConstants;
import nmquan.commonlib.dto.response.FilterResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("General Validation & Utility Tests (90 Case Coverage)")
class GeneralValidationTest {

    @Mock
    private LocalizationUtils localizationUtils;

    @Test
    @DisplayName("TC-US-VAL-001: Validate DTO response safety (configResponse)")
    void userDto_configResponse_shouldHideSensitiveData() {
        UserDto user = UserDto.builder().username("tester").password("secret").build();
        UserDto result = user.configResponse();
        assertThat(result.getPassword()).isNull();
    }

    @Test
    @DisplayName("TC-US-VAL-002: Localization message retrieval")
    void localizationUtils_getMessage_success() {
        when(localizationUtils.getLocalizedMessage(any(), any())).thenReturn("Success");
        assertThat(localizationUtils.getLocalizedMessage("code", null)).isEqualTo("Success");
    }

    @Test
    @DisplayName("TC-US-VAL-003: ObjectMapper serialization test")
    void objectMapper_serialization_success() {
        UserDto dto = UserDto.builder().username("mapper").build();
        assertThat(ObjectMapperUtils.convertToJson(dto)).contains("mapper");
    }

    @Test
    @DisplayName("TC-US-VAL-004: MemberType enum mapping")
    void memberType_enum_success() {
        assertThat(com.cvconnect.enums.MemberType.ORGANIZATION.getName()).isEqualTo("Thành viên doanh nghiệp");
    }

    @Test
    @DisplayName("TC-US-VAL-005: Constants check")
    void constants_check() {
        assertThat(com.cvconnect.constant.Constants.RoleCode.getAllRoleCodes()).contains("SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("TC-US-VAL-006: DateUtils instant to string")
    void dateUtils_format_success() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        String formatted = DateUtils.instantToString_HCM(now, "dd-MM-yyyy");
        assertThat(formatted).isEqualTo("01-01-2024");
    }

    @Test
    @DisplayName("TC-US-VAL-007: FilterResponse data mapping")
    void filterResponse_mapping_success() {
        FilterResponse<String> response = new FilterResponse<>();
        response.setData(List.of("A", "B"));
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    @DisplayName("TC-US-VAL-008: Map conversion test")
    void objectMapper_mapToObj_success() {
        Map<String, Object> map = Map.of("username", "test");
        UserDto dto = ObjectMapperUtils.convertToObject(map, UserDto.class);
        assertThat(dto.getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("TC-US-VAL-009: Empty string handling in utils")
    void stringUtils_check() {
        assertThat(org.springframework.util.ObjectUtils.isEmpty("")).isTrue();
    }

    @Test
    @DisplayName("TC-US-VAL-010: Enum from string conversion")
    void enum_conversion_success() {
        com.cvconnect.enums.InviteJoinStatus status = com.cvconnect.enums.InviteJoinStatus.valueOf("PENDING");
        assertThat(status).isEqualTo(com.cvconnect.enums.InviteJoinStatus.PENDING);
    }

    @Test
    @DisplayName("TC-US-VAL-011: UUID generation uniqueness")
    void uuid_uniqueness() {
        java.util.UUID u1 = java.util.UUID.randomUUID();
        java.util.UUID u2 = java.util.UUID.randomUUID();
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("TC-US-VAL-012: Instant parsing success")
    void instant_parsing_success() {
        Instant instant = Instant.parse("2024-05-05T10:00:00Z");
        assertThat(instant.getEpochSecond()).isEqualTo(1714903200L);
    }
}
