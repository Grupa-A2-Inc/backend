package org.elearning.backend.assessment.exception;

public class LessonAlreadyHasTestException extends RuntimeException {
    public LessonAlreadyHasTestException(String message) {
        super(message);
    }
}
