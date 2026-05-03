package org.elearning.backend.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationSubscriptionStatusResponse {
    private UUID organizationId;
    private OrganizationSubscriptionStatus status;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private SubscriptionPlanResponse plan;
}
