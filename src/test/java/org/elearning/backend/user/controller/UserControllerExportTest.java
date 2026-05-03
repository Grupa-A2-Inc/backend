package org.elearning.backend.user.controller;

import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.UserImportService;
import org.elearning.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerExportTest {

    @Mock
    private UserService userService;

    @Mock
    private UserImportService userImportService;

    @InjectMocks
    private UserController userController;

    @Test
    void exportOrganizationUsers_shouldReturnCsvFileResponse() {
        UUID currentUserId = UUID.randomUUID();

        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(currentUserId);

        String csv = """
                id,email,firstName,lastName,role,status,organizationId
                "1","ana@test.com","Ana","Ionescu","STUDENT","ACTIVE","org-1"
                """;

        when(userService.exportOrganizationUsersCsv("ana", "STUDENT", UserStatus.ACTIVE, currentUserId))
                .thenReturn(csv);

        ResponseEntity<byte[]> response = userController.exportOrganizationUsers(
                "ana",
                "STUDENT",
                UserStatus.ACTIVE,
                currentUser
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(csv, new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("attachment; filename=organization-users.csv",
                response.getHeaders().getFirst("Content-Disposition"));
        assertEquals("text/csv",
                response.getHeaders().getFirst("Content-Type"));

        verify(userService).exportOrganizationUsersCsv("ana", "STUDENT", UserStatus.ACTIVE, currentUserId);
    }
}