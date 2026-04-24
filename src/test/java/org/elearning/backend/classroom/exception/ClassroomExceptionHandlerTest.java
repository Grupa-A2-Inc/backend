package org.elearning.backend.classroom.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassroomExceptionHandlerTest {

    private final ClassroomExceptionHandler handler = new ClassroomExceptionHandler();

    @Test
    void handlesNotFound() {
        var response = handler.handleNotFound(new ClassroomNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }

    @Test
    void handlesConflict() {
        var response = handler.handleConflict(new ClassroomConflictException("duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "duplicate");
    }

    @Test
    void handlesBadRequest() {
        var response = handler.handleBadRequest(new ClassroomBadRequestException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "invalid");
    }

    @Test
    void handlesIllegalArgumentAsBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("bad argument"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "bad argument");
    }

    @Test
    void handlesValidationUsingFirstFieldErrorMessage() {
        var response = handler.handleValidation(
                validationException(List.of(new FieldError("classroom", "name", "Classroom name is required")))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Classroom name is required");
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

    private MethodArgumentNotValidException validationException(List<FieldError> fieldErrors) {
        try {
            Method method = getClass().getDeclaredMethod("sampleValidationTarget", String.class);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "classroom");
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
