package org.elearning.backend.subscription.service;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.elearning.backend.subscription.config.StripeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeClientWrapperTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private StripeSdkClient stripeSdkClient;

    @InjectMocks
    private StripeClientWrapper stripeClientWrapper;

    @Test
    void createCheckoutSession_setsApiKeyAndDelegatesToStripe() throws Exception {
        SessionCreateParams params = SessionCreateParams.builder().build();
        Session session = new Session();

        when(stripeConfig.getSecretKey()).thenReturn("sk_test_123");
        when(stripeSdkClient.createSession(params)).thenReturn(session);

        Session result = stripeClientWrapper.createCheckoutSession(params);

        assertThat(result).isSameAs(session);
        assertThat(Stripe.apiKey).isEqualTo("sk_test_123");
    }

    @Test
    void constructWebhookEvent_delegatesToStripeWebhook() throws Exception {
        Event event = new Event();
        when(stripeSdkClient.createWebhookEvent("payload", "sig", "secret")).thenReturn(event);

        Event result = stripeClientWrapper.constructWebhookEvent("payload", "sig", "secret");

        assertThat(result).isSameAs(event);
    }

    @Test
    void createSession_delegatesToStripeSdkClient() throws Exception {
        SessionCreateParams params = SessionCreateParams.builder().build();
        Session session = new Session();

        when(stripeSdkClient.createSession(params)).thenReturn(session);

        Session result = stripeClientWrapper.createSession(params);

        assertThat(result).isSameAs(session);
        verify(stripeSdkClient).createSession(params);
    }

    @Test
    void createWebhookEvent_delegatesToStripeSdkClient() throws Exception {
        Event event = new Event();

        when(stripeSdkClient.createWebhookEvent("payload", "sig", "secret")).thenReturn(event);

        Event result = stripeClientWrapper.createWebhookEvent("payload", "sig", "secret");

        assertThat(result).isSameAs(event);
        verify(stripeSdkClient).createWebhookEvent("payload", "sig", "secret");
    }
}
