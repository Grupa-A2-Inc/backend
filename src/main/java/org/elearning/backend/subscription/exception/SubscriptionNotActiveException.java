package org.elearning.backend.subscription.exception;

import java.util.UUID;

public class SubscriptionNotActiveException extends RuntimeException {
    public SubscriptionNotActiveException(UUID organizationId) {
        super("Organization " + organizationId + " does not have an active subscription.");
    }
}