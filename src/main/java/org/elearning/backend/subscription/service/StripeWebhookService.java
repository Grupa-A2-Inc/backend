package org.elearning.backend.subscription.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.subscription.config.StripeConfig;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.entity.SubscriptionProvider;
import org.elearning.backend.subscription.exception.StripeWebhookVerificationException;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeConfig stripeConfig;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StripeClientWrapper stripeClientWrapper;

    public Event verifyAndParseWebhook(String payload, String sigHeader) {
        try {
            return stripeClientWrapper.constructWebhookEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new StripeWebhookVerificationException("Invalid Stripe webhook signature", e);
        }
    }

    @Transactional
    public void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Ignoring unsupported Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session)
                event.getDataObjectDeserializer().getObject().orElseThrow();

        UUID organizationId = UUID.fromString(session.getMetadata().get("organizationId"));
        UUID planId = UUID.fromString(session.getMetadata().get("planId"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        OrganizationSubscription subscription = organizationSubscriptionRepository
                .findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId)
                .orElse(new OrganizationSubscription());

        subscription.setSubscriptionPlan(plan);
        subscription.setStatus(OrganizationSubscriptionStatus.ACTIVE);
        subscription.setProvider(SubscriptionProvider.STRIPE);
        subscription.setProviderCustomerId(session.getCustomer());
        subscription.setProviderSubscriptionId(session.getSubscription());
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));

        organizationSubscriptionRepository.save(subscription);
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription stripeSubscription = (Subscription)
                event.getDataObjectDeserializer().getObject().orElseThrow();

        organizationSubscriptionRepository
                .findByProviderSubscriptionId(stripeSubscription.getId())
                .ifPresent(subscription -> {
                    subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
                    if (stripeSubscription.getItems() != null && stripeSubscription.getItems().getData() != null
                            && !stripeSubscription.getItems().getData().isEmpty()) {
                        var item = stripeSubscription.getItems().getData().get(0);
                        subscription.setCurrentPeriodStart(toLocalDateTime(item.getCurrentPeriodStart()));
                        subscription.setCurrentPeriodEnd(toLocalDateTime(item.getCurrentPeriodEnd()));
                    }
                    organizationSubscriptionRepository.save(subscription);
                });
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription stripeSubscription = (Subscription)
                event.getDataObjectDeserializer().getObject().orElseThrow();

        organizationSubscriptionRepository
                .findByProviderSubscriptionId(stripeSubscription.getId())
                .ifPresent(subscription -> {
                    subscription.setStatus(OrganizationSubscriptionStatus.CANCELED);
                    organizationSubscriptionRepository.save(subscription);
                });
    }

    private OrganizationSubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> OrganizationSubscriptionStatus.ACTIVE;
            case "trialing" -> OrganizationSubscriptionStatus.TRIALING;
            case "past_due" -> OrganizationSubscriptionStatus.PAST_DUE;
            case "canceled" -> OrganizationSubscriptionStatus.CANCELED;
            default -> OrganizationSubscriptionStatus.CANCELED;
        };
    }

    private LocalDateTime toLocalDateTime(Long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }
}
