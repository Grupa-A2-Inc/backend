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

    /**
     * This method processes the submission of an adaptive learning session by a student. It performs the following steps:
     * 1. Validates that the session exists and is active for the given student.
     * 2. Retrieves the exercises associated with the session and their correct answers.
     * 3. Compares the student's submitted answers with the correct answers to calculate scores for each exercise.
     * 4. Stores the student's answers and scores in the database.
     * 5. Updates the session status to "COMPLETED".
     * 6. Sends feedback data to an AI service and updates the session record if feedback was sent successfully.
     *
     * @param sessionId The ID of the adaptive session being submitted.
     * @param studentId The ID of the student submitting the session.
     * @param studentAnswers The answers provided by the student for each exercise in the session.
     * @return An AdaptiveResultDto containing the total score, detailed results for each exercise, and whether AI feedback was sent.
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
     * This method calculates the score for a given exercise based on the type of exercise, the answers provided by the student, and the correct answers.
     * The scoring logic is as follows:
     * - For SINGLE_CHOICE and TRUE_FALSE: 1 point if the given answer matches the correct answer, otherwise 0 points.
     * - For MULTIPLE_CHOICE:
     *   - 1 point if all given answers match all correct answers (order does not matter).
     *   - 0.5 points if the given answers are a subset of the correct answers (i.e., all given answers are correct but not all correct answers are given).
     *   - 0 points if any given answer is incorrect or if there are extra incorrect answers.
     *
     * @param type The type of exercise (e.g., SINGLE_CHOICE, TRUE_FALSE, MULTIPLE_CHOICE).
     * @param given The list of answers provided by the student.
     * @param correct The list of correct answers for the exercise.
     * @return The calculated score for the exercise.
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
     * This method converts a given object to its JSON string representation using the ObjectMapper.
     * If the object is null, it returns an empty JSON array "[]".
     * If there is an error during serialization, it logs the error and throws a ValidationException.
     *
     * @param object The object to be converted to JSON.
     * @return The JSON string representation of the object.
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
