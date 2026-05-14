package org.elearning.backend.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGenerateResponse {
    private List<AiQuestionDto> questions;
}