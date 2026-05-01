package org.elearning.backend.ai.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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

    private final String apiKey;
    private static final String API_KEY = "X-Api-Key";
    private final RestClient generateRestClient;
    private final RestClient feedbackRestClient;
    private final RestClient adaptiveRestClient;
    private final LessonRepository lessonRepository;

    public AiApiClient(
            RestClient.Builder restClientBuilder,
            LessonRepository lessonRepository,
            @Value("${ai.api.base-url}") String baseUrl,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.api.timeout-generate-ms:10000}") int generateTimeout,
            @Value("${ai.api.timeout-feedback-ms:5000}") int feedbackTimeout,
            @Value("${ai.api.timeout-adaptive-ms:10000}") int adaptiveTimeout) {

        this.apiKey = apiKey;
        this.lessonRepository = lessonRepository;

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

    public AiGenerateResponse generateTest(UUID lessonId, int count) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lectia nu exista: " + lessonId));

        String content = lesson.getContentMarkdown();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Lectia nu are continut: " + lessonId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonContent", content);
        payload.put("count", count);

        try {
            return generateRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY, apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                log.error("Eroare de la API-ul AI: Status {}", response.getStatusCode());
                                throw new AiApiException("Eroare API AI: " + response.getStatusCode());
                            })
                    .body(AiGenerateResponse.class);

        } catch (ResourceAccessException e) {
            log.error("Timeout la generare AI pentru lessonId: {}", lessonId);
            throw new AiTimeoutException("Timeout generare AI: " + e.getMessage());
        }
    }

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
                    .header(API_KEY, apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AiApiException("Serviciul AI indisponibil: Status " + response.getStatusCode());
                    })
                    .body(AiAdaptiveResponse.class);

        } catch (ResourceAccessException e) {
            log.error("Timeout la AI Adaptive pentru studentId: {}", studentId);
            throw new AiApiException("Timeout AI Adaptive: " + e.getMessage());
        }
    }

    public void sendAdaptiveFeedback(Object payload) {
        try {
            feedbackRestClient.post()
                    .uri("/api/adaptive/feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY, apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("A esuat feedback-ul adaptiv la ML. Eroare: {}", e.getMessage());
        }
    }
}