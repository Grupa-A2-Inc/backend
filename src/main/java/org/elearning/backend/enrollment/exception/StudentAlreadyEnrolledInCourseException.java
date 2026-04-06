package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class StudentAlreadyEnrolledInCourseException extends RuntimeException{
    public StudentAlreadyEnrolledInCourseException(UUID studentId, UUID courseId) {
        super("Student with id " + studentId + " is already enrolled in course with id " + courseId);
    }
}
