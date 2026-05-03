package org.elearning.backend.ai.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.dto.AiStudentRegistrationRequest;
import org.elearning.backend.ai.dto.AiStudentRegistrationResponse;
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

    private static final String API_KEY_HEADER = "X-API-Key";

    private final String apiKey;
    private static final String API_KEY = "X-Api-Key";
    private static final String AI_SUCCESS_STATUS = "ok";
    private static final String GENERATE_TEST_URI = "/api/generate";
    private static final String ADAPTIVE_EXERCISES_URI = "/api/adaptive/exercises";
    private static final String ADAPTIVE_FEEDBACK_URI = "/api/adaptive/feedback";
    private static final String STUDENT_REGISTRATION_URI = "/ai/api/students";

    private final String apiKey;
    private final RestClient generateRestClient;
    private final RestClient feedbackRestClient;
    private final RestClient adaptiveRestClient;
    private final LessonRepository lessonRepository;
    private final RestClient studentRegistrationRestClient;

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
            LessonRepository lessonRepository,
            @Value("${ai.api.base-url}") String baseUrl,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.api.timeout-generate-ms:10000}") int generateTimeout,
            @Value("${ai.api.timeout-feedback-ms:5000}") int feedbackTimeout,
            @Value("${ai.api.timeout-adaptive-ms:10000}") int adaptiveTimeout,
            @Value("${ai.api.timeout-student-registration-ms:5000}") int studentRegistrationTimeout) {

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

        this.studentRegistrationRestClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory(studentRegistrationTimeout))
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
        payload.put("content", content);
        payload.put("count", count);

        try {
            return generateRestClient.post()
                    .uri("/ai/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
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
        payload.put("studentId", studentId.toString());
        payload.put("subjectId", subjectId);
        payload.put("topicId", topicId);
        payload.put("count", count);

        try {
            return adaptiveRestClient.post()
                    .uri("/ai/api/adaptive/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("AI returned error status: {}", response.getStatusCode());
                        throw new AiApiException("Serviciul AI indisponibil: Status " + response.getStatusCode());
                    })
                    .body(AiAdaptiveResponse.class);

        } catch (ResourceAccessException e) {
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
                    .uri("/ai/api/adaptive/feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("A esuat feedback-ul adaptiv la ML. Eroare: {}", e.getMessage());
        }
    }

    public AiStudentRegistrationResponse registerStudent(UUID requestId, UUID studentId) {
        AiStudentRegistrationRequest payload = new AiStudentRegistrationRequest(
                requestId.toString(),
                studentId.toString()
        );

        try {
            AiStudentRegistrationResponse response = studentRegistrationRestClient.post()
                    .uri(STUDENT_REGISTRATION_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY, apiKey)
                    .header("X-Request-Id", requestId.toString())
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new AiApiException(
                                "AI student registration failed with status: " + clientResponse.getStatusCode()
                        );
                    })
                    .body(AiStudentRegistrationResponse.class);

            if (response == null) {
                throw new AiApiException("AI student registration returned an empty response");
            }

            if (!AI_SUCCESS_STATUS.equalsIgnoreCase(response.status())) {
                throw new AiApiException(
                        "AI student registration failed with status payload: " + response.status()
                );
            }

            return response;
        } catch (ResourceAccessException e) {
            throw new AiTimeoutException("Timeout student registration AI: " + e.getMessage());
        }
    }
}
