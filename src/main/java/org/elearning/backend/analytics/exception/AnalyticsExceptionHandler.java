package org.elearning.backend.analytics.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.analytics")
public class AnalyticsExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AccessDeniedException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }
}
