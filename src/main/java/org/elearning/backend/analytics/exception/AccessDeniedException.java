package org.elearning.backend.analytics.exception;

import java.util.UUID;

public class AccessDeniedException extends org.springframework.security.access.AccessDeniedException {
    public AccessDeniedException(UUID userId) {
        super("User " + userId.toString() + " has no access to this field");
    }
}