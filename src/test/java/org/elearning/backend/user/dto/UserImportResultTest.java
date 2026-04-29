package org.elearning.backend.user.dto;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ActiveProfiles("test")
class UserImportResultTest {

    private UserResponse buildUserResponse(String email) {
        return new UserResponse(
                UUID.randomUUID(),
                email,
                "Ion",
                "Pop",
                RoleName.STUDENT,
                null,
                UserStatus.ACTIVE
        );
    }

    @Test
    void succeeded_setsSuccessTrueAndPopulatesUser() {
        UserResponse userResponse = buildUserResponse("ion@scoala.ro");

        UserImportResult result = UserImportResult.succeeded(userResponse);

        assertTrue(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertEquals(userResponse, result.getUser());
        assertNull(result.getErrorMessage());
    }

    @Test
    void failed_setsSuccessFalseAndPopulatesErrorMessage() {
        UserImportResult result = UserImportResult.failed("ion@scoala.ro", "Email already exists");

        assertFalse(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertNull(result.getUser());
        assertEquals("Email already exists", result.getErrorMessage());
    }
}
