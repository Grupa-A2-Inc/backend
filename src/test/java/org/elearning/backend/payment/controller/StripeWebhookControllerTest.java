package org.elearning.backend.payment.controller;

import com.stripe.model.Event;
import org.elearning.backend.subscription.service.StripeWebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private StripeWebhookService stripeWebhookService;

    @InjectMocks
    private StripeWebhookController stripeWebhookController;

    @Test
    void handleWebhook_verifiesAndProcessesEvent() {
        Event event = new Event();
        when(stripeWebhookService.verifyAndParseWebhook("payload", "sig")).thenReturn(event);

        ResponseEntity<Void> response = stripeWebhookController.handleWebhook("payload", "sig");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(stripeWebhookService).verifyAndParseWebhook("payload", "sig");
        verify(stripeWebhookService).handleEvent(event);
    }
}
