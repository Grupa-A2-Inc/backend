package org.elearning.backend.assessment.exception;

public class AlreadyPublishedException extends RuntimeException {
    public AlreadyPublishedException(String message) {
        super(message);
    }
}
