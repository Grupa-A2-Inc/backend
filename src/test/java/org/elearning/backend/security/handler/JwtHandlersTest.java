package org.elearning.backend.security.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtHandlersTest {

    @Test
    void jwtAuthenticationEntryPoint_canBeInstantiated() {
        assertThat(new JwtAuthenticationEntryPoint()).isNotNull();
    }

    @Test
    void jwtAccessDeniedHandler_canBeInstantiated() {
        assertThat(new JwtAccessDeniedHandler()).isNotNull();
    }
}
