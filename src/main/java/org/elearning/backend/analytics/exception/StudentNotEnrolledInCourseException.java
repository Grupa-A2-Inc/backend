package org.elearning.backend.analytics.exception;

public class StudentNotEnrolledInCourseException extends RuntimeException {
    public StudentNotEnrolledInCourseException(String message) {
        super(message);
    }
}
