package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Classroom> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}