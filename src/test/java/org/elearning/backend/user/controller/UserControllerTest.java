package org.elearning.backend.user.controller;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void createUser_returns201Created() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ana@example.com")
                .password("parola123")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        UserResponse responseBody = new UserResponse(
                UUID.randomUUID(),
                "ana@example.com",
                "Ana",
                "Pop",
                RoleName.STUDENT,
                UserStatus.ACTIVE
        );

        when(userService.createUser(request)).thenReturn(responseBody);

        ResponseEntity<UserResponse> response = userController.createUser(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("ana@example.com", response.getBody().getEmail());
    }

    @Test
    void getUserById_returns200Ok() {
        UUID id = UUID.randomUUID();

        UserResponse responseBody = new UserResponse(
                id,
                "ana@example.com",
                "Ana",
                "Pop",
                RoleName.STUDENT,
                UserStatus.ACTIVE
        );

        when(userService.getUserById(id)).thenReturn(responseBody);

        ResponseEntity<UserResponse> response = userController.getUserById(id);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void deleteUser_returns204NoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = userController.deleteUser(id);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
    }
}