package org.elearning.backend.analytics.exception;

import org.elearning.backend.ai.exception.ResourceConflictException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.common.GlobalExceptionHandler;
import org.elearning.backend.content.exception.CourseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.analytics")
public class AnalyticsExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(WithoutAccessException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(WithoutAccessException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotExist(DoesNotExistException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StudentNotEnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(StudentNotEnrolledInCourseException exception){
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(CourseNotFoundException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }
}
