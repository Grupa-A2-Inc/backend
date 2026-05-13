package org.elearning.backend.ai.exception;

import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AiExceptionHandlerTest {

    private final AiExceptionHandler handler = new AiExceptionHandler();

    @Test
    void handlesAiApiException() {
        var response = handler.handleAiApiException(new AiApiException("gateway"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("message", "gateway");
    }

    @Test
    void handlesAiTimeoutException() {
        var response = handler.handleAiTimeoutException(new AiTimeoutException("timeout"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).containsEntry("message", "timeout");
    }

    @Test
    void handlesValidationException() {
        var response = handler.handleValidationException(new ValidationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("message", "invalid");
    }

    @Test
    void handlesResourceConflict() {
        var response = handler.handleResourceConflict(new ResourceConflictException("conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "conflict");
    }

    @Test
    void handlesDoesNotExist() {
        var response = handler.handleDoesNotExist(new DoesNotExistException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }

    @Test
    void handlesJsonSerializing() {
        var response = handler.handleJsonSerializing(new JsonSerializingException("serialize"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "serialize");
    }

    @Test
    void handlesAdaptiveServiceUnavailable() {
        var response = handler.handleAdaptiveServiceUnavailable(new AdaptiveServiceUnavailableException("down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("message", "down");
    }
}
