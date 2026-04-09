package org.elearning.backend.auth.exception;

public class AuthResourceNotFoundException extends RuntimeException {
    public AuthResourceNotFoundException(String message) {
        super(message);
    }
}
