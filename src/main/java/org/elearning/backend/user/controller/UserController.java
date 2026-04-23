package org.elearning.backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
<<<<<<< Updated upstream
=======
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
>>>>>>> Stashed changes
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.service.UserImportService;
import org.elearning.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserImportService userImportService;

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user for a specific organization if you are the admin of that specific organization"
    )
    @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("@accessService.canCreateUser(authentication, #request)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@P("request")@RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
<<<<<<< Updated upstream
=======
            summary = "Bulk import users",
            description = "Creates multiple users in a single request. Uses partial success — " +
                    "each user is processed independently and the response contains a full report."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Import processed — check 'results' for individual outcomes",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BulkImportResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Request body invalid",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("@accessService.canImportUsers(authentication, #request)")
    @PostMapping("/import")
    public ResponseEntity<BulkImportResponse> importUsers(@Valid @RequestBody CreateUserBulkRequest request) {
        BulkImportResponse response = userImportService.importUsers(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
>>>>>>> Stashed changes
            summary = "Get all users",
            description = "Returns the list of all users visible to administrators"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
<<<<<<< Updated upstream
=======
            summary = "Get users",
            description = "Returns the list of users that are part of the administrator's organization"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    @GetMapping("/organization")
    public ResponseEntity<List<UserResponse>> getOrganizationUsers() {
        return ResponseEntity.ok(userService.getCurrentOrganizationUsers());
    }

    @Operation(
            summary = "Update user status",
            description = "Updates the user's status identified by the given UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "User updated successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canUpdateUserStatus(authentication, #id)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(@P("id") @PathVariable UUID id,
                                                 @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateUserStatus(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
>>>>>>> Stashed changes
            summary = "Get user by id",
            description = "Returns a single user identified by its UUID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canViewUser(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@P("id") @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(
            summary = "Update user",
            description = "Updates the user identified by the given UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "User updated successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canEditUser(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@P("id") @PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes the user identified by the given UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "User deleted successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canDeleteUser(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@P("id") @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password of user that has the specified UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Password change with success",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canChangePassword(authentication, #id)")
    @PatchMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(@P("id") @PathVariable UUID id,
                                               @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}