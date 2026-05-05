package org.elearning.backend.organization.controller;

import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.service.OrganizationService;
import org.elearning.backend.subscription.dto.request.CheckoutRequest;
import org.elearning.backend.subscription.dto.request.UpdateSubscriptionPlanRequest;
import org.elearning.backend.subscription.dto.response.CheckoutSessionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionStatusResponse;
import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.elearning.backend.subscription.service.StripeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.elearning.backend.common.dto.response.PaginatedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationSubscriptionService organizationSubscriptionService;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private OrganizationController organizationController;

    @Test
    void createOrganization_returns201Created() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("Org", null, null, null, null, null, null);
        OrganizationResponse responseBody = makeResponse();
        when(organizationService.createOrganization(request)).thenReturn(responseBody);

        ResponseEntity<OrganizationResponse> response = organizationController.createOrganization(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void getAllOrganizations_returns200Ok() {
        PaginatedResponse<OrganizationResponse> responses =
                new PaginatedResponse<>(List.of(makeResponse(), makeResponse()), 0, 10, 2L);

        when(organizationService.getAllOrganizationsPaginated(null, null, null, null, null))
                .thenReturn(responses);

        ResponseEntity<PaginatedResponse<OrganizationResponse>> response =
                organizationController.getAllOrganizations(null, null, null, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responses);
    }

    @Test
    void getOrganizationById_returns200Ok() {
        UUID id = UUID.randomUUID();
        OrganizationResponse responseBody = makeResponse();
        when(organizationService.getOrganizationById(id)).thenReturn(responseBody);

        ResponseEntity<OrganizationResponse> response = organizationController.getOrganizationById(id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void getOrganizationSubscription_returns200Ok() {
        UUID organizationId = UUID.randomUUID();
        OrganizationSubscriptionStatusResponse responseBody = new OrganizationSubscriptionStatusResponse(
                organizationId,
                OrganizationSubscriptionStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0),
                new SubscriptionPlanResponse(
                        UUID.randomUUID(),
                        "FREE",
                        "Free",
                        31,
                        1,
                        3,
                        false,
                        null,
                        "EUR",
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 1, 0, 0)
                )
        );
        when(organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId)).thenReturn(responseBody);

        ResponseEntity<OrganizationSubscriptionStatusResponse> response =
                organizationController.getOrganizationSubscription(organizationId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void updateOrganization_returns204NoContent() {
        UUID id = UUID.randomUUID();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("Updated", null, null, null, null, null);

        ResponseEntity<Void> response = organizationController.updateOrganization(id, request);

        verify(organizationService).updateOrganization(id, request);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void deleteOrganization_returns204NoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = organizationController.deleteOrganization(id);

        verify(organizationService).deleteOrganization(id);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void createCheckoutSession_returns200Ok() {
        UUID organizationId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest();
        request.setPlanId(planId);
        request.setSuccessUrl("http://localhost:3000/success");
        request.setCancelUrl("http://localhost:3000/cancel");
        CheckoutSessionResponse responseBody = new CheckoutSessionResponse(
                "https://checkout.stripe.com/test",
                "cs_test_123"
        );

        when(stripeService.createCheckoutSession(organizationId, request)).thenReturn(responseBody);

        ResponseEntity<CheckoutSessionResponse> response =
                organizationController.createCheckoutSession(organizationId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void changeSubscriptionPlan_returns200Ok() {
        UUID organizationId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UpdateSubscriptionPlanRequest request = new UpdateSubscriptionPlanRequest();
        request.setPlanId(planId);
        OrganizationSubscriptionResponse responseBody = new OrganizationSubscriptionResponse(
                UUID.randomUUID(),
                organizationId,
                planId,
                OrganizationSubscriptionStatus.ACTIVE,
                null,
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        when(organizationSubscriptionService.changePlan(organizationId, request)).thenReturn(responseBody);

        ResponseEntity<OrganizationSubscriptionResponse> response =
                organizationController.changeSubscriptionPlan(organizationId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    private OrganizationResponse makeResponse() {
        return new OrganizationResponse(
                UUID.randomUUID(),
                "Org",
                "Romania",
                "Bucharest",
                "School",
                "Address",
                "123",
                UUID.randomUUID(),
                "owner@example.com"
        );
    }
}
