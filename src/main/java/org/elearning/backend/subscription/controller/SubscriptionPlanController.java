package org.elearning.backend.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "Endpoints for listing available subscription plans")
public class SubscriptionPlanController {
    private static final String OK = "200";

    private final OrganizationSubscriptionService organizationSubscriptionService;

    @Operation(
            summary = "List subscription plans",
            description = "Returns all available subscription plans ordered by display name"
    )
    @ApiResponse(
            responseCode = OK,
            description = "Subscription plans retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SubscriptionPlanResponse.class))
            )
    )
    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getSubscriptionPlans() {
        return ResponseEntity.ok(organizationSubscriptionService.getAllSubscriptionPlans());
    }
}
