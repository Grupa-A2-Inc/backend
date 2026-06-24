package org.elearning.backend.reward.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RewardExceptionHandler {

    @ExceptionHandler(RewardBadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(RewardBadRequestException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(RewardConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(RewardConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(RewardNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RewardNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }
}
