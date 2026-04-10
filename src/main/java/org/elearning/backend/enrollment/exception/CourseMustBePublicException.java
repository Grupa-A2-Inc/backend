package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CourseMustBePublicException extends RuntimeException {
    public CourseMustBePublicException(UUID courseId) {

        super("Course with ID: " + courseId.toString() + " is not public");
    }
}
