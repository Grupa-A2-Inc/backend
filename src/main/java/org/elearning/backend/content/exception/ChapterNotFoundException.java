package org.elearning.backend.content.exception;


import java.util.UUID;


public class ChapterNotFoundException extends RuntimeException {
    public ChapterNotFoundException(UUID id) {
        super("Chapter not found with id: " + id);
    }
}