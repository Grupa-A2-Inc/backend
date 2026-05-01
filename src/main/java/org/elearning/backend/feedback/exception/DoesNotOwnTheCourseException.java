package org.elearning.backend.feedback.exception;

public class DoesNotOwnTheCourseException extends RuntimeException {
    public DoesNotOwnTheCourseException(String message) {
        super(message);
    }
}
