package org.elearning.backend.ai.dto;

public record AiAdaptiveExercisesRequest(
        String studentId,
        int subjectId,
        int topicId,
        int count
) {
}
