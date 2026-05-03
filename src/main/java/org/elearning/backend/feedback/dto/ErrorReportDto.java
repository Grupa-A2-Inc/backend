package org.elearning.backend.feedback.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.feedback.model.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorReportDto {
    private UUID id;
    private Integer questionId;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
