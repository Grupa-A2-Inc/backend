package org.elearning.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.dto.*;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.exception.AdaptiveServiceUnavailableException;
import org.elearning.backend.ai.exception.ResourceConflictException;
import org.elearning.backend.ai.exception.ValidationException;
import org.elearning.backend.ai.model.AdaptiveSession;
import org.elearning.backend.ai.model.AdaptiveSessionAnswer;
import org.elearning.backend.ai.model.AdaptiveSessionExercise;
import org.elearning.backend.ai.repository.AdaptiveSessionAnswerRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionExerciseRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionRepository;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveSessionService {
    private final AdaptiveSessionRepository adaptiveSessionRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;
    private final AdaptiveSessionExerciseRepository exerciseRepository;
    private final AdaptiveSessionAnswerRepository adaptiveSessionAnswerRepository;
    private static final int SESSION_MINUTES = 30;

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
                .expiresAt(LocalDateTime.now().plusMinutes(SESSION_MINUTES))
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

    /**
     * Process a student's submission for an adaptive learning session.
     *
     * Validates the session belongs to the student, is active, and not expired; grades each exercise, persists answers and scores,
     * marks the session as completed, and attempts to send AI feedback (best-effort).
     *
     * @param sessionId the adaptive session ID being submitted
     * @param studentId the student submitting the session
     * @param studentAnswers the student's answers for the session's exercises
     * @return an AdaptiveResultDto containing the session ID, total score, per-exercise results, and whether AI feedback was sent
     * @throws DoesNotExistException if no session exists for the given sessionId and studentId
     * @throws ResourceConflictException if the session is not active or has expired
     * @throws ValidationException if stored correct answers cannot be parsed or if serialization of given answers fails
     */
    @Transactional(noRollbackFor = ResourceConflictException.class)
    public AdaptiveResultDto submitSession(UUID sessionId, UUID studentId, AdaptiveSubmitRequestDto studentAnswers) {

        AdaptiveSession session = adaptiveSessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new DoesNotExistException("The session with ID " + sessionId + " does not exist for the student."));

        String status = session.getStatus();

        if (!"ACTIVE".equals(status)) {
            throw new ResourceConflictException("The session is not active and cannot be submitted. Current status: " + status);
        }

        LocalDateTime expiresAt = session.getExpiresAt();

        if (LocalDateTime.now().isAfter(expiresAt)) {
            session.setStatus("EXPIRED");
            adaptiveSessionRepository.save(session);
            throw new ResourceConflictException("The session has expired and cannot be submitted. Expiration time was: " + expiresAt);
        }

        Integer subjectId = session.getSubjectId();
        Integer topicId = session.getTopicId();

        List<AdaptiveSessionExercise> dbExercises = exerciseRepository.findAllBySessionId(sessionId);

        List<FeedbackResultDto> mlFeedbackResults = new ArrayList<>();
        List<ClientResultDto> clientResults = new ArrayList<>();
        double totalScore = 0.0;

        for (AdaptiveSessionExercise dbExercise : dbExercises) {

            UUID dbExerciseUuid = dbExercise.getId();
            String mlExerciseId = dbExercise.getMlExerciseId();
            String type = dbExercise.getExerciseType();
            List<String> correctAnswers;

            try {
                correctAnswers = objectMapper.readValue(dbExercise.getCorrectAnswersRaw(), new TypeReference<List<String>>() {});
            } catch (Exception exception) {
                throw new ValidationException("Error parsing correct answers from database for exercise " + mlExerciseId + ": " + exception.getMessage());
            }

            Optional<AdaptiveSubmitRequestDto.AnswerDto> optionalGiven = studentAnswers.getAnswers().stream()
                    .filter(answer -> answer.getExerciseId().equals(mlExerciseId))
                    .findFirst();

            double score = 0.0;
            List<String> givenAnswers = new ArrayList<>();
            Integer timeSpent = 0;

            if (optionalGiven.isPresent()) {
                AdaptiveSubmitRequestDto.AnswerDto given = optionalGiven.get();
                givenAnswers = given.getGivenAnswers();
                timeSpent = given.getTimeSpent();
                score = calculateScore(type, givenAnswers, correctAnswers);
            }

            totalScore += score;

            adaptiveSessionAnswerRepository.save(AdaptiveSessionAnswer.builder()
                    .sessionId(sessionId)
                    .exerciseId(dbExerciseUuid)
                    .givenAnswers(convertToJson(givenAnswers))
                    .score(BigDecimal.valueOf(score))
                    .timeSpent(timeSpent)
                    .build());

            mlFeedbackResults.add(new FeedbackResultDto(mlExerciseId, score, timeSpent));
            clientResults.add(new ClientResultDto(mlExerciseId, score == 1.0, score, correctAnswers, givenAnswers));
        }

        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        adaptiveSessionRepository.save(session);

        boolean feedbackSent = false;
        try {
            AiFeedbackPayloadDto payload = new AiFeedbackPayloadDto(studentId, subjectId, topicId, mlFeedbackResults);
            aiApiClient.sendAdaptiveFeedback(payload);

            session.setAiFeedbackSent(true);
            adaptiveSessionRepository.save(session);
            feedbackSent = true;
        } catch (Exception exception) {
            log.warn("Error sending AI feedback for session {}: {}", sessionId, exception.getMessage());
        }

        return new AdaptiveResultDto(sessionId, totalScore, clientResults, feedbackSent);
    }

    /**
     * Compute the score for an exercise based on its type and the provided answers.
     *
     * Scoring:
     * - SINGLE_CHOICE, TRUE_FALSE: 1.0 if the first given answer equals the first correct answer, 0.0 otherwise.
     * - MULTIPLE_CHOICE: 1.0 if the sets of given and correct answers are equal; 0.5 if given answers are a non-empty subset of correct answers; 0.0 otherwise.
     *
     * @param type    the exercise type identifier (e.g., "SINGLE_CHOICE", "TRUE_FALSE", "MULTIPLE_CHOICE")
     * @param given   the answers provided by the student (may be null or empty)
     * @param correct the correct answers for the exercise (may be null or empty)
     * @return        the score for the exercise: `1.0`, `0.5`, or `0.0` depending on correctness
     */
    private double calculateScore(String type, List<String> given, List<String> correct) {
        if (given == null || given.isEmpty() || correct == null || correct.isEmpty()) {
            return 0.0;
        }

        if ("SINGLE_CHOICE".equals(type) || "TRUE_FALSE".equals(type)) {
            return given.get(0).equals(correct.get(0)) ? 1.0 : 0.0;
        }

        if ("MULTIPLE_CHOICE".equals(type)) {
            Set<String> givenSet = new HashSet<>(given);
            Set<String> correctSet = new HashSet<>(correct);

            if (givenSet.equals(correctSet)) {
                return 1.0;
            }

            Set<String> intersection = new HashSet<>(givenSet);
            intersection.retainAll(correctSet);

            // If the user selected only correct options but not all of them (e.g., selected 1 out of 2 correct) -> 0.5
            if (!intersection.isEmpty() && givenSet.size() == intersection.size()) {
                return 0.5;
            }

            return 0.0;
        }

        return 0.0;
    }

    /**
     * Serialize the given object to its JSON string representation.
     *
     * If the provided object is null, returns the JSON empty array string "[]".
     *
     * @param object the object to serialize; may be null
     * @return the JSON string representation of the object, or "[]" if the object is null
     * @throws ValidationException if serialization fails
     */
    private String convertToJson(Object object) {
        if (object == null) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception exception) {
            log.warn("Error serializing object to JSON: {}", exception.getMessage());
            throw new ValidationException("Internal error: Unable to serialize object to JSON");
        }
    }
}
