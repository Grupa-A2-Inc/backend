package org.elearning.backend.user.controller;

import lombok.AllArgsConstructor;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.response.UserResponse;
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

    @PreAuthorize("@accessService.canCreateUser(authentication, #request)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@P("request")@RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("@accessService.canViewUser(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@P("id") @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("@accessService.canEditUser(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@P("id") @PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PreAuthorize("@accessService.canDeleteUser(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@P("id") @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
