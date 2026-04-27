package org.elearning.backend.user.dto;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BulkImportResponseTest {

    private UserImportResult success(String email) {
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(), email, "Ion", "Pop",
                RoleName.STUDENT, null, UserStatus.ACTIVE
        );
        return UserImportResult.succeeded(userResponse);
    }

    private UserImportResult failure(String email) {
        return UserImportResult.failed(email, "Email already exists: " + email);
    }

    @Test
    void constructor_allSucceeded_setsCountsCorrectly() {
        List<UserImportResult> results = List.of(success("a@x.ro"), success("b@x.ro"));

        BulkImportResponse response = new BulkImportResponse(results);

        assertEquals(2, response.getTotal());
        assertEquals(2, response.getSucceeded());
        assertEquals(0, response.getFailed());
        assertEquals(results, response.getResults());
    }

    @Test
    void constructor_allFailed_setsCountsCorrectly() {
        List<UserImportResult> results = List.of(failure("a@x.ro"), failure("b@x.ro"));

        BulkImportResponse response = new BulkImportResponse(results);

        assertEquals(2, response.getTotal());
        assertEquals(0, response.getSucceeded());
        assertEquals(2, response.getFailed());
    }

    @Test
    void constructor_mixedResults_setsCountsCorrectly() {
        List<UserImportResult> results = List.of(
                success("a@x.ro"),
                failure("b@x.ro"),
                success("c@x.ro")
        );

        BulkImportResponse response = new BulkImportResponse(results);

        assertEquals(3, response.getTotal());
        assertEquals(2, response.getSucceeded());
        assertEquals(1, response.getFailed());
    }
}