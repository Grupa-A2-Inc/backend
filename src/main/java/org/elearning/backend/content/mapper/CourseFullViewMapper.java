package org.elearning.backend.content.mapper;

import org.elearning.backend.content.dto.CourseFullViewDto;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper interface for converting between Course entities and their corresponding CourseFullViewDTOs.
 * This interface uses MapStruct to automatically generate the implementation for mapping between different object types.
 * It includes a method for converting a Course entity to a CourseFullViewDTO, which is a detailed view of the course including its chapters and lessons.
 */
@Mapper(componentModel = "spring", uses = {LessonResourceMapper.class})
public interface CourseFullViewMapper {

    /**
     * Maps a Course entity to a CourseFullViewDTO.
     *
     * @param course the Course entity to be mapped
     * @return the corresponding CourseFullViewDTO
     */
    CourseFullViewDto toCourseFullViewDTO(Course course);

    /**
     * Maps a Chapter entity to a CourseFullViewDTO.ChapterDTO.
     *
     * @param chapter the Chapter entity to be mapped
     * @return the corresponding CourseFullViewDTO.ChapterDTO
     */
    @Mapping(source = "course.id", target = "courseId")
    CourseFullViewDto.ChapterFullViewDTO toChapterDTO(Chapter chapter);

    /**
     * Maps a Lesson entity to a CourseFullViewDTO.LessonDTO.
     *
     * @param lesson the Lesson entity to be mapped
     * @return the corresponding CourseFullViewDTO.LessonDTO
     */
    @Mapping(source = "chapter.id", target = "chapterId")
    CourseFullViewDto.LessonFullViewDTO toLessonDTO(Lesson lesson);
}
