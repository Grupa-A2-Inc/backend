package org.elearning.backend.content.dto;

import lombok.*;
import org.elearning.backend.content.model.CourseStatus;

import java.util.List;

@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseDto {
    private String title;
    private String description;
    private String category;
    private CourseStatus status;

    private List<CreateChapterDTO> chapters;

    @Getter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChapterDTO {
        private String title;
        private int orderIndex;

        private List<CreateLessonDTO> lessons;
    }

    @Getter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLessonDTO {
        private String title;
        private String contentMarkdown;
        private int orderIndex;

        private List<CreateLessonResourceDto> lessonResources;
    }
}