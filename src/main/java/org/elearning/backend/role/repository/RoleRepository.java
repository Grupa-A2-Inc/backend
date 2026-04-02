package org.elearning.backend.role.repository;

import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}