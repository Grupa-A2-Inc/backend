package org.elearning.backend.assessment.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.assessment")
public class AssessmentExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException exception) {
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleTestDoesNotExist(DoesNotExistException exception){
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TestNotPublishedException.class)
    public ResponseEntity<Map<String, Object>> handleNotPublished(TestNotPublishedException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TestCannotBePublished.class)
    public ResponseEntity<Map<String, Object>> handleNotPublished(TestCannotBePublished exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AttemptInProgressException.class)
    public ResponseEntity<Map<String, Object>> handleInProgress(AttemptInProgressException exception){
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AttemptAlreadySubmittedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadySubmitted(AttemptAlreadySubmittedException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LessonAlreadyHasTestException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyHasTest(LessonAlreadyHasTestException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AlreadyPublishedException.class)
    public ResponseEntity<Map<String, Object>> handlePublished(AlreadyPublishedException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TimerExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTimerExpired(TimerExpiredException exception) {
        return buildErrorResponse(exception, HttpStatus.GONE);
    }

    @ExceptionHandler(InvalidAttemptUserException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAttemptUser(InvalidAttemptUserException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserHasNoPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleNoPermission(UserHasNoPermissionException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    /*
    Makes sure to catch invalid data inside a DTO object and throw a BAD_REQUEST status code instead of 500
     */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body("Invalid request body: " + ex.getMessage());
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(jakarta.validation.ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(jakarta.validation.ValidationException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

}