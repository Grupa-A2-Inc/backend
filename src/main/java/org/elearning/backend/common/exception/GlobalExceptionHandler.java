package org.elearning.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * deci serviceul arunca exceptii
 * si aici le prind si le decid statusul si bodyul
 */

@RestControllerAdvice
public class GlobalExceptionHandler
{

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

}