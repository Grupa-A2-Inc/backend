package org.elearning.backend.security.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtHandlersTest {

    private JwtAuthenticationEntryPoint authenticationEntryPoint;
    private JwtAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        authenticationEntryPoint = new JwtAuthenticationEntryPoint();
        accessDeniedHandler = new JwtAccessDeniedHandler();
    }

    @Test
    void jwtAuthenticationEntryPoint_canBeInstantiated() {
        assertThat(authenticationEntryPoint).isNotNull();
    }

    @Test
    void jwtAccessDeniedHandler_canBeInstantiated() {
        assertThat(accessDeniedHandler).isNotNull();
    }

    @Test
    void jwtAuthenticationEntryPoint_writesUnauthorizedError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Unauthorized");
    }

    @Test
    void jwtAccessDeniedHandler_writesForbiddenJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("blocked"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Access denied\"}");
    }
}
