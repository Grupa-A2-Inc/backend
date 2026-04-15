package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(UUID courseId) {
        super("Course not found with ID: " + courseId);
    }
}
