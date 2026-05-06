package com.cvconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CORSConfigTest {

    @Test
    void corsWebFilter_shouldAllowConfiguredFrontendOriginForPreflightRequest() {
        // Test Case ID: TC-GW-CORS-001
        // Objective: browser preflight from the local Nuxt frontend must be accepted by API Gateway.
        CORSConfig corsConfig = new CORSConfig();
        CorsWebFilter corsWebFilter = corsConfig.corsWebFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost:10094/api/v1/user/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .build()
        );
        AtomicBoolean chainWasCalled = new AtomicBoolean(false);
        WebFilterChain chain = webExchange -> {
            chainWasCalled.set(true);
            return Mono.empty();
        };

        corsWebFilter.filter(exchange, chain).block();

        assertThat(chainWasCalled).isFalse();
        // Spring's mock response may keep status null for successful preflight;
        // the CORS headers below are the observable behavior required by browsers.
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:3000");
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .isEqualTo("true");
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowMethods())
                .contains(HttpMethod.POST);
    }
}
