package org.elearning.backend.payment.controller;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.subscription.service.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event = stripeWebhookService.verifyAndParseWebhook(payload, sigHeader);
        stripeWebhookService.handleEvent(event);

        return ResponseEntity.ok().build();
    }
}