package org.elearning.backend.content.mapper;

import org.elearning.backend.content.dto.LessonResourceDTOGet;
import org.elearning.backend.content.dto.LessonResourceDTOPatch;
import org.elearning.backend.content.dto.LessonResourceDTOPost;
import org.elearning.backend.content.model.LessonResource;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper interface for converting between LessonResource entities and their corresponding DTOs.
 * This interface uses MapStruct to automatically generate the implementation for mapping between different object types.
 * It includes methods for updating an existing entity from a patch DTO, converting an entity to a get DTO, and converting a post DTO to an entity.
 */
@Mapper(componentModel = "spring")
public interface LessonResourceMapper {

    /**
     * Updates an existing LessonResource entity with values from a LessonResourceDTOPatch DTO.
     * This method ignores null values in the DTO, meaning that only non-null properties will be updated in the entity.
     * The 'id', 'lesson', and 'createdAt' fields of the entity are also ignored during the update process.
     *
     * @param dto    The LessonResourceDTOPatch containing the new values for the entity.
     * @param entity The existing LessonResource entity to be updated.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateLessonResourceFromDto(LessonResourceDTOPatch dto, @MappingTarget LessonResource entity);

    /**
     * Converts a LessonResource entity to a LessonResourceDTOGet DTO.
     * The 'lesson.id' property of the entity is mapped to the 'lessonId' property of the DTO.
     *
     * @param lessonResource The LessonResource entity to be converted.
     * @return A LessonResourceDTOGet containing the mapped properties from the entity.
     */
    @Mapping(source = "lesson.id", target = "lessonId")
    LessonResourceDTOGet toLessonResourceDTOGet(LessonResource lessonResource);

    /**
     * Converts a list of LessonResource entities to a list of LessonResourceDTOGet DTOs.
     *
     * @param resources The list of LessonResource entities to be converted.
     * @return A list of LessonResourceDTOGet containing the mapped properties from the entities.
     */
    List<LessonResourceDTOGet> toLessonResourcesDTOGetList(List<LessonResource> resources);

    /**
     * Converts a LessonResourceDTOPost DTO to a LessonResource entity.
     * The 'id', 'lesson', and 'createdAt' fields of the entity are ignored during the conversion process, meaning they will not be set based on the DTO values.
     *
     * @param lessonResourceDTOPost The LessonResourceDTOPost containing the values for the new entity.
     * @return A LessonResource entity with properties mapped from the DTO, except for 'id', 'lesson', and 'createdAt'.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    LessonResource toLessonResource(LessonResourceDTOPost lessonResourceDTOPost);
}
