package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CurriculumCatalogResponseDto {

    private List<SubjectDto> subjects;
    private List<TopicsDto> topics;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class SubjectDto {
        private int subjectId;
        private String subjectName;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class TopicsDto {
        private int topicId;
        private int subjectId;
        private String subjectName;
        private int grade;
        private String topicName;
    }
}
