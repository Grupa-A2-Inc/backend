package org.elearning.backend.organization.repository;

import org.elearning.backend.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByOwnerId(UUID ownerId);
    boolean existsByName(String name);
    Optional<Organization> findByName(String name);
}