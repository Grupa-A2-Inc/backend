// dto/StartAttemptResponseDTO.java
package org.elearning.backend.assessment.dto.test_dto;

import lombok.*;
import org.elearning.backend.assessment.dto.question_dto.QuestionForStudentDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class StartAttemptResponseDto {
    private UUID attemptId;
    private int attemptNumber;
    private LocalDateTime startedAt;
    private int timeLimitSec;
    private TestInfoForAttemptDto test;
    private List<QuestionForStudentDto> questions;

    @AllArgsConstructor @NoArgsConstructor
    @Getter
    @Setter
    public static class TestInfoForAttemptDto {
        private UUID id;
        private String title;
    }
}