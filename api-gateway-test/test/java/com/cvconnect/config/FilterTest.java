package com.cvconnect.config;

import com.cvconnect.dto.Response;
import com.cvconnect.dto.VerifyResponse;
import com.cvconnect.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ParameterizedTest(name = "{0} public endpoint should bypass auth")
    @MethodSource("publicEndpointCases")
    void filter_shouldBypassAuthenticationForPublicEndpoint(String testCaseId, HttpMethod method, String path) {
        // Selected Test Case IDs: TC-GW-FLT-001, 005, 010, 013, 020
        AuthService authService = mock(AuthService.class);
        Filter filter = new Filter(authService, OBJECT_MAPPER);
        MockServerWebExchange exchange = exchange(method, path, null);
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);

        filter.filter(exchange, mockedChain(chainWasCalled)).block();

        assertThat(testCaseId).startsWith("TC-GW-FLT-");
        assertThat(chainWasCalled).isTrue();
        verify(authService, never()).verify(anyString());
    }

    @ParameterizedTest(name = "{0} protected endpoint without auth should return 401")
    @MethodSource("protectedEndpointWithoutAuthCases")
    void filter_shouldRejectProtectedEndpointWhenAuthorizationHeaderIsMissing(
            String testCaseId, HttpMethod method, String path
    ) {
        // Selected Test Case IDs: TC-GW-FLT-021, 024, 035, 039, 040
        AuthService authService = mock(AuthService.class);
        Filter filter = new Filter(authService, OBJECT_MAPPER);
        MockServerWebExchange exchange = exchange(method, path, null);
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);

        filter.filter(exchange, mockedChain(chainWasCalled)).block();

        assertThat(testCaseId).startsWith("TC-GW-FLT-");
        assertThat(chainWasCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseBody(exchange)).contains("\"code\":401");
        verify(authService, never()).verify(anyString());
    }

    @ParameterizedTest(name = "{0} valid bearer token should reach downstream service")
    @MethodSource("validBearerTokenCases")
    void filter_shouldRouteProtectedEndpointWhenBearerTokenIsValid(
            String testCaseId, HttpMethod method, String path, String token
    ) {
        // Selected Test Case IDs: TC-GW-FLT-041, 044, 055, 059, 060
        AuthService authService = mock(AuthService.class);
        when(authService.verify(token)).thenReturn(Mono.just(Response.<VerifyResponse>builder()
                .data(VerifyResponse.builder().isValid(true).build())
                .build()));
        Filter filter = new Filter(authService, OBJECT_MAPPER);
        MockServerWebExchange exchange = exchange(method, path, "Bearer " + token);
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);

        filter.filter(exchange, mockedChain(chainWasCalled)).block();

        assertThat(testCaseId).startsWith("TC-GW-FLT-");
        assertThat(chainWasCalled).isTrue();
        verify(authService).verify(token);
    }

    @ParameterizedTest(name = "{0} invalid bearer token should return auth service error")
    @MethodSource("invalidBearerTokenCases")
    void filter_shouldReturnAuthServiceErrorWhenBearerTokenIsInvalid(
            String testCaseId, String path, String token, HttpStatus status, int code, String message
    ) {
        // Selected Test Case IDs: TC-GW-FLT-061, 063, 072, 075
        AuthService authService = mock(AuthService.class);
        when(authService.verify(token)).thenReturn(Mono.just(Response.<VerifyResponse>builder()
                .data(VerifyResponse.builder()
                        .isValid(false)
                        .status(status)
                        .code(code)
                        .message(message)
                        .build())
                .build()));
        Filter filter = new Filter(authService, OBJECT_MAPPER);
        MockServerWebExchange exchange = exchange(HttpMethod.GET, path, "Bearer " + token);
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);

        filter.filter(exchange, mockedChain(chainWasCalled)).block();

        assertThat(testCaseId).startsWith("TC-GW-FLT-");
        assertThat(chainWasCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
        assertThat(responseBody(exchange)).contains("\"code\":" + code, message);
    }

    @ParameterizedTest(name = "{0} auth service failure should return 500")
    @MethodSource("authServiceFailureCases")
    void filter_shouldReturnInternalServerErrorWhenAuthServiceFails(
            String testCaseId, String path, String token, String errorMessage
    ) {
        // Selected Test Case IDs: TC-GW-FLT-081
        AuthService authService = mock(AuthService.class);
        when(authService.verify(token)).thenReturn(Mono.error(new RuntimeException(errorMessage)));
        Filter filter = new Filter(authService, OBJECT_MAPPER);
        MockServerWebExchange exchange = exchange(HttpMethod.GET, path, "Bearer " + token);
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);

        filter.filter(exchange, mockedChain(chainWasCalled)).block();

        assertThat(testCaseId).startsWith("TC-GW-FLT-");
        assertThat(chainWasCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseBody(exchange)).contains("\"code\":500");
    }

    private static Stream<Arguments> publicEndpointCases() {
        return Stream.of(
                // Test Case ID: TC-GW-FLT-001
                Arguments.of("TC-GW-FLT-001", HttpMethod.GET, "/v3/api-docs"),
                // Test Case ID: TC-GW-FLT-005
                Arguments.of("TC-GW-FLT-005", HttpMethod.POST, "/api/v1/user/auth/login"),
                // Test Case ID: TC-GW-FLT-010
                Arguments.of("TC-GW-FLT-010", HttpMethod.GET, "/api/v1/user/oauth2/authorization/google"),
                // Test Case ID: TC-GW-FLT-013
                Arguments.of("TC-GW-FLT-013", HttpMethod.GET, "/api/v1/core/industry/public"),
                // Test Case ID: TC-GW-FLT-020
                Arguments.of("TC-GW-FLT-020", HttpMethod.POST, "/api/v1/user/auth/logout")
        );
    }

    private static Stream<Arguments> protectedEndpointWithoutAuthCases() {
        return Stream.of(
                // Test Case ID: TC-GW-FLT-021
                Arguments.of("TC-GW-FLT-021", HttpMethod.GET, "/api/v1/user/user/my-info/1"),
                // Test Case ID: TC-GW-FLT-024
                Arguments.of("TC-GW-FLT-024", HttpMethod.POST, "/api/v1/user/org-member/invite"),
                // Test Case ID: TC-GW-FLT-035
                Arguments.of("TC-GW-FLT-035", HttpMethod.GET, "/api/v1/notify/notification"),
                // Test Case ID: TC-GW-FLT-039
                Arguments.of("TC-GW-FLT-039", HttpMethod.GET, "/socket/info"),
                // Test Case ID: TC-GW-FLT-040
                Arguments.of("TC-GW-FLT-040", HttpMethod.GET, "/api/v1/core/dashboard")
        );
    }

    private static Stream<Arguments> validBearerTokenCases() {
        return Stream.of(
                // Test Case ID: TC-GW-FLT-041
                Arguments.of("TC-GW-FLT-041", HttpMethod.GET, "/api/v1/user/user/my-info/1", "valid-token-046"),
                // Test Case ID: TC-GW-FLT-044
                Arguments.of("TC-GW-FLT-044", HttpMethod.POST, "/api/v1/user/org-member/invite", "valid-token-049"),
                // Test Case ID: TC-GW-FLT-055
                Arguments.of("TC-GW-FLT-055", HttpMethod.GET, "/api/v1/notify/notification", "valid-token-060"),
                // Test Case ID: TC-GW-FLT-059
                Arguments.of("TC-GW-FLT-059", HttpMethod.GET, "/socket/info", "valid-token-064"),
                // Test Case ID: TC-GW-FLT-060
                Arguments.of("TC-GW-FLT-060", HttpMethod.GET, "/api/v1/core/dashboard", "valid-token-065")
        );
    }

    private static Stream<Arguments> invalidBearerTokenCases() {
        return Stream.of(
                // Test Case ID: TC-GW-FLT-061
                Arguments.of("TC-GW-FLT-061", "/api/v1/user/user/my-info/1", "invalid-token-066", HttpStatus.UNAUTHORIZED, 104, "Token expired"),
                // Test Case ID: TC-GW-FLT-063
                Arguments.of("TC-GW-FLT-063", "/api/v1/user/org-member/invite", "invalid-token-068", HttpStatus.FORBIDDEN, 106, "Permission denied"),
                // Test Case ID: TC-GW-FLT-072
                Arguments.of("TC-GW-FLT-072", "/api/v1/notify/notification", "invalid-token-077", HttpStatus.UNAUTHORIZED, 115, "Notification token invalid"),
                // Test Case ID: TC-GW-FLT-075
                Arguments.of("TC-GW-FLT-075", "/socket/info", "invalid-token-080", HttpStatus.UNAUTHORIZED, 118, "Socket token invalid")
        );
    }

    private static Stream<Arguments> authServiceFailureCases() {
        return Stream.of(
                // Test Case ID: TC-GW-FLT-081
                Arguments.of("TC-GW-FLT-081", "/api/v1/user/user/my-info/1", "error-token-086", "user service timeout")
        );
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String authorizationHeader) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.method(method, path);
        if (authorizationHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return MockServerWebExchange.from(requestBuilder.build());
    }

    private GatewayFilterChain mockedChain(AtomicBoolean chainWasCalled) {
        return exchange -> {
            chainWasCalled.set(true);
            return Mono.empty();
        };
    }

    private String responseBody(MockServerWebExchange exchange) {
        Flux<DataBuffer> body = exchange.getResponse().getBody();
        return body.map(buffer -> buffer.toString(StandardCharsets.UTF_8))
                .reduce("", String::concat)
                .block();
    }
}

