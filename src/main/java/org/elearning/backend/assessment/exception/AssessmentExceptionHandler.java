package org.elearning.backend.assessment.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
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


}