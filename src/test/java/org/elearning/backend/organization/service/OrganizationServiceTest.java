package org.elearning.backend.organization.service;

import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.exception.OrganizationOwnerNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

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
        org.setCountry("Romania");
        org.setCity("Cluj-Napoca");
        org.setOrganizationType("Scoala");
        org.setAddress("Str. Principala 1");
        org.setPhoneNumber("0740000000");
        org.setOwner(owner);
        return org;
    }

    private CreateOrganizationRequest createOrganizationRequest(UUID ownerId) {
        return new CreateOrganizationRequest(
                "Scoala Nr. 1",
                "Romania",
                "Cluj-Napoca",
                "Scoala",
                "Str. Principala 1",
                "0740000000",
                ownerId
        );
    }

    @Test
    void createOrganization_success() {
        User owner = createTestUser();
        CreateOrganizationRequest request = createOrganizationRequest(owner.getId());
        Organization saved = createTestOrganization(owner);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertEquals("Scoala Nr. 1", response.getName());
        assertEquals("Romania", response.getCountry());
        assertEquals("Cluj-Napoca", response.getCity());
        assertEquals("Scoala", response.getOrganizationType());
        assertEquals("Str. Principala 1", response.getAddress());
        assertEquals("0740000000", response.getPhoneNumber());
        assertEquals(owner.getId(), response.getOwnerId());
        assertEquals("owner@scoala.ro", response.getOwnerEmail());
    }

    @Test
    void createOrganization_withoutOptionalFields_success() {
        User owner = createTestUser();

        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Scoala Nr. 1",
                "Romania",
                "Cluj-Napoca",
                "Scoala",
                null,
                null,
                owner.getId()
        );

        Organization saved = new Organization();
        saved.setId(UUID.randomUUID());
        saved.setName("Scoala Nr. 1");
        saved.setCountry("Romania");
        saved.setCity("Cluj-Napoca");
        saved.setOrganizationType("Scoala");
        saved.setOwner(owner);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertEquals("Scoala Nr. 1", response.getName());
        assertNull(response.getAddress());
        assertNull(response.getPhoneNumber());
    }

    @Test
    void createOrganization_ownerNotFound_throwsException() {
        UUID ownerId = UUID.randomUUID();
        CreateOrganizationRequest request = createOrganizationRequest(ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(OrganizationOwnerNotFoundException.class,
                () -> organizationService.createOrganization(request));
    }

    @Test
    void getOrganizationById_success() {
        User owner = createTestUser();
        Organization org = createTestOrganization(owner);

        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        OrganizationResponse response = organizationService.getOrganizationById(org.getId());

        assertEquals("Scoala Nr. 1", response.getName());
        assertEquals("Romania", response.getCountry());
        assertEquals("Cluj-Napoca", response.getCity());
        assertEquals("Scoala", response.getOrganizationType());
        assertEquals(owner.getId(), response.getOwnerId());
    }

    @Test
    void getOrganizationById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> organizationService.getOrganizationById(id));
    }

    @Test
    void getAllOrganizations_returnsListOfOrganizations() {
        User owner = createTestUser();
        Organization org1 = createTestOrganization(owner);

        Organization org2 = new Organization();
        org2.setId(UUID.randomUUID());
        org2.setName("Scoala Nr. 2");
        org2.setCountry("Romania");
        org2.setCity("Timisoara");
        org2.setOrganizationType("Liceu");
        org2.setOwner(owner);

        when(organizationRepository.findAll()).thenReturn(List.of(org1, org2));

        List<OrganizationResponse> responses = organizationService.getAllOrganizations();

        assertEquals(2, responses.size());
        assertEquals("Scoala Nr. 1", responses.get(0).getName());
        assertEquals("Scoala Nr. 2", responses.get(1).getName());
        assertEquals("Cluj-Napoca", responses.get(0).getCity());
        assertEquals("Timisoara", responses.get(1).getCity());
    }

    @Test
    void getAllOrganizations_emptyList() {
        when(organizationRepository.findAll()).thenReturn(List.of());

        List<OrganizationResponse> responses = organizationService.getAllOrganizations();

        assertTrue(responses.isEmpty());
    }

    @Test
    void updateOrganization_success() {
        User owner = createTestUser();
        Organization org = createTestOrganization(owner);

        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "Scoala Nr. 1 Actualizata",
                "Romania",
                "Bucuresti",
                "Liceu",
                "Str. Noua 5",
                "0750000000"
        );

        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenReturn(org);

        OrganizationResponse response = organizationService.updateOrganization(org.getId(), request);

        assertEquals("Scoala Nr. 1 Actualizata", response.getName());
        assertEquals("Bucuresti", response.getCity());
        assertEquals("Liceu", response.getOrganizationType());
        assertEquals("Str. Noua 5", response.getAddress());
        assertEquals("0750000000", response.getPhoneNumber());
    }

    @Test
    void updateOrganization_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "Scoala Nr. 1 Actualizata",
                "Romania",
                "Bucuresti",
                "Liceu",
                null,
                null
        );

        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
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

        assertThrows(OrganizationNotFoundException.class,
                () -> organizationService.deleteOrganization(id));
    }

    @Test
    void getAllOrganizations_shouldReturnPaginatedResponseWithMetadata() {
        Organization first = buildOrganization("Alpha School");
        Organization second = buildOrganization("Beta School");

        Page<Organization> page = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 2, Sort.by("name").ascending()),
                5
        );

        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<OrganizationResponse> response =
                organizationService.getAllOrganizationsPaginated(0, 2, null, "name", "asc");

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5L);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Alpha School");
        assertThat(response.getContent().get(1).getName()).isEqualTo("Beta School");
    }

    @Test
    void getAllOrganizations_shouldUseDefaultPaginationAndSorting_whenParamsAreNull() {
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        organizationService.getAllOrganizationsPaginated(null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(organizationRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllOrganizations_shouldUseRequestedPaginationAndSorting() {
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        organizationService.getAllOrganizationsPaginated(1, 5, "alpha", "createdAt", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(organizationRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllOrganizations_shouldThrowWhenSortFieldIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> organizationService.getAllOrganizationsPaginated(0, 10, null, "email", "asc"));

        verify(organizationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllOrganizations_shouldHandleSearchParameter() {
        Organization organization = buildOrganization("Open Learning Academy");

        Page<Organization> page = new PageImpl<>(
                List.of(organization),
                PageRequest.of(0, 10),
                1
        );

        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<OrganizationResponse> response =
                organizationService.getAllOrganizationsPaginated(0, 10, "learning", "name", "asc");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Open Learning Academy");
    }

    private Organization buildOrganization(String name) {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@" + name.replace(" ", "").toLowerCase() + ".com");

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(name);
        organization.setCountry("Romania");
        organization.setCity("Bucharest");
        organization.setOrganizationType(null);
        organization.setAddress("Address");
        organization.setPhoneNumber("0712345678");
        organization.setOwner(owner);

        return organization;
    }
}
