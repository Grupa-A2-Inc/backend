package org.elearning.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AiApiClient {

    private final String baseUrl;
    private final String apiKey;

    private final RestClient generateRestClient;
    private final RestClient feedbackRestClient;
    private final RestClient adaptiveRestClient;

    public AiApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.api.base-url}") String baseUrl,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.api.timeout-generate-ms:10000}") int generateTimeout,
            @Value("${ai.api.timeout-feedback-ms:5000}") int feedbackTimeout,
            @Value("${ai.api.timeout-adaptive-ms:10000}") int adaptiveTimeout) {

        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        this.generateRestClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory(generateTimeout))
                .build();

        this.feedbackRestClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory(feedbackTimeout))
                .build();

        this.adaptiveRestClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory(adaptiveTimeout))
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }

    // ==========================================
    // FLUX 1: Generare Test
    // ==========================================
    public AiGenerateResponse generateTest(UUID requestId, UUID studentId, int subjectId, int topicId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("studentId", studentId);
        payload.put("subjectId", subjectId);
        payload.put("topicId", topicId);

        log.info("Trimit request generare test la AI pentru requestId: {}", requestId);

        try {
            return generateRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", apiKey)
                    .header("X-Request-Id", requestId.toString())
                    .body(payload)
                    .retrieve()
                    // Gestionează elegant erorile HTTP 4xx și 5xx
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                log.error("Eroare de la API-ul AI: Status {}", response.getStatusCode());
                                throw new AiApiException("Eroare API AI: " + response.getStatusCode());
                            })
                    .body(AiGenerateResponse.class);

        } catch (ResourceAccessException e) {
            log.error("Timeout la generare AI pentru requestId: {}", requestId);
            throw new AiTimeoutException("Timeout generare AI: " + e.getMessage());
        }
    }

    // ==========================================
    // FLUX 2: Exerciții Adaptive (Start Sesiune)
    // ==========================================
    public AiAdaptiveResponse requestAdaptiveExercises(UUID sessionId, UUID studentId, int subjectId, int topicId, int count) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId);
        payload.put("subjectId", subjectId);
        payload.put("topicId", topicId);
        payload.put("count", count);

        try {
            return adaptiveRestClient.post()
                    .uri("/api/adaptive/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new AiApiException("Serviciul AI indisponibil: Status " + response.getStatusCode());
                    })
                    .body(AiAdaptiveResponse.class);

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Timeout la AI Adaptive pentru studentId: {}", studentId);
            throw new AiApiException("Timeout AI Adaptive: " + e.getMessage());
        }
    }

    // ==========================================
    // FLUX 2: Feedback Adaptive
    // ==========================================
    public void sendAdaptiveFeedback(Object payload) {
        try {
            feedbackRestClient.post()
                    .uri("/api/adaptive/feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("A eșuat feedback-ul adaptiv la ML. Eroare: {}", e.getMessage());
        }
    }
}