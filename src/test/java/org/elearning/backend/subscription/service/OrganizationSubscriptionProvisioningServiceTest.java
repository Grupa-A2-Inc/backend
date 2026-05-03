package org.elearning.backend.subscription.service;

import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.entity.SubscriptionProvider;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSubscriptionProvisioningServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @InjectMocks
    private OrganizationSubscriptionProvisioningService organizationSubscriptionProvisioningService;

    @Test
    void provisionFreeSubscription_createsInternalActiveSubscription() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());

        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setId(UUID.randomUUID());
        freePlan.setCode("FREE");

        when(subscriptionPlanRepository.findByCodeIgnoreCase("FREE")).thenReturn(Optional.of(freePlan));

        organizationSubscriptionProvisioningService.provisionFreeSubscription(organization);

        ArgumentCaptor<OrganizationSubscription> captor = ArgumentCaptor.forClass(OrganizationSubscription.class);
        verify(organizationSubscriptionRepository).save(captor.capture());

        OrganizationSubscription subscription = captor.getValue();
        assertThat(subscription.getOrganization()).isEqualTo(organization);
        assertThat(subscription.getSubscriptionPlan()).isEqualTo(freePlan);
        assertThat(subscription.getStatus()).isEqualTo(OrganizationSubscriptionStatus.ACTIVE);
        assertThat(subscription.getProvider()).isEqualTo(SubscriptionProvider.INTERNAL);
        assertThat(subscription.getCurrentPeriodStart()).isNotNull();
        assertThat(subscription.getCurrentPeriodEnd()).isAfter(subscription.getCurrentPeriodStart());
    }

    @Test
    void provisionFreeSubscription_throwsWhenFreePlanMissing() {
        Organization organization = new Organization();
        when(subscriptionPlanRepository.findByCodeIgnoreCase("FREE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationSubscriptionProvisioningService.provisionFreeSubscription(organization))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Default subscription plan FREE not found");
    }
}
