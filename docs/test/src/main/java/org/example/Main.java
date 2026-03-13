package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

// 1. ENTRY POINT - Am adaugat "exclude" pentru a ignora baza de date momentan
@SpringBootApplication()
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}

// 2. SECURITY CONFIG - Permite accesul liber la endpoint-uri
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/status", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}

// 3. TEST CONTROLLER - Ce vei vedea in browser
@RestController
@RequestMapping("/api")
class TestController {
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "UP",
                "message", "Backend-ul cu baza de date",
                "java_version", System.getProperty("java.version"),
                "timestamp", LocalDateTime.now()
        );
    }
}