package org.elearning.backend.auth;

import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void register_success_returns200() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("User registered successfully");
    }

    @Test
    void register_userSavedInDB() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("db@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        restTemplate.postForEntity("/auth/register", request, String.class);

        assertThat(userRepository.existsByEmail("db@test.com")).isTrue();
    }

    @Test
    void register_duplicateEmail_fails() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        restTemplate.postForEntity("/auth/register", request, String.class);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("nu-sunt-email");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_missingPassword_returns400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}