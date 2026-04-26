package org.elearning.backend.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.AiAdaptiveExerciseDto;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.service.AiApiClient;
import org.elearning.backend.analytics.dto.AdaptiveStartDto;
import org.elearning.backend.analytics.dto.ClientExerciseDto;
import org.elearning.backend.analytics.exception.AdaptiveServiceUnavailableException;
import org.elearning.backend.analytics.model.AdaptiveSession;
import org.elearning.backend.analytics.model.AdaptiveSessionExercise;
import org.elearning.backend.analytics.repository.AdaptiveSessionExerciseRepository;
import org.elearning.backend.analytics.repository.AdaptiveSessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveSessionService {
    private final AdaptiveSessionRepository adaptiveSessionRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;
    private final AdaptiveSessionExerciseRepository exerciseRepository;

    /**
     * Starts a new adaptive learning session for a student and returns its client-facing details.
     *
     * @param studentId the UUID of the student for whom the session is created
     * @param subjectId the subject identifier for the session
     * @param topicId   the topic identifier for the session
     * @param count     the number of adaptive exercises to request
     * @return an AdaptiveStartDto containing the persisted session ID, the session expiry timestamp, and the list of exercises safe to send to the client
     * @throws AdaptiveServiceUnavailableException if the external adaptive exercise service is unavailable or times out
     * @throws RuntimeException                    if exercise data cannot be serialized for persistence
     */
    public AdaptiveStartDto startSession(UUID studentId, Integer subjectId, Integer topicId, int count) {
        AiAdaptiveResponse response;
        try {
            response = aiApiClient.requestAdaptiveExercises(UUID.randomUUID(), studentId, subjectId, topicId, count);
        } catch (AiApiException | AiTimeoutException exception) {
            log.error("Failed to start adaptive session: {}", exception.getMessage());
            throw new AdaptiveServiceUnavailableException("Adaptive service is currently unavailable. Please try again later.");
        }
        AdaptiveSession session = AdaptiveSession.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .topicId(topicId)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        session = adaptiveSessionRepository.save(session);
        UUID sessionId = session.getId();
        List<ClientExerciseDto> safeExercises = new ArrayList<>();
        for (AiAdaptiveExerciseDto aiExercise : response.getExercises()) {
            try {
                String answersJson = objectMapper.writeValueAsString(aiExercise.getAnswers());
                String correctAnswersJson = objectMapper.writeValueAsString(aiExercise.getCorrectAnswers());

                AdaptiveSessionExercise exerciseEntity = AdaptiveSessionExercise.builder()
                        .sessionId(sessionId)
                        .mlExerciseId(aiExercise.getExerciseId())
                        .exerciseText(aiExercise.getText())
                        .exerciseType(aiExercise.getType().name())
                        .answersRaw(answersJson)
                        .correctAnswersRaw(correctAnswersJson)
                        .difficulty(BigDecimal.valueOf(aiExercise.getDifficulty()))
                        .build();
                exerciseRepository.save(exerciseEntity);
                safeExercises.add(new ClientExerciseDto(
                        aiExercise.getExerciseId(),
                        aiExercise.getText(),
                        aiExercise.getType(),
                        aiExercise.getAnswers()
                ));
            } catch(JsonProcessingException exception) {
                log.error("Error serializing JSON for exercise {}", aiExercise.getExerciseId(), exception);
                throw new RuntimeException("Failed to process exercise data.");
            }
        }

        return new AdaptiveStartDto(sessionId, session.getExpiresAt(), safeExercises);
    }
}
