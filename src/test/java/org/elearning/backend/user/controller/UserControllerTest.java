package org.elearning.backend.user.controller;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UserPaginationRequest;
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.UserImportService;
import org.elearning.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.elearning.backend.common.dto.response.PaginatedResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserImportService userImportService;

    @InjectMocks
    private UserController userController;

    @Test
    void createUser_returns201Created() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ana@example.com")
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
        PaginatedResponse<UserResponse> responseBody = new PaginatedResponse<>(
                List.of(
                        new UserResponse(
                                UUID.randomUUID(),
                                "ana@example.com",
                                "Ana",
                                "Pop",
                                RoleName.STUDENT,
                                null,
                                UserStatus.ACTIVE
                        )
                ),
                0,
                10,
                1L
        );

        when(userService.getAllUsersPaginated(0, 10, null, null, null, null, null))
                .thenReturn(responseBody);

        ResponseEntity<PaginatedResponse<UserResponse>> response =
                userController.getAllUsers(0, 10, null, null, null, null, null);

        verify(userService).getAllUsersPaginated(0, 10, null, null, null, null, null);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
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
    void updateUserStatus_returns204NoContent() {
        UUID id = UUID.randomUUID();
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.BLOCKED)
                .build();

        ResponseEntity<Void> response = userController.updateUserStatus(id, request);

        verify(userService).updateUserStatus(id, request);
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
                UUID.randomUUID(),
                "ion@scoala.ro",
                "Ion",
                "Pop",
                RoleName.STUDENT,
                null,
                UserStatus.ACTIVE
        );

        List<UserImportResult> results = List.of(UserImportResult.succeeded(userResponse));
        BulkImportResponse bulkResponse = new BulkImportResponse(results);

        CreateUserBulkRequest request = new CreateUserBulkRequest(
                List.of(CreateUserRequest.builder()
                        .email("ion@scoala.ro")
                        .firstName("Ion")
                        .lastName("Pop")
                        .roleName(RoleName.STUDENT)
                        .build())
        );

        when(userImportService.importUsers(request)).thenReturn(bulkResponse);

        ResponseEntity<BulkImportResponse> response = userController.importUsers(request);

        verify(userImportService).importUsers(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotal());
        assertEquals(1, response.getBody().getSucceeded());
        assertEquals(0, response.getBody().getFailed());
    }


    @Test
    void getOrganizationUsers_returns200Ok() {
        UUID currentUserId = UUID.randomUUID();

        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(currentUserId);

        PaginatedResponse<UserResponse> paginatedResponse = new PaginatedResponse<>(
                List.of(makeResponse("student@org.com"), makeResponse("teacher@org.com")),
                0,
                10,
                2L
        );

        when(userService.getCurrentOrganizationUsersPaginated(
                currentUserId, new UserPaginationRequest(0, 10, "org", "TEACHER", UserStatus.ACTIVE, "email", "desc")
        )).thenReturn(paginatedResponse);

        ResponseEntity<PaginatedResponse<UserResponse>> response =
                userController.getOrganizationUsers(0, 10, "org", "TEACHER", UserStatus.ACTIVE, "email", "desc", currentUser);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(paginatedResponse);
    }

    private UserResponse makeResponse(String email) {
        return new UserResponse(
                UUID.randomUUID(),
                email,
                "Ana",
                "Ionescu",
                RoleName.STUDENT,
                UUID.randomUUID(),
                UserStatus.ACTIVE
        );
    }
}
