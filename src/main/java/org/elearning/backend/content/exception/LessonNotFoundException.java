package org.elearning.backend.content.exception;


import  java.util.UUID;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(UUID id) {
        super("Lesson not found with id: " + id);
    }
}
