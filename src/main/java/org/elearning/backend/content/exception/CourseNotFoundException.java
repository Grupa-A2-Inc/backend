package org.elearning.backend.content.exception;

import java.util.UUID;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(UUID id) {
        super("Course not found with id: " + id);
    }
}