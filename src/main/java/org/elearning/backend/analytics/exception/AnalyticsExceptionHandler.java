package org.elearning.backend.analytics.exception;

import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.common.GlobalExceptionHandler;
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

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflict(ResourceConflictException exception){
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException exception){
        return buildErrorResponse(exception, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(StudentNotEnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(StudentNotEnrolledInCourseException exception){
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }
}
