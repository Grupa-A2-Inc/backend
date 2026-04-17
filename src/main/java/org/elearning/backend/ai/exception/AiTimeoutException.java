package org.elearning.backend.ai.exception;

public class AiTimeoutException extends RuntimeException {
    public AiTimeoutException(String message) {
        super(message);
    }
}
