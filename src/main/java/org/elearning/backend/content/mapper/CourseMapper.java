package org.elearning.backend.content.mapper;

import org.elearning.backend.content.dto.CourseDtoGet;
import org.elearning.backend.content.dto.CreateCourseDto;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {LessonResourceMapper.class})
public interface CourseMapper {
    Lesson toLesson(CreateCourseDto.CreateLessonDTO lessonDtoPost);
    Chapter toChapter(CreateCourseDto.CreateChapterDTO chapterDtoPost);
    Course toCourse(CreateCourseDto courseDtoPost);

    CourseDtoGet toCourseDtoGet(Course course);
}
