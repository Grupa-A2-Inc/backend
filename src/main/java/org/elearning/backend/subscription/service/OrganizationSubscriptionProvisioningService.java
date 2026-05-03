package org.elearning.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.entity.SubscriptionProvider;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationSubscriptionProvisioningService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;

    public void provisionFreeSubscription(Organization organization) {
        SubscriptionPlan freePlan = subscriptionPlanRepository.findByCodeIgnoreCase("FREE")
                .orElseThrow(() -> new IllegalStateException("Default subscription plan FREE not found"));

        LocalDateTime now = LocalDateTime.now();

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setOrganization(organization);
        subscription.setSubscriptionPlan(freePlan);
        subscription.setStatus(OrganizationSubscriptionStatus.ACTIVE);
        subscription.setProvider(SubscriptionProvider.INTERNAL);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusYears(100));

        organizationSubscriptionRepository.save(subscription);
    }
}
