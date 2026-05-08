package org.elearning.backend.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateSubscriptionPlanRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;
}