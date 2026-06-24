package org.elearning.backend.reward.repository;

import org.elearning.backend.reward.entity.StudentWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentWalletRepository extends JpaRepository<StudentWallet, UUID> {
    Optional<StudentWallet> findByStudentId(UUID studentId);

    boolean existsByWalletAddressIgnoreCase(String walletAddress);
}
