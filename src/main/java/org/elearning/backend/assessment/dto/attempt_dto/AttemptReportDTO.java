package org.elearning.backend.assessment.dto.attempt_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.dto.question_dto.QuestionForAttemptReportDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Setter
@Getter
@Builder
public class AttemptReportDTO {
    private UUID attemptId;
    private BigDecimal score;
    private BigDecimal scorePercent;
    private boolean passed;
    private LocalDateTime completedAt;
    private List<QuestionForAttemptReportDTO> question;
}
