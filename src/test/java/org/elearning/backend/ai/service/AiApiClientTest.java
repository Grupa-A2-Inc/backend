package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityNotFoundException;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.JsonSerializingException;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiApiClientTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ObjectMapper objectMapper;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateTestReturnsParsedResponse() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setContentMarkdown("lesson content");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        AiApiClient client = client(startServer("/ai/api/v1/generate", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("secret", exchange.getRequestHeaders().getFirst("X-API-Key"));
            respond(exchange, 200, "{\"requestId\":\"00000000-0000-0000-0000-000000000001\",\"questions\":[]}");
        }), new ObjectMapper(), 1000);

        AiGenerateResponse response = client.generateTest(lessonId, 3);

        assertThat(response.getRequestId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.getQuestions()).isEmpty();
    }

    @Test
    void generateTestThrowsWhenLessonMissing() {
        AiApiClient client = client("http://localhost:1", new ObjectMapper(), 1000);
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> client.generateTest(lessonId, 2))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generateTestThrowsWhenLessonContentBlank() {
        AiApiClient client = client("http://localhost:1", new ObjectMapper(), 1000);
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setContentMarkdown(" ");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThatThrownBy(() -> client.generateTest(lessonId, 2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requestAdaptiveExercisesThrowsWhenSerializationFails() throws Exception {
        AiApiClient client = client("http://localhost:1", objectMapper, 1000);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("bad json") {});

        assertThatThrownBy(() -> client.requestAdaptiveExercises(UUID.randomUUID(), UUID.randomUUID(), 1, 2, 3))
                .isInstanceOf(JsonSerializingException.class)
                .hasMessage("Failed to serialize adaptive exercises request payload.");
    }

    @Test
    void requestAdaptiveExercisesReturnsResponse() throws Exception {
        AiApiClient client = client(startServer("/ai/api/v1/adaptive/exercises", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            respond(exchange, 200, "{\"exercises\":[]}");
        }), objectMapper, 1000);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"studentId\":\"s\"}");

        AiAdaptiveResponse response = client.requestAdaptiveExercises(UUID.randomUUID(), UUID.randomUUID(), 1, 2, 3);

        assertThat(response.getExercises()).isEmpty();
    }

    @Test
    void requestAdaptiveExercisesThrowsTimeout() throws Exception {
        AiApiClient client = client("http://localhost:1", objectMapper, 50);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"studentId\":\"s\"}");

        assertThatThrownBy(() -> client.requestAdaptiveExercises(UUID.randomUUID(), UUID.randomUUID(), 1, 2, 3))
                .isInstanceOf(AiTimeoutException.class);
    }

    @Test
    void registerStudentReturnsValidatedResponse() {
        UUID requestId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        AiApiClient client = client(startServer("/ai/api/v1/students", exchange -> {
            assertEquals("secret", exchange.getRequestHeaders().getFirst("X-API-Key"));
            assertEquals(requestId.toString(), exchange.getRequestHeaders().getFirst("X-Request-Id"));
            respond(exchange, 200, "{\"requestId\":\"" + requestId + "\",\"status\":\"ok\",\"message\":\"created\"}");
        }), new ObjectMapper(), 1000);

        AiStudentRegistrationResponse response = client.registerStudent(requestId, studentId);

        assertThat(response.status()).isEqualTo("ok");
    }

    @Test
    void registerStudentThrowsWhenResponseIsEmpty() {
        AiApiClient client = client(startServer("/ai/api/v1/students", exchange -> respond(exchange, 200, "null")), new ObjectMapper(), 1000);

        assertThatThrownBy(() -> client.registerStudent(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI student registration returned an empty response");
    }

    @Test
    void registerStudentThrowsWhenStatusPayloadIsNotOk() {
        UUID requestId = UUID.randomUUID();
        AiApiClient client = client(startServer("/ai/api/v1/students", exchange ->
                respond(exchange, 200, "{\"requestId\":\"" + requestId + "\",\"status\":\"failed\",\"message\":\"nope\"}")
        ), new ObjectMapper(), 1000);

        assertThatThrownBy(() -> client.registerStudent(requestId, UUID.randomUUID()))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI student registration failed with status payload: failed");
    }

    @Test
    void getCurriculumCatalogReturnsResponse() {
        AiApiClient client = client(startServer("/ai/api/v1/catalog/curriculum", exchange -> {
            assertThat(exchange.getRequestURI().getQuery()).contains("subjectId=2", "topicId=4", "grade=8");
            respond(exchange, 200, "{\"subjects\":[],\"topics\":[]}");
        }), new ObjectMapper(), 1000);

        CurriculumCatalogResponseDto response = client.getCurriculumCatalog(new CurriculumCatalogRequestDto(8, 2, 4));

        assertThat(response.getSubjects()).isEmpty();
        assertThat(response.getTopics()).isEmpty();
    }

    @Test
    void getCurriculumCatalogThrowsWhenResponseIsEmpty() {
        AiApiClient client = client(startServer("/ai/api/v1/catalog/curriculum", exchange -> respond(exchange, 200, "null")), new ObjectMapper(), 1000);

        assertThatThrownBy(() -> client.getCurriculumCatalog(new CurriculumCatalogRequestDto()))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI curriculum catalog returned an empty response");
    }

    @Test
    void readErrorResponseBodyReturnsFallbackValues() {
        MockClientHttpResponse blank = new MockClientHttpResponse("".getBytes(StandardCharsets.UTF_8), 400);

        assertThat(AiApiClient.readErrorResponseBody(blank)).isEqualTo("<empty>");
        assertThat(AiApiClient.readErrorResponseBody(new UnreadableResponse())).isEqualTo("<unreadable>");
    }

    private AiApiClient client(String baseUrl, ObjectMapper mapper, int timeoutMs) {
        return new AiApiClient(RestClient.builder(), lessonRepository, mapper, baseUrl, "secret", timeoutMs, timeoutMs, timeoutMs, timeoutMs);
    }

    private String startServer(String path, ThrowingHandler handler) {
        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext(path, exchange -> {
                try {
                    handler.handle(exchange);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return "http://localhost:" + server.getAddress().getPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static final class UnreadableResponse extends MockClientHttpResponse {
        private UnreadableResponse() {
            super(new byte[0], 500);
        }

        @Override
        public java.io.InputStream getBody() throws IOException {
            throw new IOException("cannot read");
        }
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
