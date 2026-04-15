package org.elearning.backend.organization.exception;

public class OrganizationOwnerNotFoundException extends RuntimeException {
    public OrganizationOwnerNotFoundException(String message) {
        super(message);
    }
}
