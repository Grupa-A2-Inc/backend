package org.elearning.backend.subscription.exception;

public class StripeWebhookVerificationException extends RuntimeException {

    public StripeWebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
