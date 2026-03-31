package org.elearning.backend.content.exception;

import org.elearning.backend.common.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "org.elearning.backend.content")
public class ContentExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(CourseNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChapterNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleChapterNotFound(ChapterNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLessonNotFound(LessonNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LessonResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLessonResourceNotFound(LessonResourceNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidOrderIndexException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrderIndex(InvalidOrderIndexException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidResourceDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidResourceData(InvalidResourceDataException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }
}