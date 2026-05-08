package org.elearning.backend.user.service;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.subscription.exception.UserLimitExceededException;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserImportServiceQuotaTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserImportService userImportService;

    private final UUID organizationId = UUID.randomUUID();

    @Test
    void importUsers_whenAllBelowQuota_allSucceed() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com"),
                buildRequest("u2@test.com"),
                buildRequest("u3@test.com")
        );

        for (int i = 0; i < requests.size(); i++) {
            CreateUserRequest req = requests.get(i);
            when(userService.createUser(req)).thenReturn(buildUserResponse(req.getEmail()));
        }

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getSucceeded()).isEqualTo(3);
        assertThat(response.getFailed()).isZero();
    }

    @Test
    void importUsers_whenQuotaExceededMidImport_partialSuccessWithFailedMarked() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com"),
                buildRequest("u2@test.com"),
                buildRequest("u3@test.com")
        );

        when(userService.createUser(requests.get(0))).thenReturn(buildUserResponse("u1@test.com"));
        when(userService.createUser(requests.get(1))).thenReturn(buildUserResponse("u2@test.com"));
        when(userService.createUser(requests.get(2)))
                .thenThrow(new UserLimitExceededException(organizationId, 2));

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getSucceeded()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(1);

        assertThat(response.getResults().get(2).isSuccess()).isFalse();
        assertThat(response.getResults().get(2).getEmail()).isEqualTo("u3@test.com");
        assertThat(response.getResults().get(2).getErrorMessage()).contains("2");
    }

    @Test
    void importUsers_whenAllExceedQuota_allFailed() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com"),
                buildRequest("u2@test.com")
        );

        requests.forEach(req ->
                when(userService.createUser(req))
                        .thenThrow(new UserLimitExceededException(organizationId, 0))
        );

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getSucceeded()).isZero();
          assertThat(response.getFailed()).isEqualTo(2);

        response.getResults().forEach(r -> {
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getErrorMessage()).isNotBlank();
        });
    }

    private CreateUserRequest buildRequest(String email) {
        return CreateUserRequest.builder()
                .email(email)
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .organizationId(organizationId)
                .build();
    }

    private UserResponse buildUserResponse(String email) {
        return new UserResponse(
                UUID.randomUUID(),
                email,
                "Ion",
                "Pop",
                RoleName.STUDENT,
                organizationId,
                UserStatus.PENDING
        );
    }
}