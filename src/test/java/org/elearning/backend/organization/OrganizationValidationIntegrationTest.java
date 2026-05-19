package org.elearning.backend.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.organization.controller.OrganizationController;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.exception.OrganizationExceptionHandler;
import org.elearning.backend.organization.service.OrganizationService;
import org.elearning.backend.security.access.AccessService;
import org.elearning.backend.security.jwt.JwtAuthenticationFilter;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.subscription.service.OrganizationSubscriptionService;
import org.elearning.backend.subscription.service.StripeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OrganizationExceptionHandler.class)
@ActiveProfiles("test")
class OrganizationValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private OrganizationSubscriptionService organizationSubscriptionService;

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AccessService accessService;

    @Test
    void createOrganization_invalidPhoneNumber_returns400() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Scoala Nr. 1",
                "Romania",
                "Bucuresti",
                "Scoala",
                "Str. Test 1",
                "1111111111",
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phone number format is invalid"));

        verify(organizationService, never()).createOrganization(any(CreateOrganizationRequest.class));
    }

    @Test
    void updateOrganization_acceptsInternationalPhoneNumber() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "Scoala Nr. 1",
                "Romania",
                "Bucuresti",
                "Scoala",
                "Str. Test 1",
                "+40722123456"
        );

        when(organizationService.updateOrganization(any(UUID.class), any(UpdateOrganizationRequest.class)))
                .thenReturn(new OrganizationResponse(
                        organizationId,
                        "Scoala Nr. 1",
                        "Romania",
                        "Bucuresti",
                        "Scoala",
                        "Str. Test 1",
                        "+40722123456",
                        UUID.randomUUID(),
                        "owner@example.com"
                ));

        mockMvc.perform(put("/api/v1/organizations/{id}", organizationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(organizationService).updateOrganization(any(UUID.class), any(UpdateOrganizationRequest.class));
    }
}
