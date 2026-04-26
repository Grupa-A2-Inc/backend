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

    /**
     * Handle access-denied errors for analytics endpoints and produce a 403 Forbidden response.
     *
     * @param exception the {@link WithoutAccessException} describing the access denial
     * @return a {@link ResponseEntity} whose body contains an error representation and whose status is 403 Forbidden
     */
    @ExceptionHandler(WithoutAccessException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(WithoutAccessException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle a missing-resource error by building an HTTP 404 Not Found response.
     *
     * @param exception the exception thrown when the requested resource does not exist
     * @return a ResponseEntity containing the error payload and HTTP status 404 (Not Found)
     */
    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotExist(DoesNotExistException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles ResourceConflictException by returning an HTTP 409 Conflict response.
     *
     * @param exception the ResourceConflictException that triggered the handler
     * @return a ResponseEntity containing an error payload map and HTTP status 409 (Conflict)
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflict(ResourceConflictException exception){
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    /**
     * Handles validation failures from analytics controllers by producing an error response.
     *
     * @param exception the validation exception containing details about the failed validation
     * @return a ResponseEntity with an error payload and HTTP status 422 (Unprocessable Entity)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException exception){
        return buildErrorResponse(exception, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles a StudentNotEnrolledInCourseException and produces an error response with HTTP 403 Forbidden.
     *
     * @param exception the exception indicating a student is not enrolled in the specified course
     * @return a ResponseEntity containing an error payload and HTTP status 403 Forbidden
     */
    @ExceptionHandler(StudentNotEnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(StudentNotEnrolledInCourseException exception){
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }
}
