package org.elearning.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * deci serviceul arunca exceptii
 * si aici le prind si le decid statusul si bodyul
 */

@RestControllerAdvice
public class GlobalExceptionHandler
{

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<MyErrorBody> handleBadRequestException(BadRequestException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        MyErrorBody errorBody = new MyErrorBody(
                status.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(errorBody, status);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<MyErrorBody> handleDuplicateResource(DuplicateResourceException ex){

        HttpStatus status = HttpStatus.CONFLICT;
        MyErrorBody body=new MyErrorBody(
                status.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(InvalidCredentials.class)
    public ResponseEntity<MyErrorBody> handleInvalidCredentials(InvalidCredentials ex){

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        MyErrorBody body=new MyErrorBody(
                status.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MyErrorBody> handleResourceNotFound(ResourceNotFoundException ex){

        HttpStatus status = HttpStatus.NOT_FOUND;
        MyErrorBody body=new MyErrorBody(
                status.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MyErrorBody> handleDefaultException(Exception ex){

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        MyErrorBody body=new MyErrorBody(
                status.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MyErrorBody> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        MyErrorBody body = new MyErrorBody(status.value(), message);

        return new ResponseEntity<>(body, status);
    }
}