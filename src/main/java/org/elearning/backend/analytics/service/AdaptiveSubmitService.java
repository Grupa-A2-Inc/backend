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
import org.elearning.backend.assessment.exception.TimerExpiredException;
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
