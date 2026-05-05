package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID>, JpaSpecificationExecutor<Classroom> {
    List<Classroom> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Classroom> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
