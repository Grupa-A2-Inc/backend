package org.elearning.backend.user.exception;

public class UserOrganizationNotFoundException extends RuntimeException {
    public UserOrganizationNotFoundException(String message) {
        super(message);
    }
}
