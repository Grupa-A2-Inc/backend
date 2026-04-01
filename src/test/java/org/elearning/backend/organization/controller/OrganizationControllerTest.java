package org.elearning.backend.organization.controller;

import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private OrganizationController organizationController;

    @Test
    void createOrganization_returns201Created() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder().name("Org").build();
        OrganizationResponse responseBody = makeResponse();
        when(organizationService.createOrganization(request)).thenReturn(responseBody);

        ResponseEntity<OrganizationResponse> response = organizationController.createOrganization(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void getAllOrganizations_returns200Ok() {
        List<OrganizationResponse> responses = List.of(makeResponse(), makeResponse());
        when(organizationService.getAllOrganizations()).thenReturn(responses);

        ResponseEntity<List<OrganizationResponse>> response = organizationController.getAllOrganizations();

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
    void updateOrganization_returns200Ok() {
        UUID id = UUID.randomUUID();
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder().name("Updated").build();
        OrganizationResponse responseBody = makeResponse();
        when(organizationService.updateOrganization(id, request)).thenReturn(responseBody);

        ResponseEntity<OrganizationResponse> response = organizationController.updateOrganization(id, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void deleteOrganization_returns204NoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = organizationController.deleteOrganization(id);

        verify(organizationService).deleteOrganization(id);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
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
