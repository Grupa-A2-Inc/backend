package org.elearning.backend.organization.controller;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.security.access.AccessService;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionStatusResponse;
import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
@WithMockUser(username = "org-admin@test.com", roles = "ORGANIZATION_ADMIN")
class OrganizationSubscriptionControllerAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private AccessService accessService;

    @MockitoBean
    private OrganizationSubscriptionService organizationSubscriptionService;

    @Test
    void getOrganizationSubscription_returnsCurrentSubscriptionPayloadForAuthorizedUser() throws Exception {
        UUID organizationId = UUID.randomUUID();
        when(accessService.canViewOrganization(any(), eq(organizationId))).thenReturn(true);
        when(organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId))
                .thenReturn(new OrganizationSubscriptionStatusResponse(
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
                                new BigDecimal("0.00"),
                                "EUR",
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                LocalDateTime.of(2026, 1, 1, 0, 0)
                        )
                ));

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/subscription", organizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPeriodStart").exists())
                .andExpect(jsonPath("$.currentPeriodEnd").exists())
                .andExpect(jsonPath("$.plan.code").value("FREE"))
                .andExpect(jsonPath("$.plan.displayName").value("Free"))
                .andExpect(jsonPath("$.plan.maxUsers").value(31))
                .andExpect(jsonPath("$.plan.maxClassrooms").value(1));

        verify(organizationSubscriptionService).getCurrentOrganizationSubscription(organizationId);
    }

    @Test
    void getOrganizationSubscription_returns403ForUnauthorizedUser() throws Exception {
        UUID organizationId = UUID.randomUUID();
        when(accessService.canViewOrganization(any(), eq(organizationId))).thenReturn(false);

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/subscription", organizationId))
                .andExpect(status().isForbidden());
    }
}
