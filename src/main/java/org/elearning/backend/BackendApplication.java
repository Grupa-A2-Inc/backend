package org.elearning.backend;

import com.testifyai.crypto.config.CryptoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.function.BiFunction;

@SpringBootApplication(scanBasePackages = {"org.elearning.backend", "com.testifyai.crypto"})
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(CryptoProperties.class)
public class BackendApplication {

    /**
     * Boots the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    static BiFunction<Class<?>, String[], ConfigurableApplicationContext> applicationRunner = SpringApplication::run;

    public static void main(String[] args) {
        applicationRunner.apply(BackendApplication.class, args);

    }

}
