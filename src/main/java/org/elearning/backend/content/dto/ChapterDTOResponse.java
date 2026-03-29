package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elearning.backend.content.model.Chapter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChapterDTOResponse {
    private UUID id = null;
    private String title = null;
    private Integer orderIndex = null;

    public ChapterDTOResponse(Chapter chapter) {
        this.id = chapter.getId();
        this.title = chapter.getTitle();
        this.orderIndex = chapter.getOrderIndex();
    }
}
