package org.elearning.backend.security.auth;

import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_returnsWrappedUser() {
        User user = makeUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo(user.getEmail());
    }

    @Test
    void loadUserByUsername_missingUser_throwsException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: missing@example.com");
    }

    @Test
    void loadUserById_returnsWrappedUser() {
        User user = makeUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        CustomUserDetails userDetails = customUserDetailsService.loadUserById(user.getId());

        assertThat(userDetails.getUserId()).isEqualTo(user.getId());
    }

    @Test
    void loadUserById_missingUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserById(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    private User makeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setRole(new Role(RoleName.STUDENT));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
