package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.dto.AiAdaptiveJobResponse;
import org.elearning.backend.ai.dto.AiAdaptiveJobStatusResponse;
import org.elearning.backend.ai.dto.AiGenerateJobResponse;
import org.elearning.backend.ai.dto.AiGenerateJobStatusResponse;
import org.elearning.backend.ai.dto.AiStudentRegistrationResponse;
import org.elearning.backend.ai.dto.AiFeedbackPayloadDto;
import org.elearning.backend.ai.dto.CurriculumCatalogRequestDto;
import org.elearning.backend.ai.dto.CurriculumCatalogResponseDto;
import org.elearning.backend.ai.dto.FeedbackResultDto;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.JsonSerializingException;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.assessment.model.QuestionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.ClientHttpResponse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiApiClientTest {

    @Mock
    private LessonRepository lessonRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void startGenerateJob_shouldReturnJobData() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");

        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(200, "application/json", "{\"jobId\":\"job-123\",\"status\":\"PENDING\"}", capture -> {
            requestBody.set(capture.body());
            assertThat(capture.path()).isEqualTo("/ai/api/v1/generate/jobs");
            assertThat(capture.method()).isEqualTo("POST");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiGenerateJobResponse response = client.startGenerateJob(lessonId, 5);

        assertThat(requestBody.get()).contains("Lesson content");
        assertThat(requestBody.get()).contains("5");
        assertThat(response.getJobId()).isEqualTo("job-123");
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.PENDING);
    }

    @Test
    void startGenerateJob_shouldThrowWhenLessonMissing() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.empty());

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Lectia nu exista");
    }

    @Test
    void startGenerateJob_shouldThrowWhenLessonContentBlank() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("   ");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lectia nu are continut");
    }

    @Test
    void startGenerateJob_shouldThrowWhenLessonContentNull() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown(null);
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lectia nu are continut");
    }

    @Test
    void startGenerateJob_shouldThrowWhenPayloadSerializationFails() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        ObjectMapper failingObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, failingObjectMapper, "http://localhost:1", "secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(JsonSerializingException.class);
    }

    @Test
    void startGenerateJob_shouldThrowApiException_onErrorStatus() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        startServer(502, "text/plain", "error code: 502", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("502 BAD_GATEWAY")
                .hasMessageContaining("error code: 502");
    }

    @Test
    void startGenerateJob_shouldThrowWhenResponseIsEmpty() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void startGenerateJob_shouldThrowWhenResponseIsInvalid() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        startServer(200, "application/json", "{\"jobId\":\"\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startGenerateJob_shouldThrowWhenResponseJobIdIsNull() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        startServer(200, "application/json", "{\"jobId\":null,\"status\":\"PENDING\"}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startGenerateJob_shouldThrowWhenResponseStatusIsNull() throws Exception {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        startServer(200, "application/json", "{\"jobId\":\"job-123\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startGenerateJob_shouldThrowTimeoutException_onConnectionFailure() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setContentMarkdown("Lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(java.util.Optional.of(lesson));

        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.startGenerateJob(lessonId, 5))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout generare AI");
    }

    @Test
    void getGenerateJobStatus_shouldReturnQuestions() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"job-123\",\"status\":\"DONE\",\"questions\":[{\"text\":\"Q1\",\"type\":\"MULTIPLE_CHOICE\",\"answers\":[\"A\",\"B\"],\"correctAnswers\":[\"A\"],\"difficulty\":0.5}]}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/generate/jobs/job-123");
            assertThat(capture.method()).isEqualTo("GET");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiGenerateJobStatusResponse response = client.getGenerateJobStatus("job-123");

        assertThat(response.getJobId()).isEqualTo("job-123");
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.DONE);
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getType()).isEqualTo(QuestionType.MULTI_CHOICE);
    }

    @Test
    void getGenerateJobStatus_shouldReturnFailedPayload() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"job-123\",\"status\":\"FAILED\",\"error\":\"invalid\"}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/generate/jobs/job-123");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiGenerateJobStatusResponse response = client.getGenerateJobStatus("job-123");

        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(response.getError()).isEqualTo("invalid");
    }

    @Test
    void getGenerateJobStatus_shouldThrowWhenResponseIsEmpty() throws Exception {
        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getGenerateJobStatus_shouldThrowWhenResponseIsInvalid() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\" \",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getGenerateJobStatus_shouldThrowWhenResponseJobIdIsNull() throws Exception {
        startServer(200, "application/json", "{\"jobId\":null,\"status\":\"RUNNING\"}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getGenerateJobStatus_shouldThrowWhenResponseStatusIsNull() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"job-123\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getGenerateJobStatus_shouldThrowApiException_onErrorStatus() throws Exception {
        startServer(500, "text/plain", "generate status error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("500 INTERNAL_SERVER_ERROR")
                .hasMessageContaining("generate status error");
    }

    @Test
    void getGenerateJobStatus_shouldThrowTimeoutException_onConnectionFailure() {
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.getGenerateJobStatus("job-123"))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout status AI");
    }

    @Test
    void requestAdaptiveExercises_shouldReturnExercises() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"exercises\":[{\"exerciseId\":\"e1\",\"text\":\"T\",\"type\":\"MULTIPLE_CHOICE\",\"answers\":[\"A\",\"B\"],\"correctAnswers\":[\"A\"],\"difficulty\":0.4}]}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/adaptive/exercises");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
            assertThat(capture.body()).contains(studentId.toString());
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiAdaptiveResponse response = client.requestAdaptiveExercises(UUID.randomUUID(), studentId, 1, 2, 3);

        assertThat(response.getExercises()).hasSize(1);
        assertThat(response.getExercises().get(0).getType()).isEqualTo(QuestionType.MULTI_CHOICE);
    }

    @Test
    void requestAdaptiveExercises_shouldThrowWhenSerializationFails() throws Exception {
        UUID studentId = UUID.randomUUID();
        ObjectMapper failingObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, failingObjectMapper, "http://localhost:1", "secret", 2000, 2000, 2000, 2000);
        UUID lessonId = UUID.randomUUID();

        assertThatThrownBy(() -> client.requestAdaptiveExercises(lessonId, studentId, 1, 2, 3))
                .isInstanceOf(JsonSerializingException.class);
    }

    @Test
    void requestAdaptiveExercises_shouldThrowApiException_onErrorStatus() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(500, "text/plain", "AI error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        UUID lessonId = UUID.randomUUID();

        assertThatThrownBy(() -> client.requestAdaptiveExercises(lessonId, studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("Serviciul AI indisponibil");
    }

    @Test
    void requestAdaptiveExercises_shouldThrowTimeoutException_onConnectionFailure() {
        UUID studentId = UUID.randomUUID();
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.requestAdaptiveExercises(UUID.randomUUID(), studentId, 1, 2, 3))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout AI Adaptive");
    }

    @Test
    void startAdaptiveJob_shouldReturnJobData() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"jobId\":\"adaptive-job-123\",\"status\":\"PENDING\"}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/adaptive/exercises/jobs");
            assertThat(capture.method()).isEqualTo("POST");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
            assertThat(capture.body()).contains(studentId.toString());
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiAdaptiveJobResponse response = client.startAdaptiveJob(studentId, 1, 2, 3);

        assertThat(response.getJobId()).isEqualTo("adaptive-job-123");
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.PENDING);
    }

    @Test
    void startAdaptiveJob_shouldThrowWhenSerializationFails() throws Exception {
        UUID studentId = UUID.randomUUID();
        ObjectMapper failingObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });

        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, failingObjectMapper, "http://localhost:1", "secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(JsonSerializingException.class);
    }

    @Test
    void startAdaptiveJob_shouldThrowWhenResponseIsEmpty() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void startAdaptiveJob_shouldThrowWhenResponseIsInvalid() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"jobId\":\" \",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startAdaptiveJob_shouldThrowWhenResponseJobIdIsNull() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"jobId\":null,\"status\":\"PENDING\"}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startAdaptiveJob_shouldThrowWhenResponseStatusIsNull() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"jobId\":\"adaptive-job-123\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void startAdaptiveJob_shouldThrowApiException_onErrorStatus() throws Exception {
        UUID studentId = UUID.randomUUID();
        startServer(502, "text/plain", "adaptive error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("502 BAD_GATEWAY")
                .hasMessageContaining("adaptive error");
    }

    @Test
    void startAdaptiveJob_shouldThrowTimeoutException_onConnectionFailure() {
        UUID studentId = UUID.randomUUID();
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.startAdaptiveJob(studentId, 1, 2, 3))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout adaptive job start");
    }

    @Test
    void getAdaptiveJobStatus_shouldReturnResponse() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"adaptive-job-123\",\"status\":\"RUNNING\"}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/adaptive/exercises/jobs/adaptive-job-123");
            assertThat(capture.method()).isEqualTo("GET");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiAdaptiveJobStatusResponse response = client.getAdaptiveJobStatus("adaptive-job-123");

        assertThat(response.getJobId()).isEqualTo("adaptive-job-123");
        assertThat(response.getStatus()).isEqualTo(AiRequestStatus.RUNNING);
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenResponseIsEmpty() throws Exception {
        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenResponseIsInvalid() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenResponseJobIdIsNull() throws Exception {
        startServer(200, "application/json", "{\"jobId\":null,\"status\":\"RUNNING\"}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowWhenResponseStatusIsNull() throws Exception {
        startServer(200, "application/json", "{\"jobId\":\"adaptive-job-123\",\"status\":null}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowApiException_onErrorStatus() throws Exception {
        startServer(500, "text/plain", "adaptive status error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("500 INTERNAL_SERVER_ERROR")
                .hasMessageContaining("adaptive status error");
    }

    @Test
    void getAdaptiveJobStatus_shouldThrowTimeoutException_onConnectionFailure() {
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.getAdaptiveJobStatus("adaptive-job-123"))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout adaptive job status");
    }

    @Test
    void sendAdaptiveFeedback_shouldReturnFalseOnApiErrors() throws Exception {
        startServer(500, "text/plain", "AI error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThat(client.sendAdaptiveFeedback(feedbackPayload())).isFalse();
    }

    @Test
    void sendAdaptiveFeedback_shouldWorkOnSuccess() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(204, "text/plain", "", capture -> requestBody.set(capture.body()));
        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        assertThat(client.sendAdaptiveFeedback(feedbackPayload())).isTrue();
        assertThat(requestBody.get()).contains("\"studentId\"");
        assertThat(requestBody.get()).contains("\"subjectId\":3");
        assertThat(requestBody.get()).contains("\"topicId\":1215");
        assertThat(requestBody.get()).contains("\"results\"");
    }

    @Test
    void registerStudent_shouldReturnResponseOnSuccess() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"requestId\":\"" + requestId + "\",\"status\":\"ok\",\"message\":\"done\"}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/students");
            assertThat(capture.headers().getFirst("X-Request-Id")).isEqualTo(requestId.toString());
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        AiStudentRegistrationResponse response = client.registerStudent(requestId, studentId);

        assertThat(response.requestId()).isEqualTo(requestId.toString());
        assertThat(response.status()).isEqualTo("ok");
    }

    @Test
    void registerStudent_shouldThrowWhenResponseBodyIsEmpty() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.registerStudent(requestId, studentId))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void registerStudent_shouldThrowWhenPayloadStatusIsNotOk() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        startServer(200, "application/json", "{\"requestId\":\"" + requestId + "\",\"status\":\"error\",\"message\":\"bad\"}", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.registerStudent(requestId, studentId))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("status payload");
    }

    @Test
    void registerStudent_shouldThrowApiException_onErrorStatus() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        startServer(502, "text/plain", "error code: 502", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        assertThatThrownBy(() -> client.registerStudent(requestId, studentId))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("502 BAD_GATEWAY")
                .hasMessageContaining("error code: 502");
    }

    @Test
    void registerStudent_shouldThrowTimeoutException_onConnectionFailure() {
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.registerStudent(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout student registration AI");
    }

    @Test
    void getCurriculumCatalog_shouldReturnResponse() throws Exception {
        startServer(200, "application/json", "{\"subjects\":[{\"subjectId\":1,\"subjectName\":\"Math\"}],\"topics\":[{\"topicId\":2,\"subjectId\":1,\"subjectName\":\"Math\",\"grade\":9,\"topicName\":\"Algebra\"}]}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/catalog/curriculum");
            assertThat(capture.headers().getFirst("X-API-Key")).isEqualTo("secret");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        CurriculumCatalogRequestDto dto = new CurriculumCatalogRequestDto(9, 1, 2);
        CurriculumCatalogResponseDto response = client.getCurriculumCatalog(dto);

        assertThat(response.getSubjects()).hasSize(1);
        assertThat(response.getTopics()).hasSize(1);
    }

    @Test
    void getCurriculumCatalog_shouldThrowWhenResponseIsEmpty() throws Exception {
        startServer(204, "text/plain", "", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        CurriculumCatalogRequestDto dto2 = new CurriculumCatalogRequestDto(9, 1, 2);
        assertThatThrownBy(() -> client.getCurriculumCatalog(dto2))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getCurriculumCatalog_shouldThrowApiException_onErrorStatus() throws Exception {
        startServer(500, "text/plain", "AI error", capture -> { });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);

        CurriculumCatalogRequestDto dto3 = new CurriculumCatalogRequestDto(9, 1, 2);
        assertThatThrownBy(() -> client.getCurriculumCatalog(dto3))
                .isInstanceOf(AiApiException.class)
                .hasMessageContaining("AI API Error");
    }

    @Test
    void getCurriculumCatalog_shouldOmitNullQueryParameters() throws Exception {
        startServer(200, "application/json", "{\"subjects\":[],\"topics\":[]}", capture -> {
            assertThat(capture.path()).isEqualTo("/ai/api/v1/catalog/curriculum");
        });

        AiApiClient client = newClient("secret", 2000, 2000, 2000, 2000);
        CurriculumCatalogResponseDto response = client.getCurriculumCatalog(new CurriculumCatalogRequestDto(null, null, null));

        assertThat(response.getSubjects()).isEmpty();
        assertThat(response.getTopics()).isEmpty();
    }

    @Test
    void getCurriculumCatalog_shouldThrowTimeoutException_onConnectionFailure() {
        AiApiClient client = new AiApiClient(RestClient.builder(), lessonRepository, objectMapper, "http://localhost:1", "secret", 100, 100, 100, 100);

        assertThatThrownBy(() -> client.getCurriculumCatalog(new CurriculumCatalogRequestDto(9, 1, 2)))
                .isInstanceOf(org.elearning.backend.ai.exception.AiTimeoutException.class)
                .hasMessageContaining("Timeout AI");
    }

    @Test
    void readErrorResponseBodyReturnsEmptyMarkerWhenBodyIsBlank() throws Exception {
        ClientHttpResponse response = org.mockito.Mockito.mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(new java.io.ByteArrayInputStream("   ".getBytes(StandardCharsets.UTF_8)));

        assertThat(AiApiClient.readErrorResponseBody(response)).isEqualTo("<empty>");
    }

    @Test
    void readErrorResponseBodyReturnsUnreadableMarkerWhenBodyReadFails() throws Exception {
        ClientHttpResponse response = org.mockito.Mockito.mock(ClientHttpResponse.class);
        when(response.getBody()).thenThrow(new IOException("boom"));

        assertThat(AiApiClient.readErrorResponseBody(response)).isEqualTo("<unreadable>");
    }

    private AiApiClient newClient(String apiKey, int generateTimeout, int feedbackTimeout, int adaptiveTimeout, int studentRegistrationTimeout) {
        return new AiApiClient(
                RestClient.builder(),
                lessonRepository,
                objectMapper,
                baseUrl(),
                apiKey,
                generateTimeout,
                feedbackTimeout,
                adaptiveTimeout,
                studentRegistrationTimeout
        );
    }

    private String baseUrl() {
        if (server == null) {
            return "http://localhost:1";
        }
        return "http://localhost:" + server.getAddress().getPort();
    }

    private AiFeedbackPayloadDto feedbackPayload() {
        return new AiFeedbackPayloadDto(
                UUID.fromString("3e4c7df2-7024-4fa8-9266-430d4d67a0c4"),
                3,
                1215,
                List.of(new FeedbackResultDto("ai-1215035", 0.0, 1))
        );
    }

    private void startServer(int statusCode, String contentType, String responseBody, Consumer<RequestCapture> requestAssertion) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", new FixedResponseHandler(statusCode, contentType, responseBody, requestAssertion));
        server.start();
    }

    private record RequestCapture(String method, String path, String body, com.sun.net.httpserver.Headers headers) {
    }

    private static class FixedResponseHandler implements HttpHandler {
        private final int statusCode;
        private final String contentType;
        private final String responseBody;
        private final Consumer<RequestCapture> requestAssertion;

        private FixedResponseHandler(int statusCode, String contentType, String responseBody, Consumer<RequestCapture> requestAssertion) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.responseBody = responseBody;
            this.requestAssertion = requestAssertion;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requestAssertion.accept(new RequestCapture(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(),
                        requestBody,
                        exchange.getRequestHeaders()
                ));

                if (statusCode == 204) {
                    exchange.sendResponseHeaders(statusCode, -1);
                    return;
                }

                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bytes);
                }
            } finally {
                exchange.close();
            }
        }
    }
}
