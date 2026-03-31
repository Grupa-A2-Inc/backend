package org.elearning.backend.content.exception;

import java.util.UUID;

public class LessonResourceNotFoundException extends RuntimeException {
    public LessonResourceNotFoundException(UUID id) {
        super("Lesson resource not found with id: " + id);
    }
}