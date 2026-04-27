package org.elearning.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
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

    /**
     * Creates an AiApiClient and configures three RestClient instances for generate, feedback,
     * and adaptive endpoints using the provided base URL and per-endpoint timeouts. The API key
     * is stored for use in outbound requests.
     *
     * @param restClientBuilder a RestClient.Builder used to construct per-endpoint RestClient instances
     * @param baseUrl the base URL for the AI API endpoints
     * @param apiKey the API key value sent in the `X-Api-Key` header for requests
     * @param generateTimeout connect/read timeout in milliseconds for the generate endpoint
     * @param feedbackTimeout connect/read timeout in milliseconds for the feedback endpoint
     * @param adaptiveTimeout connect/read timeout in milliseconds for the adaptive endpoint
     */
    public AiApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.api.base-url}") String baseUrl,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.api.timeout-generate-ms:10000}") int generateTimeout,
            @Value("${ai.api.timeout-feedback-ms:5000}") int feedbackTimeout,
            @Value("${ai.api.timeout-adaptive-ms:10000}") int adaptiveTimeout) {

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

    /**
     * Create a ClientHttpRequestFactory with both connect and read timeouts set to the given value.
     *
     * @param timeoutMs timeout in milliseconds to apply to both connect and read operations
     * @return a configured ClientHttpRequestFactory with the specified timeouts
     */
    private ClientHttpRequestFactory createRequestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }

    // ==========================================
    // FLUX 1: Generare Test
    /**
     * Requests the AI service to generate a test for the specified lesson and request identifier.
     *
     * @param requestId unique identifier for the generation request (used to correlate the operation)
     * @param lessonId  identifier of the lesson for which the test should be generated
     * @return an {@link AiGenerateResponse} containing the generated test data from the AI service
     * @throws AiApiException     if the AI service responds with an error HTTP status
     * @throws AiTimeoutException if a network timeout occurs while contacting the AI service
     */
    public AiGenerateResponse generateTest(UUID requestId, UUID lessonId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("lessonId", lessonId);

        log.info("Trimit request generare test la AI pentru requestId: {}", requestId);

        try {
            return generateRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY, apiKey)
                    .header("X-Request-Id", requestId.toString())
                    .body(payload)
                    .retrieve()
                    // Gestioneaza elegant erorile HTTP 4xx și 5xx
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
    /**
     * Requests a set of adaptive exercises for a student from the AI service.
     *
     * @param sessionId an optional session identifier (accepted by the method but not included in the outbound payload)
     * @param studentId the UUID of the student for whom exercises are requested
     * @param subjectId the subject identifier to scope the exercises
     * @param topicId   the topic identifier to scope the exercises
     * @param count     the number of exercises to request
     * @return          the AI service's response containing the adaptive exercises
     * @throws AiApiException if the AI service returns a non-success HTTP status or if a timeout occurs
     */
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

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Timeout la AI Adaptive pentru studentId: {}", studentId);
            throw new AiApiException("Timeout AI Adaptive: " + e.getMessage());
        }
    }

    // ==========================================
    // FLUX 2: Feedback Adaptive
    /**
     * Sends the given object as a JSON payload to the AI adaptive feedback endpoint.
     *
     * The method posts the payload to "/api/adaptive/feedback" and logs any error that occurs;
     * exceptions are swallowed and not propagated.
     *
     * @param payload the object to serialize as JSON and send as the request body
     */
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
            log.warn("A eșuat feedback-ul adaptiv la ML. Eroare: {}", e.getMessage());
        }
    }
}