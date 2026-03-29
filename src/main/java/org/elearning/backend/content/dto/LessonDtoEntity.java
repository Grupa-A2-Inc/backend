package org.elearning.backend.content.dto;

import lombok.Getter;
import org.elearning.backend.content.model.Lesson;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class LessonDtoEntity {

    private final UUID id;
    private final UUID chapterID;
    private final String title;
    private final String contentMarkdown;
    private final int orderIndex;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public LessonDtoEntity(Lesson lesson){
        this.id = lesson.getId();
        this.chapterID = lesson.getChapter().getId();
        this.title = lesson.getTitle();
        this.contentMarkdown = lesson.getContentMarkdown();
        this.orderIndex = lesson.getOrderIndex();
        this.createdAt = lesson.getCreatedAt();
        this.updatedAt = lesson.getUpdatedAt();
    }


}
