package org.elearning.backend.enrollment.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.enrollment")
public class EnrollmentExceptionHandler extends GlobalExceptionHandler {
    @ExceptionHandler(CourseIsPrivateException.class)
    public ResponseEntity<Map<String, Object>> handleCourseIsPrivate(CourseIsPrivateException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(StudentAlreadyEnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleStudentAlreadyEnrolledInCourse(StudentAlreadyEnrolledInCourseException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CourseEnrollmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseEnrollmentNotFound(CourseEnrollmentNotFoundException exception) {
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(CourseNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }
}
