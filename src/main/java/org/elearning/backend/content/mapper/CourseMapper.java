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

    /**
     * Maps a Course entity to a ResponseCourseDto.
     *
     * @param course The Course entity to be mapped.
     * @return A ResponseCourseDto containing the mapped data from the Course entity.
     */
    ResponseCourseDto toCourseDtoGet(Course course);

    /**
     * Maps a list of Course entities to a list of ResponseCourseDto.
     *
     * @param course The list of Course entities to be mapped.
     * @return A list of ResponseCourseDto containing the mapped data from the list of Course entities.
     */
    List<ResponseCourseDto> toCourseDtoGetList(List<Course> course);

    /**
     * Updates an existing Course entity with data from an UpdateCourseDto.
     * This method ignores null values in the UpdateCourseDto, meaning that only non-null properties will be updated in the Course entity.
     * It also ignores certain properties such as id, createdBy, createdAt, updatedAt, and chapters to prevent unintended modifications to these fields.
     *
     * @param dto The UpdateCourseDto containing the data to update the Course entity.
     * @param entity The existing Course entity to be updated.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "chapters", ignore = true)
    void updateCourseFromDto(UpdateCourseDto dto, @MappingTarget Course entity);
}
