package org.elearning.backend.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@Tag(name = "Organizations", description = "Endpoints for managing organizations")
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(
            summary = "Create a new organization",
            description = "Creates a new organization"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Organization created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationResponse.class)
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(@RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request));
    }

    @Operation(
            summary = "Get all organizations",
            description = "Returns the list of all organizations"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Organizations retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = OrganizationResponse.class))
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
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrganizationResponse>> getAllOrganizations(@RequestParam(required = false) Integer page,
                                                                                       @RequestParam(required = false) Integer size,
                                                                                       @RequestParam(required = false) String search,
                                                                                       @RequestParam(required = false) String sortBy,
                                                                                       @RequestParam(required = false) String sortDir) {
        return ResponseEntity.ok(
                organizationService.getAllOrganizationsPaginated(page, size, search, sortBy, sortDir)
        );
    }

    @Operation(
            summary = "Get organization by id",
            description = "Returns a single organization identified by its UUID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Organization retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationResponse.class)
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
            description = "Organization not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canViewOrganization(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@P("id") @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @Operation(
            summary = "Update organization",
            description = "Updates the organization identified by the given UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Organization updated successfully",
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
            description = "Organization not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canEditOrganization(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOrganization(@P("id") @PathVariable UUID id,
                                                   @RequestBody UpdateOrganizationRequest request) {
        organizationService.updateOrganization(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Delete organization",
            description = "Deletes the organization identified by the given UUID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Organization deleted successfully",
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
            description = "Organization not found",
            content = @Content
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}