package org.elearning.backend.subscription.controller;

import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanControllerTest {

    @Mock
    private OrganizationSubscriptionService organizationSubscriptionService;

    @InjectMocks
    private SubscriptionPlanController subscriptionPlanController;

    @Test
    void getSubscriptionPlans_returns200Ok() {
        List<SubscriptionPlanResponse> responseBody = List.of(
                new SubscriptionPlanResponse(
                        UUID.randomUUID(),
                        "STARTER",
                        "Starter",
                        100,
                        5,
                        20,
                        false,
                        new BigDecimal("29.99"),
                        "EUR",
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 1, 0, 0)
                )
        );
        when(organizationSubscriptionService.getAllSubscriptionPlans()).thenReturn(responseBody);

        ResponseEntity<List<SubscriptionPlanResponse>> response = subscriptionPlanController.getSubscriptionPlans();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }
}
