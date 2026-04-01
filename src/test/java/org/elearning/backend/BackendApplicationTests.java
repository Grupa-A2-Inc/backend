package org.elearning.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BackendApplicationTests {

    @Test
    void main_runsApplicationWithMinimalConfiguration() {
        assertDoesNotThrow(() -> BackendApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.lazy-initialization=true",
                "--spring.autoconfigure.exclude="
                        + DataSourceAutoConfiguration.class.getName() + ","
                        + DataSourceTransactionManagerAutoConfiguration.class.getName() + ","
                        + HibernateJpaAutoConfiguration.class.getName() + ","
                        + FlywayAutoConfiguration.class.getName(),
                "--JWT_SECRET=12345678901234567890123456789012"
        }));
    }

}
