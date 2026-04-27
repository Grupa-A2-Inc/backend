package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InjectionResultDto {
    private UUID testId;
    private boolean testCreated;
    private int injectedCount;
    private int newTotalQuestions;
    private UUID lessonId;
}
