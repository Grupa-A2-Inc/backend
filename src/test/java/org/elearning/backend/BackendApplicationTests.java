package org.elearning.backend;

import org.elearning.backend.auth.repository.RevokedAccessTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class BackendApplicationTests {

    @Test
    void application_startsWithMinimalConfiguration() {
        assertDoesNotThrow(() -> {
            try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(
                    BackendApplication.class,
                    MinimalTestConfig.class
            )
                    .web(WebApplicationType.NONE)
                    .lazyInitialization(true)
                    .properties(
                            "spring.autoconfigure.exclude="
                                    + DataSourceAutoConfiguration.class.getName() + ","
                                    + DataSourceTransactionManagerAutoConfiguration.class.getName() + ","
                                    + HibernateJpaAutoConfiguration.class.getName() + ","
                                    + FlywayAutoConfiguration.class.getName(),
                            "JWT_SECRET=12345678901234567890123456789012"
                    )
                    .run()) {
                // Successful context creation is the contract under test.
            }
        });
    }

    @TestConfiguration
    static class MinimalTestConfig {

        @Bean
        RevokedAccessTokenRepository revokedAccessTokenRepository() {
            return mock(RevokedAccessTokenRepository.class);
        }
    }
}
