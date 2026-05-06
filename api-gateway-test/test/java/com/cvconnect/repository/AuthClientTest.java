package com.cvconnect.repository;

import com.cvconnect.dto.Response;
import com.cvconnect.dto.VerifyRequest;
import com.cvconnect.dto.VerifyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

class AuthClientTest {

    @Test
    void verify_shouldDeclareExpectedPostExchangeContract() throws NoSuchMethodException {
        // Test Case ID: TC-GW-AUTHC-001
        // Objective: verify the HTTP contract used by API Gateway to call user-service token verification.
        Method verifyMethod = AuthClient.class.getMethod("verify", VerifyRequest.class);
        PostExchange postExchange = verifyMethod.getAnnotation(PostExchange.class);

        assertThat(postExchange).isNotNull();
        assertThat(postExchange.url()).isEqualTo("/user/auth/verify");
        assertThat(postExchange.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void verify_shouldUseRequestBodyAndReactiveResponseType() throws NoSuchMethodException {
        // Test Case ID: TC-GW-AUTHC-002
        // Objective: verify AuthClient.verify accepts a JSON request body and returns Mono<Response<VerifyResponse>>.
        Method verifyMethod = AuthClient.class.getMethod("verify", VerifyRequest.class);
        Parameter requestParameter = verifyMethod.getParameters()[0];
        Type monoType = verifyMethod.getGenericReturnType();

        assertThat(requestParameter.getAnnotation(RequestBody.class)).isNotNull();
        assertThat(monoType).isInstanceOf(ParameterizedType.class);
        ParameterizedType monoParameterizedType = (ParameterizedType) monoType;
        assertThat(monoParameterizedType.getRawType()).isEqualTo(Mono.class);
        assertThat(monoParameterizedType.getActualTypeArguments()[0].getTypeName())
                .isEqualTo(Response.class.getName() + "<" + VerifyResponse.class.getName() + ">");
    }
}
