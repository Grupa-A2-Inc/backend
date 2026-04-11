package org.elearning.backend.user.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserExceptionHandlerTest {

    private final UserExceptionHandler handler = new UserExceptionHandler();

    @Test
    void handlesUserAlreadyExists() {
        var response = handler.handleAlreadyExists(new UserAlreadyExistsException("duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "duplicate");
    }

    @Test
    void handlesUserNotFound() {
        var response = handler.handleNotFound(new UserNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }

    @Test
    void handlesUserRoleNotFound() {
        var response = handler.handleNotFound(new UserRoleNotFoundException("role missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "role missing");
    }

    @Test
    void handlesUserOrganizationNotFound() {
        var response = handler.handleNotFound(new UserOrganizationNotFoundException("organization missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "organization missing");
    }

    @Test
    void handlesBadRequest() {
        var response = handler.handleBadRequest(new UserBadRequestException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "invalid");
    }

    @Test
    void handlesValidationUsingFirstFieldErrorMessage() {
        var response = handler.handleValidation(validationException(List.of(new FieldError("user", "email", "Email is required"))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Email is required");
    }

    @Test
    void handlesValidationFallbackWhenFieldErrorsAreMissing() {
        var response = handler.handleValidation(validationException(List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Validation failed");
    }

    @Test
    void handlesAccessDenied() {
        var response = handler.handleAccessDenied(new AccessDeniedException("blocked"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "blocked");
    }

    @Test
    void handlesUnexpectedExceptions() {
        var response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "boom");
    }

    private MethodArgumentNotValidException validationException(List<FieldError> fieldErrors) {
        try {
            Method method = getClass().getDeclaredMethod("sampleValidationTarget", String.class);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
            fieldErrors.forEach(bindingResult::addError);
            return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private void sampleValidationTarget(String value) {
        throw new UnsupportedOperationException("Test helper for MethodParameter only");
    }
}
