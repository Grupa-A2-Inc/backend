package org.elearning.backend.user.controller;

import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.security.auth.CustomUserDetails;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserControllerAdminTest {

    @Mock
    private UserService userService;

    @Mock
    private UserImportService userImportService;

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

        PaginatedResponse<UserResponse> paginatedResponse =
                new PaginatedResponse<>(List.of(user1, user2), 0, 10, 2L);

        when(userService.getAllUsersPaginated(0, 10, null, null, null, null, null))
                .thenReturn(paginatedResponse);

        ResponseEntity<PaginatedResponse<UserResponse>> response =
                userController.getAllUsers(0, 10, null, null, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        verify(userService).getAllUsersPaginated(0, 10, null, null, null, null, null);
    }

    @Test
    void getOrganizationUsers_shouldReturnOkWithOrganizationUsers() {
        UUID currentUserId = UUID.randomUUID();

        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(currentUserId);

        UserResponse orgUser = new UserResponse(
                UUID.randomUUID(),
                "orgadmin@test.com",
                "Org",
                "Admin",
                null,
                UUID.randomUUID(),
                UserStatus.ACTIVE
        );

        PaginatedResponse<UserResponse> paginatedResponse =
                new PaginatedResponse<>(List.of(orgUser), 0, 10, 1L);

        when(userService.getCurrentOrganizationUsersPaginated(
                currentUserId, 0, 10, null, null, null, null, null
        )).thenReturn(paginatedResponse);

        ResponseEntity<PaginatedResponse<UserResponse>> response =
                userController.getOrganizationUsers(0, 10, null, null, null, null, null, currentUser);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("orgadmin@test.com", response.getBody().getContent().get(0).getEmail());

        verify(userService).getCurrentOrganizationUsersPaginated(
                currentUserId, 0, 10, null, null, null, null, null
        );
    }
}