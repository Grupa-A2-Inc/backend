package org.elearning.backend.subscription.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.subscription.config.StripeConfig;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeClientWrapper {

    private final StripeConfig stripeConfig;
    private final StripeSdkClient stripeSdkClient;

    public com.stripe.model.checkout.Session createCheckoutSession(SessionCreateParams params) throws StripeException {
        configureApiKey(stripeConfig.getSecretKey());
        return createSession(params);
    }

    public Event constructWebhookEvent(String payload, String sigHeader, String secret)
            throws SignatureVerificationException {
        return createWebhookEvent(payload, sigHeader, secret);
    }

    private static void configureApiKey(String secretKey) {
        Stripe.apiKey = secretKey;
    }

    com.stripe.model.checkout.Session createSession(SessionCreateParams params) throws StripeException {
        return stripeSdkClient.createSession(params);
    }

    Event createWebhookEvent(String payload, String sigHeader, String secret)
            throws SignatureVerificationException {
        return stripeSdkClient.createWebhookEvent(payload, sigHeader, secret);
    }
}
