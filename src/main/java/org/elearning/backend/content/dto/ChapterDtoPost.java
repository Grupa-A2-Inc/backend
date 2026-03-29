package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChapterDtoPost {
    private String title = null;
    private Integer orderIndex = null;
}
