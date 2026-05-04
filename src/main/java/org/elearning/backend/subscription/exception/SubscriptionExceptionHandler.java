package org.elearning.backend.subscription.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SubscriptionExceptionHandler {

    @ExceptionHandler(SubscriptionNotActiveException.class)
    public ResponseEntity<Map<String, String>> handleSubscriptionNotActive(SubscriptionNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleUserLimitExceeded(UserLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ClassroomLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleClassroomLimitExceeded(ClassroomLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}