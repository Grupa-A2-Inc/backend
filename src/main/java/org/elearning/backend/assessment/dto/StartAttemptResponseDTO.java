// dto/StartAttemptResponseDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder
public class StartAttemptResponseDTO {
    private UUID attemptId;
    private LocalDateTime startedAt;
    private int timeLimitSec;
    private List<QuestionForStudentDTO> questions;
}