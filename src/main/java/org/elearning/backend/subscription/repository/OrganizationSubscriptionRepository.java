package org.elearning.backend.subscription.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, UUID> {
    @Override
    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    Optional<OrganizationSubscription> findById(UUID id);

    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    List<OrganizationSubscription> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    Optional<OrganizationSubscription> findByProviderSubscriptionId(String providerSubscriptionId);

    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    List<OrganizationSubscription> findAllByStatusOrderByCreatedAtDesc(OrganizationSubscriptionStatus status);

    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    Optional<OrganizationSubscription> findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(
            UUID organizationId,
            Collection<OrganizationSubscriptionStatus> statuses
    );

    @EntityGraph(attributePaths = {"organization", "subscriptionPlan"})
    Optional<OrganizationSubscription> findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(UUID organizationId);
}
