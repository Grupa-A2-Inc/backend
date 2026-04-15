package org.elearning.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

@Configuration
class TestMailConfig {

    @Bean
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
