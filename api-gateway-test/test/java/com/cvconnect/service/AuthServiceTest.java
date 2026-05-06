package com.cvconnect.service;

import com.cvconnect.dto.Response;
import com.cvconnect.dto.VerifyRequest;
import com.cvconnect.dto.VerifyResponse;
import com.cvconnect.repository.AuthClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void verify_shouldWrapTokenInVerifyRequestAndReturnClientResponse() {
        // Test Case ID: TC-GW-AUTHS-001
        // Objective: verify that AuthService passes the exact token to AuthClient and returns the same Mono response.
        AuthClient authClient = mock(AuthClient.class);
        AuthService authService = new AuthService(authClient);
        Response<VerifyResponse> expectedResponse = Response.<VerifyResponse>builder()
                .data(VerifyResponse.builder().isValid(true).build())
                .code(1000)
                .build();
        when(authClient.verify(org.mockito.ArgumentMatchers.any(VerifyRequest.class)))
                .thenReturn(Mono.just(expectedResponse));

        Response<VerifyResponse> actualResponse = authService.verify("valid.jwt.token").block();

        ArgumentCaptor<VerifyRequest> requestCaptor = ArgumentCaptor.forClass(VerifyRequest.class);
        verify(authClient).verify(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getToken()).isEqualTo("valid.jwt.token");
        assertThat(actualResponse).isSameAs(expectedResponse);
    }

    @Test
    void verify_shouldPropagateAuthClientError() {
        // Test Case ID: TC-GW-AUTHS-002
        // Objective: verify that AuthService does not swallow errors returned by AuthClient.
        AuthClient authClient = mock(AuthClient.class);
        AuthService authService = new AuthService(authClient);
        when(authClient.verify(org.mockito.ArgumentMatchers.any(VerifyRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("client failure")));

        assertThatThrownBy(() -> authService.verify("bad-token").block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("client failure");
    }
}
