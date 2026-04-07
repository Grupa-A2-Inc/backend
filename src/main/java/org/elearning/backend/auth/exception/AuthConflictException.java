package org.elearning.backend.auth.exception;

public class AuthConflictException extends RuntimeException {
    public AuthConflictException(String message) {
        super(message);
    }
}
