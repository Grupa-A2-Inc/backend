package org.elearning.backend.security.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig(AnotherListenerTest.TestConfig.class)
class AnotherListenerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Configuration
    static class TestConfig {
        @Bean
        Listener listener() {
            return new Listener();
        }
    }

    @Test
    void shouldLogFailedAuthenticationEvent(CapturedOutput output) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "alexia@example.com",
                        "wrong-password"
                );

        BadCredentialsException exception =
                new BadCredentialsException("Bad credentials");

        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(authentication, exception);

        publisher.publishEvent(event);

        assertThat(output.getOut()).contains("Authentication failed!");
        assertThat(output.getOut()).contains("alexia@example.com");
    }
}