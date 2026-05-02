package org.elearning.backend.feedback.exception;

public class AlreadyResolved extends RuntimeException {
    public AlreadyResolved(String message) {
        super(message);
    }
}
