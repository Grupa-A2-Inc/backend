package org.elearning.backend.security;

import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.security.config.SecurityBeansConfig;
import org.elearning.backend.security.listener.Listener;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig(classes = {
        SecurityBeansConfig.class,
        Listener.class,
        AuthenticationManagerEventsTest.TestConfig.class
})
class AuthenticationManagerEventsTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void shouldPublishFailureEventAndLogIt_whenPasswordIsWrong(CapturedOutput output) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("alexia@example.com", "wrong-password");

        assertThrows(BadCredentialsException.class,
                () -> authenticationManager.authenticate(token));

        assertThat(output.getOut()).contains("Authentication failed!");
        assertThat(output.getOut()).contains("alexia@example.com");
    }

    @Configuration
    static class TestConfig {

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public CustomUserDetails loadUserByUsername(String username) {
                    User user = new User();
                    user.setEmail(username);
                    user.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHi8mQ5nQf8U9fD5kQpimeISFRCGDpa2"); // bcrypt pentru "password"
                    user.setStatus(UserStatus.ACTIVE);

                    Role role = new Role();
                    role.setName(RoleName.ORGANIZATION_ADMIN);
                    user.setRole(role);

                    return new CustomUserDetails(user);
                }
            };
        }
    }
}