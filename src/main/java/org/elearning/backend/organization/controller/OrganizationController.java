package org.elearning.backend.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.service.OrganizationService;
import org.elearning.backend.subscription.dto.request.CheckoutRequest;
import org.elearning.backend.subscription.dto.request.UpdateSubscriptionPlanRequest;
import org.elearning.backend.subscription.dto.response.CheckoutSessionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionStatusResponse;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.elearning.backend.subscription.service.StripeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.UUID;

@AllArgsConstructor
@RestController
@Tag(name = "Organizations", description = "Endpoints for managing organizations")
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private static final String OK = "200";
    private static final String CREATED = "201";
    private static final String NO_CONTENT = "204";
    private static final String BAD_REQUEST = "400";
    private static final String UNAUTHORIZED = "401";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final OrganizationService organizationService;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final StripeService stripeService;

    @Operation(
            summary = "Create a new organization",
            description = "Creates a new organization"
    )
    @ApiResponse(
            responseCode = CREATED,
            description = "Organization created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationResponse.class)
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(@RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request));
    }

    @Operation(
            summary = "Get all organizations",
            description = """
                    Returns the list of all organizations.

                    Query parameters:
                    - `page` — zero-based page index; accepts integers `0` or greater; defaults to `0` when omitted or negative
                    - `size` — number of items per page; accepts positive integers; defaults to `10` when omitted or invalid
                    - `search` — optional case-insensitive text filter used to match organization names; accepts any text value
                    - `sortBy` — field used for sorting; accepted values are `name` and `createdAt`
                    - `sortDir` — sort direction; accepted values are `asc` and `desc`
                    """
    )
    @ApiResponse(
            responseCode = OK,
            description = "Organizations retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = OrganizationResponse.class))
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
            responseCode = OK,
            description = "Organization retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationResponse.class)
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
            description = "Organization not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canViewOrganization(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@P("id") @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @Operation(
            summary = "Get current organization subscription",
            description = "Returns the current subscription status and plan limits for the specified organization"
    )
    @ApiResponse(
            responseCode = OK,
            description = "Organization subscription retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationSubscriptionStatusResponse.class)
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
            description = "Organization or subscription not found",
            content = @Content
    )
    @PreAuthorize("@accessService.canViewOrganization(authentication, #organizationId)")
    @GetMapping("/{organizationId}/subscription")
    public ResponseEntity<OrganizationSubscriptionStatusResponse> getOrganizationSubscription(
            @P("organizationId") @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId));
    }

    @Operation(
            summary = "Update organization",
            description = "Updates the organization identified by the given UUID"
    )
    @ApiResponse(
            responseCode = NO_CONTENT,
            description = "Organization updated successfully",
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
            responseCode = NO_CONTENT,
            description = "Organization deleted successfully",
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
            description = "Organization not found",
            content = @Content
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create checkout session",
            description = "Creates a Stripe checkout session for subscription activation"
    )
    @ApiResponse(
            responseCode = OK,
            description = "Checkout session created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CheckoutSessionResponse.class)
            )
    )
    @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = FORBIDDEN, description = "Access denied", content = @Content)
    @ApiResponse(responseCode = NOT_FOUND, description = "Organization or plan not found", content = @Content)
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    @PostMapping("/{organizationId}/subscription/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody CheckoutRequest request) {

        return ResponseEntity.ok(stripeService.createCheckoutSession(organizationId, request));
    }

    @Operation(
            summary = "Change subscription plan",
            description = "Changes the active subscription plan for the specified organization"
    )
    @ApiResponse(
            responseCode = OK,
            description = "Subscription plan changed successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrganizationSubscriptionResponse.class)
            )
    )
    @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = FORBIDDEN, description = "Access denied", content = @Content)
    @ApiResponse(responseCode = NOT_FOUND, description = "Organization or subscription not found", content = @Content)
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    @PatchMapping("/{organizationId}/subscription")
    public ResponseEntity<OrganizationSubscriptionResponse> changeSubscriptionPlan(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request) {

        return ResponseEntity.ok(organizationSubscriptionService.changePlan(organizationId, request));
    }
}
