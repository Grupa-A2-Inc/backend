package org.elearning.backend.subscription.exception;

public class StripeCheckoutException extends RuntimeException {

    public StripeCheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
