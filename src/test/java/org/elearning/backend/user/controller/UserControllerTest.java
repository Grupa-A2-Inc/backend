package org.elearning.backend.user.controller;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
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
                null,
                UserStatus.ACTIVE
        );

        when(userService.createUser(request)).thenReturn(responseBody);

        ResponseEntity<UserResponse> response = userController.createUser(request);

        verify(userService).createUser(request);
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
                null,
                UserStatus.ACTIVE
        );

        when(userService.getUserById(id)).thenReturn(responseBody);

        ResponseEntity<UserResponse> response = userController.getUserById(id);

        verify(userService).getUserById(id);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void getAllUsers_returns200Ok() {
        List<UserResponse> responseBody = List.of(
                new UserResponse(
                        UUID.randomUUID(),
                        "ana@example.com",
                        "Ana",
                        "Pop",
                        RoleName.STUDENT,
                        null,
                        UserStatus.ACTIVE
                )
        );

        when(userService.getAllUsers()).thenReturn(responseBody);

        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        verify(userService).getAllUsers();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateUser_returns204NoContent() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("ana.updated@example.com")
                .firstName("Ana")
                .lastName("Updated")
                .build();

        ResponseEntity<Void> response = userController.updateUser(id, request);

        verify(userService).updateUser(id, request);
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void deleteUser_returns204NoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = userController.deleteUser(id);

        verify(userService).deleteUser(id);
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void changePassword_returns204NoContent() {
        UUID id = UUID.randomUUID();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        ResponseEntity<Void> response = userController.changePassword(id, request);

        verify(userService).changePassword(id, request);
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void importUsers_returns200Ok() {
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(), "ion@scoala.ro", "Ion", "Pop",
                RoleName.STUDENT, null, UserStatus.ACTIVE
        );
        List<UserImportResult> results = List.of(UserImportResult.succeeded(userResponse));
        BulkImportResponse bulkResponse = new BulkImportResponse(results);

        CreateUserBulkRequest request = new CreateUserBulkRequest(
                List.of(CreateUserRequest.builder()
                        .email("ion@scoala.ro").password("parola123")
                        .firstName("Ion").lastName("Pop").roleName(RoleName.STUDENT).build())
        );

        when(userService.importUsers(request)).thenReturn(bulkResponse);

        ResponseEntity<BulkImportResponse> response = userController.importUsers(request);

        verify(userService).importUsers(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotal());
        assertEquals(1, response.getBody().getSucceeded());
        assertEquals(0, response.getBody().getFailed());
    }
}
