package org.elearning.backend.subscription.exception;

import java.util.UUID;

public class UserLimitExceededException extends RuntimeException {
    public UserLimitExceededException(UUID organizationId, int limit) {
        super("Organization " + organizationId + " has reached the maximum user limit of " + limit + ".");
    }
}