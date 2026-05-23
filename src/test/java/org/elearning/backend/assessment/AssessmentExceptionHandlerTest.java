package org.elearning.backend.assessment;

import jakarta.validation.ValidationException;
import org.elearning.backend.assessment.exception.AssessmentExceptionHandler;
import org.elearning.backend.assessment.exception.InvalidAttemptUserException;
import org.elearning.backend.assessment.exception.TestVersionConflictException;
import org.elearning.backend.assessment.exception.UserHasNoPermissionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentExceptionHandlerTest {

    private final AssessmentExceptionHandler handler = new AssessmentExceptionHandler();

    @Test
    void handlesInvalidAttemptUser() {
        var response = handler.handleInvalidAttemptUser(new InvalidAttemptUserException("wrong student"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "wrong student");
    }

    @Test
    void handlesNoPermission() {
        var response = handler.handleNoPermission(new UserHasNoPermissionException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "forbidden");
    }

    @Test
    void handlesValidation() {
        var response = handler.handleValidation(new ValidationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "invalid");
    }

    @Test
    void handlesVersionConflict() {
        var response = handler.handleVersionConflict(new TestVersionConflictException("conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "conflict");
    }
}
