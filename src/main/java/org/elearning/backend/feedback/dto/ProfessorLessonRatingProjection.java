package org.elearning.backend.feedback.dto;

import java.util.UUID;

public interface ProfessorLessonRatingProjection {
    UUID getId();
    String getTitle();
    Double getAvgRating();
    Long getTotalRatings();
}
