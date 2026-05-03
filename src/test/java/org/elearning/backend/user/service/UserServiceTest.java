package org.elearning.backend.user.service;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserImportService userImportService;

    @Test
    void tryCreateSingleUser_success_returnsSucceededResult() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        UserResponse createdUser = new UserResponse(
                UUID.randomUUID(),
                "ion@scoala.ro",
                "Ion",
                "Pop",
                RoleName.STUDENT,
                null,
                UserStatus.PENDING
        );

        when(userService.createUser(request)).thenReturn(createdUser);

        UserImportResult result = userImportService.tryCreateSingleUser(request);

        assertTrue(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertNotNull(result.getUser());
        assertNull(result.getErrorMessage());
    }

    @Test
    void tryCreateSingleUser_duplicateEmail_returnsFailedResult() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        when(userService.createUser(request))
                .thenThrow(new RuntimeException("Email already exists: ion@scoala.ro"));

        UserImportResult result = userImportService.tryCreateSingleUser(request);

        assertFalse(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertNull(result.getUser());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void importUsers_allSuccess_returnsFullSuccessReport() {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("ana@scoala.ro")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(req1, req2));

        UserResponse user1 = new UserResponse(
                UUID.randomUUID(), "ion@scoala.ro", "Ion", "Pop",
                RoleName.TEACHER, null, UserStatus.PENDING
        );
        UserResponse user2 = new UserResponse(
                UUID.randomUUID(), "ana@scoala.ro", "Ana", "Pop",
                RoleName.TEACHER, null, UserStatus.PENDING
        );

        when(userService.createUser(req1)).thenReturn(user1);
        when(userService.createUser(req2)).thenReturn(user2);

        BulkImportResponse response = userImportService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(2, response.getSucceeded());
        assertEquals(0, response.getFailed());
        assertTrue(response.getResults().stream().allMatch(UserImportResult::isSuccess));
    }

    @Test
    void importUsers_partialSuccess_returnsCorrectReport() {
        CreateUserRequest validReq = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        CreateUserRequest duplicateReq = CreateUserRequest.builder()
                .email("duplicat@scoala.ro")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(validReq, duplicateReq));

        UserResponse user = new UserResponse(
                UUID.randomUUID(), "ion@scoala.ro", "Ion", "Pop",
                RoleName.TEACHER, null, UserStatus.PENDING
        );

        when(userService.createUser(validReq)).thenReturn(user);
        when(userService.createUser(duplicateReq))
                .thenThrow(new RuntimeException("Email already exists: duplicat@scoala.ro"));

        BulkImportResponse response = userImportService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(1, response.getSucceeded());
        assertEquals(1, response.getFailed());

        UserImportResult failedResult = response.getResults().stream()
                .filter(result -> !result.isSuccess())
                .findFirst()
                .orElseThrow();

        assertEquals("duplicat@scoala.ro", failedResult.getEmail());
        assertNotNull(failedResult.getErrorMessage());
    }

    @Test
    void importUsers_allFailed_returnsFullFailureReport() {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .build();

        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("ana@scoala.ro")
                .build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(req1, req2));

        when(userService.createUser(req1))
                .thenThrow(new RuntimeException("Email already exists: ion@scoala.ro"));
        when(userService.createUser(req2))
                .thenThrow(new RuntimeException("Email already exists: ana@scoala.ro"));

        BulkImportResponse response = userImportService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(0, response.getSucceeded());
        assertEquals(2, response.getFailed());
        assertTrue(response.getResults().stream().noneMatch(UserImportResult::isSuccess));
    }
}
