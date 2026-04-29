package org.elearning.backend.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class SecurityBeansConfigTest {

    private final SecurityBeansConfig securityBeansConfig = new SecurityBeansConfig();

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoder() {
        PasswordEncoder passwordEncoder = securityBeansConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
    }
}
