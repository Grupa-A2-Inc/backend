package org.elearning.backend.analytics.exception;

public class ValidationException extends RuntimeException {
    /**
     * Create a ValidationException with the specified detail message.
     *
     * @param message the detail message describing the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
}
