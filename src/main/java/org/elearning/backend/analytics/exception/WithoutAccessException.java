package org.elearning.backend.analytics.exception;

import java.util.UUID;

public class WithoutAccessException extends org.springframework.security.access.AccessDeniedException {
    /**
     * Exception indicating the specified user does not have access to a field.
     *
     * @param userId the UUID of the user who lacks access
     */
    public WithoutAccessException(UUID userId) {
        super("User " + userId.toString() + " has no access to this field");
    }
}