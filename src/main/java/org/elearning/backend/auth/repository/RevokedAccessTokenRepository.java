package org.elearning.backend.auth.repository;

import org.elearning.backend.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, UUID> {

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RevokedAccessToken r WHERE r.expiresAt < :now")
    void deleteAllExpiredBefore(LocalDateTime now);
}