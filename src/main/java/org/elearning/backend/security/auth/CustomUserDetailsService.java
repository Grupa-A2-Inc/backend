package org.elearning.backend.security.auth;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public CustomUserDetails loadUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
