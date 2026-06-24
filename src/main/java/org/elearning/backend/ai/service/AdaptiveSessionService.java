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
import org.elearning.backend.ai.model.AdaptiveExerciseJob;
import org.elearning.backend.ai.model.AdaptiveSession;
import org.elearning.backend.ai.model.AdaptiveSessionAnswer;
import org.elearning.backend.ai.model.AdaptiveSessionExercise;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.repository.AdaptiveExerciseJobRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionAnswerRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionExerciseRepository;
import org.elearning.backend.ai.repository.AdaptiveSessionRepository;
import org.elearning.backend.assessment.model.QuestionType;
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
    private final AdaptiveExerciseJobRepository adaptiveExerciseJobRepository;
    private final AdaptiveSessionRepository adaptiveSessionRepository;
    private final AiApiClient aiApiClient;
    private final ObjectMapper objectMapper;
    private final AdaptiveSessionExerciseRepository exerciseRepository;
    private final AdaptiveSessionAnswerRepository adaptiveSessionAnswerRepository;
    private static final int SESSION_MINUTES = 30;

    @Transactional
    public AdaptiveJobResponseDto createAdaptiveJob(UUID studentId, Integer subjectId, Integer topicId, int count) {
        AdaptiveExerciseJob job = AdaptiveExerciseJob.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .topicId(topicId)
                .questionCount(count)
                .status(AiRequestStatus.PENDING)
                .build();
        job = adaptiveExerciseJobRepository.save(job);

        try {
            AiAdaptiveJobResponse response = aiApiClient.startAdaptiveJob(studentId, subjectId, topicId, count);
            job.setAiJobId(response.getJobId());
            job.setStatus(response.getStatus());
        } catch (AiApiException | AiTimeoutException exception) {
            markJobAsFailed(job, exception.getMessage());
        }
        adaptiveExerciseJobRepository.save(job);

        AdaptiveJobResponseDto responseDto = new AdaptiveJobResponseDto();
        responseDto.setJobId(job.getId());
        responseDto.setStatus(job.getStatus());
        return responseDto;
    }

    @Transactional
    public AdaptiveJobStatusDto getAdaptiveJobStatus(UUID jobId, UUID studentId) {
        AdaptiveExerciseJob job = adaptiveExerciseJobRepository.findByIdAndStudentId(jobId, studentId)
                .orElseThrow(() -> new DoesNotExistException("Adaptive job not found"));

        if (!isTerminalStatus(job.getStatus()) && job.getAiJobId() != null) {
            syncJobWithAi(job);
            adaptiveExerciseJobRepository.save(job);
        }

        AdaptiveJobStatusDto statusDto = new AdaptiveJobStatusDto();
        statusDto.setJobId(job.getId());
        statusDto.setStatus(job.getStatus());
        statusDto.setError(job.getErrorMessage());

        if (job.getStatus() == AiRequestStatus.DONE && job.getSessionId() != null) {
            statusDto.setSession(buildAdaptiveStartDto(job.getSessionId()));
        }

        return statusDto;
    }

    @Transactional
    public AdaptiveStartDto startSession(UUID studentId, Integer subjectId, Integer topicId, int count) {
        AiAdaptiveResponse response;
        try {
            response = aiApiClient.requestAdaptiveExercises(UUID.randomUUID(), studentId, subjectId, topicId, count);
        } catch (AiApiException | AiTimeoutException exception) {
            log.error("Failed to start adaptive session: {}", exception.getMessage());
            throw new AdaptiveServiceUnavailableException("Adaptive service is currently unavailable. Please try again later.");
        }

        if (response.getExercises() == null || response.getExercises().isEmpty()) {
            log.warn("No exercises returned from AI for subjectId={}, topicId={}", subjectId, topicId);
            throw new AdaptiveServiceUnavailableException("No exercises available for the selected subject and topic. Please try again later or choose a different topic.");
        }

        return createSessionFromExercises(studentId, subjectId, topicId, response.getExercises());
    }

    private void syncJobWithAi(AdaptiveExerciseJob job) {
        try {
            AiAdaptiveJobStatusResponse response = aiApiClient.getAdaptiveJobStatus(job.getAiJobId());
            job.setStatus(response.getStatus());

            if (response.getStatus() == AiRequestStatus.DONE) {
                if (job.getSessionId() == null) {
                    if (response.getExercises() == null || response.getExercises().isEmpty()) {
                        markJobAsFailed(job, "Adaptive AI returned an invalid response.");
                        return;
                    }
                    AdaptiveStartDto session = createSessionFromExercises(
                            job.getStudentId(),
                            job.getSubjectId(),
                            job.getTopicId(),
                            response.getExercises()
                    );
                    job.setSessionId(session.getSessionId());
                }
                job.setErrorMessage(null);
                job.setResolvedAt(LocalDateTime.now());
                return;
            }

            if (response.getStatus() == AiRequestStatus.FAILED) {
                markJobAsFailed(job, response.getError());
            }
        } catch (AiApiException | AiTimeoutException exception) {
            markJobAsFailed(job, exception.getMessage());
        }
    }

    private AdaptiveStartDto createSessionFromExercises(
            UUID studentId,
            Integer subjectId,
            Integer topicId,
            List<AiAdaptiveExerciseDto> exercises
    ) {
        AdaptiveSession session = AdaptiveSession.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .topicId(topicId)
                .expiresAt(LocalDateTime.now().plusMinutes(SESSION_MINUTES))
                .build();
        session = adaptiveSessionRepository.save(session);
        UUID sessionId = session.getId();
        List<ClientExerciseDto> safeExercises = new ArrayList<>();

        for (AiAdaptiveExerciseDto aiExercise : exercises) {
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
            } catch (JsonProcessingException exception) {
                log.error("Error serializing JSON for exercise {}", aiExercise.getExerciseId(), exception);
                throw new ValidationException("Failed to process exercise data.");
            }
        }

        return new AdaptiveStartDto(sessionId, session.getExpiresAt(), safeExercises);
    }

    private AdaptiveStartDto buildAdaptiveStartDto(UUID sessionId) {
        AdaptiveSession session = adaptiveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new DoesNotExistException("Adaptive session not found"));

        List<ClientExerciseDto> safeExercises = exerciseRepository.findAllBySessionId(sessionId).stream()
                .map(this::toClientExercise)
                .toList();

        return new AdaptiveStartDto(session.getId(), session.getExpiresAt(), safeExercises);
    }

    private ClientExerciseDto toClientExercise(AdaptiveSessionExercise exercise) {
        try {
            List<String> answers = objectMapper.readValue(exercise.getAnswersRaw(), new TypeReference<List<String>>() {});
            return new ClientExerciseDto(
                    exercise.getMlExerciseId(),
                    exercise.getExerciseText(),
                    QuestionType.fromValue(exercise.getExerciseType()),
                    answers
            );
        } catch (Exception exception) {
            throw new ValidationException("Failed to read exercise data for session " + exercise.getSessionId());
        }
    }

    private boolean isTerminalStatus(AiRequestStatus status) {
        return status == AiRequestStatus.DONE || status == AiRequestStatus.FAILED;
    }

    private void markJobAsFailed(AdaptiveExerciseJob job, String errorMessage) {
        job.setStatus(AiRequestStatus.FAILED);
        job.setErrorMessage(errorMessage == null || errorMessage.isBlank()
                ? "Adaptive AI returned an invalid response."
                : errorMessage);
        job.setResolvedAt(LocalDateTime.now());
    }

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
            feedbackSent = aiApiClient.sendAdaptiveFeedback(payload);
            if (feedbackSent) {
                session.setAiFeedbackSent(true);
                adaptiveSessionRepository.save(session);
            }
        } catch (Exception exception) {
            log.warn("Error sending AI feedback for session {}: {}", sessionId, exception.getMessage());
        }

        return new AdaptiveResultDto(sessionId, totalScore, clientResults, feedbackSent);
    }

    private double calculateScore(String type, List<String> given, List<String> correct) {
        if (given == null || given.isEmpty() || correct == null || correct.isEmpty()) {
            return 0.0;
        }

        QuestionType questionType;
        try {
            questionType = QuestionType.fromValue(type);
        } catch (IllegalArgumentException exception) {
            return 0.0;
        }

        return switch (questionType) {
            case SINGLE_CHOICE, TRUE_FALSE -> given.get(0).equals(correct.get(0)) ? 1.0 : 0.0;
            case MULTI_CHOICE -> {
                Set<String> givenSet = new HashSet<>(given);
                Set<String> correctSet = new HashSet<>(correct);

                if (givenSet.equals(correctSet)) {
                    yield 1.0;
                }

                Set<String> intersection = new HashSet<>(givenSet);
                intersection.retainAll(correctSet);

                if (!intersection.isEmpty() && givenSet.size() == intersection.size()) {
                    yield 0.5;
                }

                yield 0.0;
            }
        };
    }

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
