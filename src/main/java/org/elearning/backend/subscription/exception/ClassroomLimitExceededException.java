package org.elearning.backend.subscription.exception;

import java.util.UUID;

public class ClassroomLimitExceededException extends RuntimeException {
    public ClassroomLimitExceededException(UUID organizationId, int limit) {
        super("Organization " + organizationId + " has reached the maximum classroom limit of " + limit + ".");
    }
}