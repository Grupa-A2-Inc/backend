package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AdaptiveSubmitRequestDto {

    private List<AnswerDto> answers;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class AnswerDto {
        private String exerciseId;
        private List<String> givenAnswers;
        private Integer timeSpent;
    }
}
