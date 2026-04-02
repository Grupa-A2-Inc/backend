package org.elearning.backend.organization.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.common.exception.ResourceNotFoundException;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getOwnerId()));

        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setCountry(request.getCountry());
        organization.setCity(request.getCity());
        organization.setOrganizationType(request.getOrganizationType());
        organization.setAddress(request.getAddress());
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setOwner(owner);

        Organization saved = organizationRepository.save(organization);
        return toResponse(saved);
    }

    public OrganizationResponse getOrganizationById(UUID id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
        return toResponse(organization);
    }

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));

        organization.setName(request.getName());
        organization.setCountry(request.getCountry());
        organization.setCity(request.getCity());
        organization.setOrganizationType(request.getOrganizationType());
        organization.setAddress(request.getAddress());
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setUpdatedAt(LocalDateTime.now());

        Organization saved = organizationRepository.save(organization);
        return toResponse(saved);
    }

    public void deleteOrganization(UUID id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found: " + id);
        }
        organizationRepository.deleteById(id);
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCountry(),
                organization.getCity(),
                organization.getOrganizationType(),
                organization.getAddress(),
                organization.getPhoneNumber(),
                organization.getOwner().getId(),
                organization.getOwner().getEmail()
        );
    }
}