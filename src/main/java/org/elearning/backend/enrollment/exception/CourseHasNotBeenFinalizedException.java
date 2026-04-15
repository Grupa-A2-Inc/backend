package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CourseHasNotBeenFinalizedException extends RuntimeException {
    public CourseHasNotBeenFinalizedException(UUID enrollmentId) {
        super("Course has not been finalized at enrollment with ID: " + enrollmentId.toString());
    }
}
