package org.elearning.backend.auth.repository;

import org.elearning.backend.auth.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {
    Optional<ActivationToken> findByTokenHash(String tokenHash);
}