package org.elearning.backend.classroom.exception;

public class CourseNotEligibleException extends RuntimeException {
    public CourseNotEligibleException(String message) {
        super(message);
    }
}