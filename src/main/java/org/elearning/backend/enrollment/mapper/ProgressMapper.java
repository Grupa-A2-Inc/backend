package org.elearning.backend.enrollment.mapper;

import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.enrollment.dto.LessonStatusDto;
import org.elearning.backend.enrollment.model.LessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgressMapper {
    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "title", source = "lesson.title")
    @Mapping(target = "visited", expression = "java(progress != null)")
    @Mapping(target = "visitedAt", expression = "java(progress != null ? progress.getVisitedAt() : null)")
    LessonStatusDto toLessonStatusDto(Lesson lesson, LessonProgress progress);
}
