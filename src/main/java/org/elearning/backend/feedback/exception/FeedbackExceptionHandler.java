package org.elearning.backend.feedback.exception;

import jakarta.validation.ConstraintViolationException;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.feedback")
public class FeedbackExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(AiApiException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }
}
