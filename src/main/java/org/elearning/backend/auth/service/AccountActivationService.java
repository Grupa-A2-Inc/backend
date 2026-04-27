package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.SetPasswordRequest;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private final ActivationTokenService activationTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResetPasswordResponse setPassword(SetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthBadRequestException("Passwords do not match.");
        }

        User user = activationTokenService.validateAndConsumeToken(request.getToken());

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new ResetPasswordResponse("Account activated successfully.");
    }
}