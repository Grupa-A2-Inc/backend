package org.elearning.backend.assessment.exception;

public class UserHasNoPermissionException extends RuntimeException {
    public UserHasNoPermissionException(String message) {
        super(message);
    }
}
