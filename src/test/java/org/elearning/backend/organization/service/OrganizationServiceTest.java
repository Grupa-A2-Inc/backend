package org.elearning.backend.organization.service;

import org.elearning.backend.common.exception.ResourceNotFoundException;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@scoala.ro");
        user.setFirstName("Ion");
        user.setLastName("Pop");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Organization createTestOrganization(User owner) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Scoala Nr. 1");
        org.setOwner(owner);
        return org;
    }

    @Test
    void createOrganization_success() {
        User owner = createTestUser();

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Scoala Nr. 1")
                .ownerId(owner.getId())
                .build();

        Organization saved = createTestOrganization(owner);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertEquals("Scoala Nr. 1", response.getName());
        assertEquals(owner.getId(), response.getOwnerId());
        assertEquals("owner@scoala.ro", response.getOwnerEmail());
    }

    @Test
    void createOrganization_ownerNotFound_throwsException() {
        UUID ownerId = UUID.randomUUID();

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Scoala Nr. 1")
                .ownerId(ownerId)
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.createOrganization(request));
    }

    @Test
    void getOrganizationById_success() {
        User owner = createTestUser();
        Organization org = createTestOrganization(owner);

        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        OrganizationResponse response = organizationService.getOrganizationById(org.getId());

        assertEquals("Scoala Nr. 1", response.getName());
        assertEquals(owner.getId(), response.getOwnerId());
    }

    @Test
    void getOrganizationById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.getOrganizationById(id));
    }

    @Test
    void getAllOrganizations_returnsListOfOrganizations() {
        User owner = createTestUser();
        Organization org1 = createTestOrganization(owner);
        Organization org2 = createTestOrganization(owner);
        org2.setName("Scoala Nr. 2");

        when(organizationRepository.findAll()).thenReturn(List.of(org1, org2));

        List<OrganizationResponse> responses = organizationService.getAllOrganizations();

        assertEquals(2, responses.size());
        assertEquals("Scoala Nr. 1", responses.get(0).getName());
        assertEquals("Scoala Nr. 2", responses.get(1).getName());
    }

    @Test
    void updateOrganization_success() {
        User owner = createTestUser();
        Organization org = createTestOrganization(owner);

        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Scoala Nr. 1 Actualizata")
                .build();

        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenReturn(org);

        OrganizationResponse response = organizationService.updateOrganization(org.getId(), request);

        assertEquals("Scoala Nr. 1 Actualizata", response.getName());
    }

    @Test
    void updateOrganization_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Scoala Nr. 1 Actualizata")
                .build();

        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.updateOrganization(id, request));
    }

    @Test
    void deleteOrganization_success() {
        UUID id = UUID.randomUUID();

        when(organizationRepository.existsById(id)).thenReturn(true);

        organizationService.deleteOrganization(id);

        verify(organizationRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteOrganization_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(organizationRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.deleteOrganization(id));
    }
}