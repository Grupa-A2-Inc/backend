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
import org.elearning.backend.subscription.entity.SubscriptionProvider;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

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
                .isInstanceOf(RuntimeException.class)
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
}