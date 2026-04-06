package org.elearning.backend.enrollment.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

public class EnrollmentExceptionHandler extends GlobalExceptionHandler {
    @ExceptionHandler(CourseIsPrivateException.class)
    public ResponseEntity<Map<String, Object>> handleCourseIsPrivate(CourseIsPrivateException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(StudentAlreadyEnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleStudentAlreadyEnrolledInCourse(StudentAlreadyEnrolledInCourseException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }
}
