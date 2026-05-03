package org.elearning.backend.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.feedback.model.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetErrorReportDto {

    private UUID id;
    private Integer questionId;
    private UUID studentId;
    private ReportStatus status;
    private String description;

    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private LocalDateTime createdAt;

    private String content;
    private QuestionSource questionSource;
    private String lessonTitle;
    private String courseTitle;

}