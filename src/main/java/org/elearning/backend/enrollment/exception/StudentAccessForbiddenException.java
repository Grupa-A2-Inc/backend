package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class StudentAccessForbiddenException extends RuntimeException {
    public StudentAccessForbiddenException(UUID studentId) {
        super("Student with the ID: " + studentId.toString() + " does not have access to this command");
    }

    public StudentAccessForbiddenException(String message) {
        super(message);
    }
}
