package org.elearning.backend.user.exception;

public class UserRoleNotFoundException extends RuntimeException {
    public UserRoleNotFoundException(String message) {
        super(message);
    }
}
