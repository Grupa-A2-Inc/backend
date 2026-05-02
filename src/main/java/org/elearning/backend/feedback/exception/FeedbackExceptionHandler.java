package org.elearning.backend.feedback.exception;


import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.feedback")
public class FeedbackExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(MethodArgumentNotValidException exception) {
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotExist(DoesNotExistException exception) {
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EnrolledInCourseException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotExist(EnrolledInCourseException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DoesNotOwnTheCourseException.class)
    public ResponseEntity<Map<String, Object>> handleDoesNotOwnTheCourse(DoesNotOwnTheCourseException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DifferentIdException.class)
    public ResponseEntity<Map<String, Object>> handleDifferentId(DifferentIdException exception) {
        return buildErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AlreadyResolved.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyResolved(AlreadyResolved exception) {
        return buildErrorResponse(exception, HttpStatus.CONFLICT);
    }
}
