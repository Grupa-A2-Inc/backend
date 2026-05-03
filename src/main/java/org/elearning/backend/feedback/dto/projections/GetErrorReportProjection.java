package org.elearning.backend.feedback.dto.projections;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GetErrorReportProjection {
    UUID getId();
    Integer getQuestionId();
    UUID getStudentId();
    String getDescription();
    String getStatus();
    LocalDateTime getResolvedAt();
    UUID getResolvedBy();
    LocalDateTime getCreatedAt();
    String getContent();
    String getQuestionSource();
    String getLessonTitle();
    String getCourseTitle();
}