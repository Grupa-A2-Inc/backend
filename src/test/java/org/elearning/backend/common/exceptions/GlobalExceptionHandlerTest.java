package org.elearning.backend.common.exceptions;

import org.elearning.backend.common.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequestException_returns400() {
        BadRequestException ex = new BadRequestException("Passwords do not match");

        ResponseEntity<MyErrorBody> response = handler.handleBadRequestException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Passwords do not match", response.getBody().getMessage());
    }

    @Test
    void handleDuplicateResource_returns409() {
        DuplicateResourceException ex =
                new DuplicateResourceException("Email already exists: ana@example.com");

        ResponseEntity<MyErrorBody> response = handler.handleDuplicateResource(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Email already exists: ana@example.com", response.getBody().getMessage());
    }

    @Test
    void handleInvalidCredentials_returns401() {
        InvalidCredentials ex = new InvalidCredentials("Invalid password");

        ResponseEntity<MyErrorBody> response = handler.handleInvalidCredentials(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Invalid password", response.getBody().getMessage());
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("User does not exist");

        ResponseEntity<MyErrorBody> response = handler.handleResourceNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("User does not exist", response.getBody().getMessage());
    }

    @Test
    void handleDefaultException_returns500() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<MyErrorBody> response = handler.handleDefaultException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Unexpected error", response.getBody().getMessage());
    }
}
