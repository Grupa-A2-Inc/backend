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

    /**
     * Handle attempts to publish an assessment that is already published.
     *
     * @param exception the exception indicating the assessment has already been published
     * @return a ResponseEntity containing an error payload and HTTP status 409 (Conflict)
     */
    @ExceptionHandler(AlreadyPublishedException.class)
    public ResponseEntity<Map<String, Object>> handlePublished(AlreadyPublishedException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    /**
     * Handles a TestMustBeDraftException by producing an HTTP 409 Conflict error response.
     *
     * @param exception the exception indicating the target test must be in draft state
     * @return a ResponseEntity with an error body derived from the exception and HTTP status 409 (Conflict)
     */
    @ExceptionHandler(TestMustBeDraftException.class)
    public ResponseEntity<Map<String, Object>> handlePublished(TestMustBeDraftException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TestVersionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleVersionConflict(TestVersionConflictException exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }

    /**
     * Handles a timer-expired condition for an attempt and maps it to an HTTP 410 Gone response.
     *
     * @param exception the TimerExpiredException indicating the attempt's timer has expired
     * @return a ResponseEntity containing an error payload and HTTP status 410 (Gone)
     */
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
