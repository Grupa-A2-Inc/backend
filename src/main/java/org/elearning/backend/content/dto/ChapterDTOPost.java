package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elearning.backend.content.model.Chapter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChapterDTOPost {
    private String title = null;
    private Integer orderIndex = null;

    public ChapterDTOPost(Chapter chapter) {
        this.title = chapter.getTitle();
        this.orderIndex = chapter.getOrderIndex();
    }
}
