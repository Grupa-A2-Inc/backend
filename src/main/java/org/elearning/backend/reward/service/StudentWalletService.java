package org.elearning.backend.reward.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.reward.dto.StudentWalletRequest;
import org.elearning.backend.reward.dto.StudentWalletResponse;
import org.elearning.backend.reward.entity.StudentWallet;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.repository.StudentWalletRepository;
import org.elearning.backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentWalletService {

    private final StudentWalletRepository studentWalletRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public StudentWalletResponse upsertWallet(UUID studentId, StudentWalletRequest request) {
        if (!studentRepository.existsById(studentId)) {
            throw new RewardBadRequestException("Student not found: " + studentId);
        }
        String walletAddress = normalizeWalletAddress(request.getWalletAddress());
        StudentWallet wallet = studentWalletRepository.findByStudentId(studentId)
                .orElseGet(StudentWallet::new);

        if ((wallet.getId() == null || !walletAddress.equalsIgnoreCase(wallet.getWalletAddress()))
                && studentWalletRepository.existsByWalletAddressIgnoreCase(walletAddress)) {
            throw new RewardBadRequestException("Wallet address is already assigned to another student");
        }

        wallet.setStudentId(studentId);
        wallet.setWalletAddress(walletAddress);
        wallet.setVerified(Boolean.TRUE);
        return toResponse(studentWalletRepository.save(wallet));
    }

    private String normalizeWalletAddress(String walletAddress) {
        if (!EvmAddressValidator.isValid(walletAddress)) {
            throw new RewardBadRequestException("Student wallet address must be a valid EVM address");
        }
        return walletAddress.toLowerCase(Locale.ROOT);
    }

    private StudentWalletResponse toResponse(StudentWallet wallet) {
        return new StudentWalletResponse(
                wallet.getId(),
                wallet.getStudentId(),
                wallet.getWalletAddress(),
                wallet.getVerified(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
