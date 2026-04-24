package org.elearning.backend.classroom.exception;

public class ClassroomBadRequestException extends RuntimeException {
    public ClassroomBadRequestException(String message) {
        super(message);
    }
}