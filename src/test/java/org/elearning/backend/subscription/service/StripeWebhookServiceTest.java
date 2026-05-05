package org.elearning.backend.subscription.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.elearning.backend.subscription.config.StripeConfig;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.exception.StripeWebhookVerificationException;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private StripeClientWrapper stripeClientWrapper;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    // ---- verifyAndParseWebhook ----

    @Test
    void verifyAndParseWebhook_success() throws SignatureVerificationException {
        String payload = "payload";
        String sigHeader = "sig";
        String secret = "whsec_test";
        Event mockEvent = mock(Event.class);

        when(stripeConfig.getWebhookSecret()).thenReturn(secret);
        when(stripeClientWrapper.constructWebhookEvent(payload, sigHeader, secret)).thenReturn(mockEvent);

        Event result = stripeWebhookService.verifyAndParseWebhook(payload, sigHeader);

        assertThat(result).isEqualTo(mockEvent);
    }

    @Test
    void verifyAndParseWebhook_invalidSignature_throwsException() throws SignatureVerificationException {
        String payload = "payload";
        String sigHeader = "invalid_sig";
        String secret = "whsec_test";

        when(stripeConfig.getWebhookSecret()).thenReturn(secret);
        when(stripeClientWrapper.constructWebhookEvent(payload, sigHeader, secret))
                .thenThrow(mock(SignatureVerificationException.class));

        assertThatThrownBy(() -> stripeWebhookService.verifyAndParseWebhook(payload, sigHeader))
                .isInstanceOf(StripeWebhookVerificationException.class)
                .hasMessageContaining("Invalid Stripe webhook signature");
    }

    // ---- handleEvent ----

    @Test
    void handleEvent_checkoutSessionCompleted_callsHandleCheckoutCompleted() {
        UUID organizationId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(planId);

        Session session = mock(Session.class);
        when(session.getMetadata()).thenReturn(Map.of(
                "organizationId", organizationId.toString(),
                "planId", planId.toString()
        ));
        when(session.getCustomer()).thenReturn("cus_123");
        when(session.getSubscription()).thenReturn("sub_123");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(organizationSubscriptionRepository.findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId))
                .thenReturn(Optional.empty());

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(any(OrganizationSubscription.class));
    }

    @Test
    void handleEvent_checkoutSessionCompleted_throwsWhenPlanIsMissing() {
        UUID organizationId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Session session = mock(Session.class);
        when(session.getMetadata()).thenReturn(Map.of(
                "organizationId", organizationId.toString(),
                "planId", planId.toString()
        ));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stripeWebhookService.handleEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(planId.toString());
    }

    @Test
    void handleEvent_subscriptionUpdated_updatesStatus() {
        String providerSubId = "sub_123";

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setProviderSubscriptionId(providerSubId);

        com.stripe.model.SubscriptionItem item = mock(com.stripe.model.SubscriptionItem.class);
        when(item.getCurrentPeriodStart()).thenReturn(1000L);
        when(item.getCurrentPeriodEnd()).thenReturn(2000L);

        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(items.getData()).thenReturn(List.of(item));

        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(providerSubId);
        when(stripeSubscription.getStatus()).thenReturn("active");
        when(stripeSubscription.getItems()).thenReturn(items);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(organizationSubscriptionRepository.findByProviderSubscriptionId(providerSubId))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        ArgumentCaptor<OrganizationSubscription> captor = ArgumentCaptor.forClass(OrganizationSubscription.class);
        verify(organizationSubscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationSubscriptionStatus.ACTIVE);
    }

    @Test
    void handleEvent_subscriptionUpdated_mapsTrialingStatus() {
        OrganizationSubscription subscription = buildSubscription("sub_trial");
        Event event = buildSubscriptionUpdatedEvent("sub_trial", "trialing", 1000L, 2000L);

        when(organizationSubscriptionRepository.findByProviderSubscriptionId("sub_trial"))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(subscription);
        assertThat(subscription.getStatus()).isEqualTo(OrganizationSubscriptionStatus.TRIALING);
    }

    @Test
    void handleEvent_subscriptionUpdated_mapsPastDueWithoutItems() {
        OrganizationSubscription subscription = buildSubscription("sub_past_due");
        LocalDateTime originalStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime originalEnd = LocalDateTime.of(2026, 2, 1, 0, 0);
        subscription.setCurrentPeriodStart(originalStart);
        subscription.setCurrentPeriodEnd(originalEnd);
        Event event = buildSubscriptionUpdatedEventWithoutItems("sub_past_due", "past_due");

        when(organizationSubscriptionRepository.findByProviderSubscriptionId("sub_past_due"))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(subscription);
        assertThat(subscription.getStatus()).isEqualTo(OrganizationSubscriptionStatus.PAST_DUE);
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(originalStart);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(originalEnd);
    }

    @Test
    void handleEvent_subscriptionUpdated_mapsCanceledStatusWithEmptyItems() {
        OrganizationSubscription subscription = buildSubscription("sub_canceled");
        Event event = buildSubscriptionUpdatedEventWithEmptyItems("sub_canceled", "canceled");

        when(organizationSubscriptionRepository.findByProviderSubscriptionId("sub_canceled"))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(subscription);
        assertThat(subscription.getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
    }

    @Test
    void handleEvent_subscriptionUpdated_defaultsUnknownStatusToCanceled() {
        OrganizationSubscription subscription = buildSubscription("sub_unknown");
        Event event = buildSubscriptionUpdatedEventWithoutItems("sub_unknown", "unknown_status");

        when(organizationSubscriptionRepository.findByProviderSubscriptionId("sub_unknown"))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(subscription);
        assertThat(subscription.getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
    }

    @Test
    void handleEvent_subscriptionUpdated_skipsPeriodUpdateWhenItemsDataIsNull() {
        OrganizationSubscription subscription = buildSubscription("sub_null_items");
        LocalDateTime originalStart = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime originalEnd = LocalDateTime.of(2026, 4, 1, 0, 0);
        subscription.setCurrentPeriodStart(originalStart);
        subscription.setCurrentPeriodEnd(originalEnd);
        Event event = buildSubscriptionUpdatedEventWithNullData("sub_null_items", "active");

        when(organizationSubscriptionRepository.findByProviderSubscriptionId("sub_null_items"))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository).save(subscription);
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(originalStart);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(originalEnd);
    }

    @Test
    void handleEvent_subscriptionDeleted_setsCanceled() {
        String providerSubId = "sub_123";

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setProviderSubscriptionId(providerSubId);

        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(providerSubId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(organizationSubscriptionRepository.findByProviderSubscriptionId(providerSubId))
                .thenReturn(Optional.of(subscription));

        stripeWebhookService.handleEvent(event);

        ArgumentCaptor<OrganizationSubscription> captor = ArgumentCaptor.forClass(OrganizationSubscription.class);
        verify(organizationSubscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
    }

    @Test
    void handleEvent_unknownEventType_doesNothing() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("unknown.event");

        stripeWebhookService.handleEvent(event);

        verify(organizationSubscriptionRepository, never()).save(any());
    }

    private OrganizationSubscription buildSubscription(String providerSubscriptionId) {
        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setProviderSubscriptionId(providerSubscriptionId);
        return subscription;
    }

    private Event buildSubscriptionUpdatedEvent(String subscriptionId, String status, Long start, Long end) {
        com.stripe.model.SubscriptionItem item = mock(com.stripe.model.SubscriptionItem.class);
        when(item.getCurrentPeriodStart()).thenReturn(start);
        when(item.getCurrentPeriodEnd()).thenReturn(end);

        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(items.getData()).thenReturn(List.of(item));

        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(subscriptionId);
        when(stripeSubscription.getStatus()).thenReturn(status);
        when(stripeSubscription.getItems()).thenReturn(items);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Event buildSubscriptionUpdatedEventWithoutItems(String subscriptionId, String status) {
        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(subscriptionId);
        when(stripeSubscription.getStatus()).thenReturn(status);
        when(stripeSubscription.getItems()).thenReturn(null);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Event buildSubscriptionUpdatedEventWithEmptyItems(String subscriptionId, String status) {
        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(items.getData()).thenReturn(List.of());

        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(subscriptionId);
        when(stripeSubscription.getStatus()).thenReturn(status);
        when(stripeSubscription.getItems()).thenReturn(items);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Event buildSubscriptionUpdatedEventWithNullData(String subscriptionId, String status) {
        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(items.getData()).thenReturn(null);

        Subscription stripeSubscription = mock(Subscription.class);
        when(stripeSubscription.getId()).thenReturn(subscriptionId);
        when(stripeSubscription.getStatus()).thenReturn(status);
        when(stripeSubscription.getItems()).thenReturn(items);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }
}
