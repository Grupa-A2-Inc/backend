package org.elearning.backend.content.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for transferring course data in GET requests.
 * This DTO is used to represent the data of a course along with its chapters and lessons when it is retrieved from the backend.
 */
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseFullViewDto {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    private List<ChapterFullViewDTO> chapters;

    @Getter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterFullViewDTO {
        private UUID id;
        private UUID courseId;
        private String title;
        private int orderIndex;

        private List<LessonFullViewDTO> lessons;
    }

    @Getter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonFullViewDTO {
        private UUID id;
        private  UUID chapterId;
        private String title;
        private String contentMarkdown;
        private int orderIndex;

        private List<LessonResourceDtoGet> lessonResources;
    }
}