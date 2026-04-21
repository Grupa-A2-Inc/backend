package org.elearning.backend.user.controller;

import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerAdminTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getAllUsers_shouldReturnOkWithAllUsers() {
        UserResponse user1 = new UserResponse(
                UUID.randomUUID(),
                "admin@test.com",
                "Admin",
                "One",
                null,
                null,
                UserStatus.ACTIVE
        );

        UserResponse user2 = new UserResponse(
                UUID.randomUUID(),
                "user@test.com",
                "User",
                "Two",
                null,
                null,
                UserStatus.ACTIVE
        );

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userService).getAllUsers();
    }

    @Test
    void getOrganizationUsers_shouldReturnOkWithOrganizationUsers() {
        UserResponse orgUser = new UserResponse(
                UUID.randomUUID(),
                "orgadmin@test.com",
                "Org",
                "Admin",
                null,
                UUID.randomUUID(),
                UserStatus.ACTIVE
        );

        when(userService.getCurrentOrganizationUsers()).thenReturn(List.of(orgUser));

        ResponseEntity<List<UserResponse>> response = userController.getOrganizationUsers();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("orgadmin@test.com", response.getBody().get(0).getEmail());

        verify(userService).getCurrentOrganizationUsers();
    }
}