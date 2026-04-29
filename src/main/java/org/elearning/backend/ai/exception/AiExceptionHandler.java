package org.elearning.backend.ai.exception;

import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.ai")
public class AiExceptionHandler extends GlobalExceptionHandler {
    @ExceptionHandler(AiApiException.class)
    public ResponseEntity<Map<String, Object>> handleAiApiException(AiApiException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(AiTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleAiTimeoutException(AiTimeoutException exception) {
        return buildErrorResponse(exception, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException exception){
        return buildErrorResponse(exception, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflict(ResourceConflictException exception){
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotExist(DoesNotExistException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(AdaptiveServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAdaptiveServiceUnavailable(AdaptiveServiceUnavailableException exception) {
        return buildErrorResponse(exception, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
