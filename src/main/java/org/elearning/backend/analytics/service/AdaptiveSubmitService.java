package org.elearning.backend.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.ai.service.AiApiClient;
import org.elearning.backend.analytics.dto.*;
import org.elearning.backend.analytics.exception.ResourceConflictException;
import org.elearning.backend.analytics.exception.ValidationException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveSubmitService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiApiClient aiApiClient;

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
    @Transactional
    public AdaptiveResultDto submitSession(UUID sessionId, UUID studentId, AdaptiveSubmitRequestDto studentAnswers) {

        String sessionSql = "SELECT subject_id, topic_id, status, expires_at FROM adaptive_sessions WHERE id = ? AND student_id = ?";
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, sessionId, studentId);

        if (sessions.isEmpty()) {
            throw new DoesNotExistException("The session with ID " + sessionId + " does not exist for the student.");
        }

        Map<String, Object> session = sessions.get(0);
        String status = (String) session.get("status");

        if (!"ACTIVE".equals(status)) {
            throw new ResourceConflictException("The session is not active and cannot be submitted. Current status: " + status);
        }

        java.sql.Timestamp expiresAtSql = (java.sql.Timestamp) session.get("expires_at");
        LocalDateTime expiresAt = expiresAtSql.toLocalDateTime();

        if (LocalDateTime.now().isAfter(expiresAt)) {
            jdbcTemplate.update("UPDATE adaptive_sessions SET status = 'EXPIRED' WHERE id = ?", sessionId);
            throw new ResourceConflictException("The session has expired and cannot be submitted. Expiration time was: " + expiresAt);
        }

        Integer subjectId = (Integer) session.get("subject_id");
        Integer topicId = (Integer) session.get("topic_id");

        String exercisesSql = "SELECT id, ml_exercise_id, exercise_type, correct_answers_raw::text FROM adaptive_session_exercises WHERE session_id = ?";
        List<Map<String, Object>> dbExercises = jdbcTemplate.queryForList(exercisesSql, sessionId);

        List<FeedbackResultDto> mlFeedbackResults = new ArrayList<>();
        List<ClientResultDto> clientResults = new ArrayList<>();
        double totalScore = 0.0;

        for (Map<String, Object> dbExercise : dbExercises) {

            UUID dbExerciseUuid = (UUID) dbExercise.get("id");
            String mlExerciseId = (String) dbExercise.get("ml_exercise_id");
            String type = (String) dbExercise.get("exercise_type");
            List<String> correctAnswers;

            try {
                correctAnswers = objectMapper.readValue((String) dbExercise.get("correct_answers_raw"), new TypeReference<List<String>>() {});
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

            jdbcTemplate.update("INSERT INTO adaptive_session_answers (session_id, exercise_id, given_answers, score, time_spent) VALUES (?, ?, ?::jsonb, ?, ?)",
                    sessionId, dbExerciseUuid, convertToJson(givenAnswers), score, timeSpent);

            mlFeedbackResults.add(new FeedbackResultDto(mlExerciseId, score, timeSpent));
            clientResults.add(new ClientResultDto(mlExerciseId, score == 1.0, score, correctAnswers, givenAnswers));
        }

        jdbcTemplate.update("UPDATE adaptive_sessions SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?", sessionId);

        boolean feedbackSent = false;
        try {
            AiFeedbackPayloadDto payload = new AiFeedbackPayloadDto(studentId, subjectId, topicId, mlFeedbackResults);
            aiApiClient.sendAdaptiveFeedback(payload);

            jdbcTemplate.update("UPDATE adaptive_sessions SET ai_feedback_sent = true WHERE id = ?", sessionId);
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
            // If the user selected any incorrect option (given size > intersection size) -> 0.0
            if (!intersection.isEmpty() && givenSet.size() == intersection.size() && givenSet.size() < correctSet.size()) {
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
