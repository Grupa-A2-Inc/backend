package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.common.exception.InvalidCredentials;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        Role role = roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)
                .orElseThrow(() -> new RuntimeException("Role ORGANIZATION_ADMIN not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        return new AuthResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {

        Optional<User> potentialUser=userRepository.findByEmail(request.getEmail());
        if(potentialUser.isEmpty()){
            throw new InvalidCredentials("An account registered with that email does not exist");
        }

        User userPwdCheck=potentialUser.get();
        if (!passwordEncoder.matches(request.getPassword(), userPwdCheck.getPasswordHash())){
            throw new InvalidCredentials("Invalid password");
        }

        return new AuthResponse("Login successful", null);
    }
}