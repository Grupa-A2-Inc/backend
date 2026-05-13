package org.elearning.backend.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class AiGenerateResponse {
    private List<AiQuestionDto> questions;
}