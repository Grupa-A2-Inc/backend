package org.elearning.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final TestableGlobalExceptionHandler handler = new TestableGlobalExceptionHandler();

    @Test
    void buildErrorResponse_includesStatusAndMessage() {
        ResponseEntity<Map<String, Object>> response = handler.exposeBuildErrorResponse(
                new IllegalStateException("boom"),
                HttpStatus.BAD_REQUEST
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("message", "boom")
                .containsKey("timestamp");
    }

    private static final class TestableGlobalExceptionHandler extends GlobalExceptionHandler {
        ResponseEntity<Map<String, Object>> exposeBuildErrorResponse(Exception ex, HttpStatus status) {
            return buildErrorResponse(ex, status);
        }
    }
}
