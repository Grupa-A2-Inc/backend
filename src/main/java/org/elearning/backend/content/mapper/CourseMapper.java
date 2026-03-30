package org.elearning.backend.content.mapper;

import org.elearning.backend.content.dto.CourseDtoGet;
import org.elearning.backend.content.dto.CreateCourseDTO;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {LessonResourceMapper.class})
public interface CourseMapper {
    Lesson toLesson(CreateCourseDTO.CreateLessonDTO lessonDtoPost);
    Chapter toChapter(CreateCourseDTO.CreateChapterDTO chapterDtoPost);
    Course toCourse(CreateCourseDTO courseDtoPost);

    CourseDtoGet toCourseDtoGet(Course course);
}
