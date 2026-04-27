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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void main_delegatesToApplicationRunner() {
        String[] args = {"--spring.main.web-application-type=none"};
        BiFunction<Class<?>, String[], ConfigurableApplicationContext> originalRunner = BackendApplication.applicationRunner;
        AtomicReference<Class<?>> receivedSource = new AtomicReference<>();
        AtomicReference<String[]> receivedArgs = new AtomicReference<>();

        try {
            BackendApplication.applicationRunner = (source, arguments) -> {
                receivedSource.set(source);
                receivedArgs.set(arguments);
                return mock(ConfigurableApplicationContext.class);
            };

            BackendApplication.main(args);
        } finally {
            BackendApplication.applicationRunner = originalRunner;
        }

        assertThat(receivedSource.get()).isEqualTo(BackendApplication.class);
        assertThat(receivedArgs.get()).isSameAs(args);
    }

    @TestConfiguration
    static class MinimalTestConfig {

        @Bean
        RevokedAccessTokenRepository revokedAccessTokenRepository() {
            return mock(RevokedAccessTokenRepository.class);
        }
    }
}
