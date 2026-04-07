package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CourseEnrollmentNotFoundException extends RuntimeException {
    public CourseEnrollmentNotFoundException(UUID message) {
        super("Course enrollment not found with ID: " + message);
    }
}
