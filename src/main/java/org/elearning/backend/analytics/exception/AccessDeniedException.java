package org.elearning.backend.analytics.exception;

import java.util.UUID;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(UUID userId) {
        super("User " + userId.toString() + " has no access to this field");
    }
}
