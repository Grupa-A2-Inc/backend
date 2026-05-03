package org.elearning.backend.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionProvider;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationSubscriptionResponse {
    private UUID id;
    private UUID organizationId;
    private UUID subscriptionPlanId;
    private OrganizationSubscriptionStatus status;
    private SubscriptionProvider provider;
    private String providerCustomerId;
    private String providerSubscriptionId;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
