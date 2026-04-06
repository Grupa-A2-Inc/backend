package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CourseIsPrivateException extends RuntimeException {
    public CourseIsPrivateException(UUID courseId) {
        super("Course with ID " + courseId + " is private");
    }
}
