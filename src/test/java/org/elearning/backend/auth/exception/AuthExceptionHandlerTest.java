package org.elearning.backend.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void handlesConflictExceptions() {
        var response = handler.handleConflict(new AuthConflictException("exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "exists");
    }

    @Test
    void handlesInvalidCredentials() {
        var response = handler.handleInvalidCredentials(new InvalidCredentialsException("bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("message", "bad creds");
    }
}
