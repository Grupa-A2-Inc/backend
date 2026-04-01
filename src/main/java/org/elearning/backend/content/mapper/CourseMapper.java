package org.elearning.backend.content.mapper;

import org.elearning.backend.content.dto.ResponseCourseDto;
import org.elearning.backend.content.dto.CreateCourseDto;
import org.elearning.backend.content.dto.UpdateCourseDto;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LessonResourceMapper.class})
public interface CourseMapper {
    /**
     * Maps a CreateCourseDto to a Course entity.
     * Do not remove toLesson and toChapter methods, they are used by MapStruct to map the nested objects in CreateCourseDto.
     *
     * @param lessonDtoPost The CreateCourseDto.CreateLessonDTO object containing the data to be mapped.
     * @return A Lesson entity containing the mapped data from the CreateCourseDto.CreateLessonDTO object.
     */
    Lesson toLesson(CreateCourseDto.CreateLessonDTO lessonDtoPost);
    Chapter toChapter(CreateCourseDto.CreateChapterDTO chapterDtoPost);
    Course toCourse(CreateCourseDto courseDtoPost);

    ResponseCourseDto toCourseDtoGet(Course course);

    List<ResponseCourseDto> toCourseDtoGetList(List<Course> course);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "chapters", ignore = true)
    void updateCourseFromDto(UpdateCourseDto dto, @MappingTarget Course entity);
}
