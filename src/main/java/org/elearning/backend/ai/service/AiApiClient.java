package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiAdaptiveExercisesRequest;
import org.elearning.backend.ai.dto.AiGenerateResponse;
import org.elearning.backend.ai.dto.AiStudentRegistrationResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.JsonSerializingException;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AiApiClient {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String AI_SUCCESS_STATUS = "ok";

    private static final String GENERATE_TEST_URI = "/ai/api/generate";
    private static final String ADAPTIVE_EXERCISES_URI = "/ai/api/v1/adaptive/exercises";
    private static final String ADAPTIVE_FEEDBACK_URI = "/ai/api/v1/adaptive/feedback";
    private static final String STUDENT_REGISTRATION_URI = "/ai/api/v1/students";

    private final String apiKey;
    private final RestClient generateRestClient;
    private final RestClient feedbackRestClient;
    private final RestClient adaptiveRestClient;
    private final RestClient studentRegistrationRestClient;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;

    public AiApiClient(
            RestClient.Builder restClientBuilder,
            LessonRepository lessonRepository,
            ObjectMapper objectMapper,
            @Value("${ai.api.base-url}") String baseUrl,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.api.timeout-generate-ms:10000}") int generateTimeout,
            @Value("${ai.api.timeout-feedback-ms:5000}") int feedbackTimeout,
            @Value("${ai.api.timeout-adaptive-ms:10000}") int adaptiveTimeout,
            @Value("${ai.api.timeout-student-registration-ms:5000}") int studentRegistrationTimeout) {

        this.apiKey = apiKey;
        this.lessonRepository = lessonRepository;
        this.objectMapper = objectMapper;

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

    // ==========================================
    // FLUX 1: Generare Test
    // ==========================================

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
                    .uri(GENERATE_TEST_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("Eroare de la API-ul AI (generate): Status {}", response.getStatusCode());
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
    // ==========================================

    public AiAdaptiveResponse requestAdaptiveExercises(UUID sessionId, UUID studentId, int subjectId, int topicId, int count) {
        AiAdaptiveExercisesRequest payload = new AiAdaptiveExercisesRequest(
                studentId.toString(),
                subjectId,
                topicId,
                count
        );
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new JsonSerializingException("Failed to serialize adaptive exercises request payload.");
        }

        log.info(
                "AI adaptive exercises request: sessionId={} studentId={} subjectId={} topicId={} count={} body={}",
                sessionId,
                studentId,
                subjectId,
                topicId,
                count,
                requestBody
        );

        try {
            return adaptiveRestClient.post()
                    .uri(ADAPTIVE_EXERCISES_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("AI returned error status (adaptive exercises): {}", response.getStatusCode());
                        throw new AiApiException("Serviciul AI indisponibil: Status " + response.getStatusCode());
                    })
                    .body(AiAdaptiveResponse.class);

        } catch (ResourceAccessException e) {
            log.error("Timeout la AI Adaptive pentru studentId: {}", studentId);
            throw new AiTimeoutException("Timeout AI Adaptive: " + e.getMessage());
        }
    }

    // ==========================================
    // FLUX 2: Feedback Adaptive
    // ==========================================

    public void sendAdaptiveFeedback(Object payload) {
        try {
            feedbackRestClient.post()
                    .uri(ADAPTIVE_FEEDBACK_URI)
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

    // ==========================================
    // FLUX 3: Înregistrare Student
    // ==========================================

    public AiStudentRegistrationResponse registerStudent(UUID requestId, UUID studentId) {
        String requestBody = """
                {"requestId":"%s","studentId":"%s"}
                """.formatted(requestId, studentId).trim();

        try {
            log.info(
                    "AI student registration request: requestId={} studentId={} body={}",
                    requestId,
                    studentId,
                    requestBody
            );

            AiStudentRegistrationResponse response = studentRegistrationRestClient.post()
                    .uri(STUDENT_REGISTRATION_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, apiKey)
                    .header("X-Request-Id", requestId.toString())
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        String responseBody = readErrorResponseBody(clientResponse);
                        log.error(
                                "AI student registration failed: status={} requestId={} studentId={} responseBody={}",
                                clientResponse.getStatusCode(),
                                requestId,
                                studentId,
                                responseBody
                        );
                        throw new AiApiException(
                                "AI student registration failed with status: "
                                        + clientResponse.getStatusCode()
                                        + ", response body: "
                                        + responseBody
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
            log.error("Timeout la student registration AI pentru studentId: {}", studentId);
            throw new AiTimeoutException("Timeout student registration AI: " + e.getMessage());
        }
    }

    static String readErrorResponseBody(ClientHttpResponse response) {
        try {
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            return responseBody == null || responseBody.isBlank() ? "<empty>" : responseBody;
        } catch (IOException e) {
            return "<unreadable>";
        }
    }
}
