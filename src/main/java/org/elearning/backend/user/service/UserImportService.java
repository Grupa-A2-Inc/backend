package org.elearning.backend.user.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class UserImportService {

    private final UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserImportResult tryCreateSingleUser(CreateUserRequest request) {
        try {
            UserResponse createdUser = userService.createUser(request);
            return UserImportResult.succeeded(createdUser);
        } catch (Exception exception) {
            return UserImportResult.failed(request.getEmail(), exception.getMessage());
        }
    }

    public BulkImportResponse importUsers(CreateUserBulkRequest request) {
        List<UserImportResult> results = request.getUsers().stream()
                .map(this::tryCreateSingleUser)
                .toList();

        return new BulkImportResponse(results);
    }
}