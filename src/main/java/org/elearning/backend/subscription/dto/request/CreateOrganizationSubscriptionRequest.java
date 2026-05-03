package org.elearning.backend.subscription.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionProvider;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationSubscriptionRequest {
    private UUID organizationId;
    private UUID subscriptionPlanId;
    private OrganizationSubscriptionStatus status;
    private SubscriptionProvider provider;
    private String providerCustomerId;
    private String providerSubscriptionId;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
}
