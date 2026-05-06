package org.elearning.backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UserPaginationRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.service.CsvImportService;
import org.elearning.backend.user.service.UserImportService;
import org.elearning.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final String OK = "200";
    private static final String CREATED = "201";
    private static final String NO_CONTENT = "204";
    private static final String BAD_REQUEST = "400";
    private static final String UNAUTHORIZED = "401";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final UserService userService;
    private final UserImportService userImportService;
    private final CsvImportService csvImportService;

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user account within a target organization. The distinction between ADMIN and ORGANIZATION_ADMIN matters here: " +
                    "a platform ADMIN has broader authority across the system, while an ORGANIZATION_ADMIN may create users only for the organization they administer. " +
                    "The request is evaluated against the target organization in the payload, so organization-scoped administrators cannot create users for another organization."
    )
    @ApiResponse(
            responseCode = CREATED,
            description = "User created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("@accessService.canCreateUser(authentication, #request)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@P("request") @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }//aici nu mi dau seama daca sa modific? pentru ca teoretic ramane valabil endpointul pentru care trebuia sa fac taskul

    @Operation(
            summary = "Bulk import users",
            description = "Creates multiple users in a single request. Uses partial success — " +
                    "each user is processed independently and the response contains a full report. The role distinction is especially important for imports: " +
                    "a platform ADMIN can work across organizations, while an ORGANIZATION_ADMIN may import only users that belong to the administrator's own organization. " +
                    "Mixed-organization imports are therefore not valid for organization-scoped administrators."
    )
    @ApiResponse(
            responseCode = OK,
            description = "Import processed — check 'results' for individual outcomes",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BulkImportResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Request body invalid",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
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
            summary = "Bulk import users from CSV",
            description = "Creates multiple users from a CSV file upload. The CSV must have headers: " +
                    "email,firstName,lastName,roleName. The organization is inferred from the authenticated user. " +
                    "Uses partial success — each row is processed independently."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Import processed — check 'results' for individual outcomes",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BulkImportResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid file or headers", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    @PostMapping(value = "/import/csv", consumes = "multipart/form-data")
    public ResponseEntity<BulkImportResponse> importUsersFromCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        BulkImportResponse response = csvImportService.importFromCsv(file, organizationId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all users",
            description = """
                    Returns the full platform-wide user list. This endpoint is reserved for the global ADMIN role and is intentionally not available to ORGANIZATION_ADMIN.
                    If you need an organization-scoped list instead of a cross-platform list, use the dedicated organization user endpoint.

                    Query parameters:
                    - `page` — zero-based page index; accepts integers `0` or greater; defaults to `0` when omitted or negative
                    - `size` — number of items per page; accepts positive integers; defaults to `10` when omitted or invalid
                    - `search` — case-insensitive text filter applied to first name, last name, and email; accepts any text value
                    - `role` — optional role filter; accepted values are `ADMIN`, `ORGANIZATION_ADMIN`, `TEACHER`, `STUDENT`, and `PARENT`
                    - `status` — optional lifecycle-status filter; accepted values are `ACTIVE`, `INACTIVE`, `BLOCKED`, and `PENDING`
                    - `sortBy` — field used for sorting; accepted values are `firstName`, `lastName`, `email`, and `createdAt`
                    - `sortDir` — sort direction; accepted values are `asc` and `desc`
                    """
    )
    @ApiResponse(
            responseCode = OK,
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
            )
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PaginatedResponse<UserResponse>> getAllUsers(@RequestParam(required = false) Integer page,
                                                                       @RequestParam(required = false) Integer size,
                                                                       @RequestParam(required = false) String search,
                                                                       @RequestParam(required = false) String role,
                                                                       @RequestParam(required = false) UserStatus status,
                                                                       @RequestParam(required = false) String sortBy,
                                                                       @RequestParam(required = false) String sortDir) {
        return ResponseEntity.ok(userService.getAllUsersPaginated(page, size, search, role, status, sortBy, sortDir));//aici schimb metoda in service
    }

    @Operation(
            summary = "Get users",
            description = """
                    Returns the users that belong to the authenticated organization administrator's organization. This endpoint is specifically organization-scoped.
                    Unlike the global user-list endpoint for ADMIN, this one does not expose users from other organizations and is intended for tenant-level administration only.

                    Query parameters:
                    - `page` — zero-based page index; accepts integers `0` or greater; defaults to `0` when omitted or negative
                    - `size` — number of items per page; accepts positive integers; defaults to `10` when omitted or invalid
                    - `search` — case-insensitive text filter applied to first name, last name, and email; accepts any text value
                    - `role` — optional role filter; accepted values are `ADMIN`, `ORGANIZATION_ADMIN`, `TEACHER`, `STUDENT`, and `PARENT`
                    - `status` — optional lifecycle-status filter; accepted values are `ACTIVE`, `INACTIVE`, `BLOCKED`, and `PENDING`
                    - `sortBy` — field used for sorting; accepted values are `firstName`, `lastName`, and `email`
                    - `sortDir` — sort direction; accepted values are `asc` and `desc`
                    """
    )
    @ApiResponse(
            responseCode = OK,
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
            )
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    @GetMapping("/organization")
    public ResponseEntity<PaginatedResponse<UserResponse>> getOrganizationUsers(@RequestParam(required = false) Integer page,
                                                                                @RequestParam(required = false) Integer size,
                                                                                @RequestParam(required = false) String search,
                                                                                @RequestParam(required = false) String role,
                                                                                @RequestParam(required = false) UserStatus status,
                                                                                @RequestParam(required = false) String sortBy,
                                                                                @RequestParam(required = false) String sortDir,
                                                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        UserPaginationRequest request = new UserPaginationRequest(page, size, search, role, status, sortBy, sortDir);
        return ResponseEntity.ok(userService.getCurrentOrganizationUsersPaginated(currentUser.getUserId(), request));
    }

    @Operation(
            summary = "Update user status",
            description = "Updates the status of the user identified by the given UUID. Access is evaluated against the target account rather than by role name alone. " +
                    "A platform ADMIN may update statuses broadly, while an ORGANIZATION_ADMIN may update only users from the same organization. " +
                    "This prevents organization-scoped administrators from changing the lifecycle state of users belonging to another tenant."
    )
    @ApiResponse(
            responseCode = NO_CONTENT,
            description = "User updated successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = NOT_FOUND,
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
            summary = "Get user by id",
            description = "Returns a single user identified by its UUID. The authorization rule distinguishes between global and organization-scoped administration. " +
                    "A platform ADMIN can view any user, while an ORGANIZATION_ADMIN can view only users that belong to the same organization. " +
                    "Some users may also be allowed to view their own record through the same access rule."
    )
    @ApiResponse(
            responseCode = OK,
            description = "User retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            )
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = NOT_FOUND,
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
            description = "Updates the user identified by the given UUID. This endpoint follows the same scope model used by user viewing: platform ADMIN has global reach, " +
                    "while ORGANIZATION_ADMIN is limited to users from the same organization. The operation is meant for administrative maintenance of account metadata and profile fields."
    )
    @ApiResponse(
            responseCode = NO_CONTENT,
            description = "User updated successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Bad request",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = NOT_FOUND,
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
            description = "Deletes the user identified by the given UUID. Because deletion has a wider operational impact, the documentation makes the role boundary explicit: " +
                    "ADMIN is platform-wide, while ORGANIZATION_ADMIN can delete only users inside the administrator's own organization. No organization-scoped administrator " +
                    "should expect to remove users from another tenant."
    )
    @ApiResponse(
            responseCode = NO_CONTENT,
            description = "User deleted successfully",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = NOT_FOUND,
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
            description = "Changes the password of the user identified by the given UUID. This endpoint is not the same as self-service password reset and is subject to access checks. " +
                    "A platform ADMIN may change passwords broadly, while ordinary users may generally change only their own password through the authorization rule that backs this endpoint. " +
                    "An ORGANIZATION_ADMIN does not automatically inherit unrestricted password control over every account in the system."
    )
    @ApiResponse(
            responseCode = NO_CONTENT,
            description = "Password change with success",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Unauthorized",
            content = @Content
    )
    @ApiResponse(
            responseCode = FORBIDDEN,
            description = "Access denied",
            content = @Content
    )
    @ApiResponse(
            responseCode = NOT_FOUND,
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

    @Operation(
            summary = "Export organization users as CSV",
            description = """
                    Exports the users that belong to the authenticated organization administrator's organization as a CSV file.
                    The export is organization-scoped and does not include users from other organizations.

                    Query parameters:
                    - `search` — optional case-insensitive text filter applied to first name, last name, and email before export; accepts any text value
                    - `role` — optional role filter applied before export; accepted values are `ADMIN`, `ORGANIZATION_ADMIN`, `TEACHER`, `STUDENT`, and `PARENT`
                    - `status` — optional lifecycle-status filter applied before export; accepted values are `ACTIVE`, `INACTIVE`, `BLOCKED`, and `PENDING`
                    """
    )
    @ApiResponse(responseCode = OK, description = "CSV generated successfully", content = @Content)
    @ApiResponse(responseCode = UNAUTHORIZED, description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = FORBIDDEN, description = "Access denied", content = @Content)
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    @GetMapping("/organization/export")
    public ResponseEntity<byte[]> exportOrganizationUsers(@RequestParam (required = false) String search,
                                                        @RequestParam (required = false) String role,
                                                        @RequestParam (required = false) UserStatus status,
                                                        @AuthenticationPrincipal CustomUserDetails currentUser){

        String csv = userService.exportOrganizationUsersCsv(search, role, status, currentUser.getUserId());

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=organization-users.csv")
                .header("Content-Type", "text/csv")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
