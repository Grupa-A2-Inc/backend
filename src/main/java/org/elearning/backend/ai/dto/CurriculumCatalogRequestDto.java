package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CurriculumCatalogRequestDto {
    private Integer grade;
    private Integer subjectId;
    private Integer topicId;
}
