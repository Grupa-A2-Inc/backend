package org.elearning.backend.ai.exception;

public class AiTimeoutException extends RuntimeException {
    /**
     * Creates a new AiTimeoutException with the specified detail message indicating an AI operation timed out.
     *
     * @param message the detail message describing the timeout condition
     */
    public AiTimeoutException(String message) {
        super(message);
    }
}
