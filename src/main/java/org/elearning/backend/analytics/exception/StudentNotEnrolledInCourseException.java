package org.elearning.backend.analytics.exception;

public class StudentNotEnrolledInCourseException extends RuntimeException {
    /**
     * Creates a StudentNotEnrolledInCourseException containing a detail message.
     *
     * @param message the detail message explaining why the student is not enrolled in the course
     */
    public StudentNotEnrolledInCourseException(String message) {
        super(message);
    }
}
