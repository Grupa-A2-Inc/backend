package org.elearning.backend.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception ex, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, status);
    }

    // Prindem erorile de tip 404
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleTestDoesNotExist(DoesNotExistException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    // Prindem testele nepublicate (400)
    @ExceptionHandler(TestNotPublishedException.class)
    public ResponseEntity<Map<String, Object>> handleNotPublished(TestNotPublishedException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TestCannotBePublished.class)
    public ResponseEntity<Map<String, Object>> handleNotPublished(TestCannotBePublished exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    // Prindem cerere Result cu Attempt-ul IN_PROGRESS (403)
    @ExceptionHandler(AttemptInProgressException.class)
    public ResponseEntity<Map<String, Object>> handleInProgress(AttemptInProgressException exception){
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    // Prindem Attempt-ul deja trimis (409)
    @ExceptionHandler(AttemptAlreadySubmittedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadySubmitted(AttemptAlreadySubmittedException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LessonAlreadyHasTestException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadySubmitted(LessonAlreadyHasTestException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    // Prindem Timer Expirat (410)
    @ExceptionHandler(TimerExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTimerExpired(TimerExpiredException ex) {
        return buildErrorResponse(ex, HttpStatus.GONE);
    }
}