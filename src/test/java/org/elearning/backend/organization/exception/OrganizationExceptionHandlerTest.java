package org.elearning.backend.organization.exception;

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

class OrganizationExceptionHandlerTest {

    private final OrganizationExceptionHandler handler = new OrganizationExceptionHandler();

    @Test
    void handlesOrganizationNotFound() {
        var response = handler.handleNotFound(new OrganizationNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }

    @Test
    void handlesOrganizationOwnerNotFound() {
        var response = handler.handleNotFound(new OrganizationOwnerNotFoundException("owner missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "owner missing");
    }

    @Test
    void handlesValidationUsingFirstFieldErrorMessage() {
        var response = handler.handleValidation(validationException(List.of(new FieldError("organization", "name", "Name is required"))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Name is required");
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
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "organization");
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
