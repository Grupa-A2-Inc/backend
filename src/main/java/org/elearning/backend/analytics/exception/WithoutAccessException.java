package org.elearning.backend.analytics.exception;

import java.util.UUID;

public class WithoutAccessException extends org.springframework.security.access.AccessDeniedException {
    public WithoutAccessException(UUID userId) {
        super("User " + userId.toString() + " has no access to this field");
    }
}