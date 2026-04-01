package org.elearning.backend.assessment.exception;

public class AttemptInProgressException extends RuntimeException {
    public AttemptInProgressException(String message) {
        super(message);
    }
}
