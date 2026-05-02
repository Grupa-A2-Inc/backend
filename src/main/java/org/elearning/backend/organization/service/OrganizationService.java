package org.elearning.backend.organization.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.request.CreateOrganizationRequest;
import org.elearning.backend.organization.dto.request.UpdateOrganizationRequest;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.exception.OrganizationOwnerNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new OrganizationOwnerNotFoundException("User not found: " + request.getOwnerId()));

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
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + id));
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
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + id));

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
            throw new OrganizationNotFoundException("Organization not found: " + id);
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

    public PaginatedResponse<OrganizationResponse> getAllOrganizationsPaginated(Integer page, Integer size, String search, String sortBy, String sortDir){

        int pageValue = (page == null || page < 0) ? 0 : page;
        int sizeValue = (size == null || size <= 0) ? 10 : size;

        String sortField = (sortBy == null || sortBy.isBlank()) ? "name" : sortBy;
        String direction = (sortDir == null || sortDir.isBlank()) ? "asc" : sortDir.toLowerCase();

        Set<String> allowedSortFields = Set.of("name", "createdAt");
        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortField);
        }

        Sort sort = direction.equals("desc")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(pageValue, sizeValue, sort);

        Specification<Organization> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String likeValue = "%" + search.toLowerCase().trim() + "%";

            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), likeValue)
            );
        }

        Page<Organization> organizationPage = organizationRepository.findAll(spec, pageable);

        List<OrganizationResponse> content = organizationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PaginatedResponse<>(
                content,
                organizationPage.getNumber(),
                organizationPage.getSize(),
                organizationPage.getTotalElements()
        );

    }
}
