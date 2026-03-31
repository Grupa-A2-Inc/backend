package org.elearning.backend.content.service;

import org.elearning.backend.content.dto.ResponseLessonResourceDto;
import org.elearning.backend.content.dto.UpdateLessonResourceDto;
import org.elearning.backend.content.dto.CreateLessonResourceDto;
import org.elearning.backend.content.mapper.LessonResourceMapper;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing lesson resources.
 * This class provides methods to create, update, delete, and retrieve lesson resources associated with specific lessons.
 */
@Service
public class LessonResourceService {
    private final LessonResourceRepository lessonResourceRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceMapper lessonResourceMapper;

    public LessonResourceService(LessonResourceRepository lessonResourceRepository, LessonRepository lessonRepository, LessonResourceMapper lessonResourceMapper) {
        this.lessonResourceRepository = lessonResourceRepository;
        this.lessonRepository = lessonRepository;
        this.lessonResourceMapper = lessonResourceMapper;
    }

    /**
     * Creates a new lesson resource and associates it with a specific lesson ID.
     *
     * @param CreateLessonResourceDto The LessonResource object containing the details of the resource to be created.
     * @param lessonId       The ID of the lesson to which the resource will be associated.
     * @return The created LessonResource object with its generated ID and associated lesson ID.
     * @throws ResponseStatusException NOT_FOUND if no lesson with the given ID exists
     * @throws ResponseStatusException BAD_REQUEST if the title or URL of the resource is null.
     */
    public ResponseLessonResourceDto createNewLessonResource(CreateLessonResourceDto lessonResourceDTOPost, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found with ID: " + lessonId));

        if(lessonResourceDTOPost.getTitle() == null || lessonResourceDTOPost.getUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title or URL cannot be null!");
        }

        LessonResource lessonResource = lessonResourceMapper.toLessonResource(lessonResourceDTOPost);
        lessonResource.setLesson(lesson);

        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(lessonResource));
    }

    /**
     * Deletes a lesson resource based on its ID and the associated lesson ID.
     *
     * @param resourceId The ID of the lesson resource to be deleted.
     * @param lessonId   The ID of the lesson to which the resource is associated.
     * @throws ResponseStatusException NOT_FOUND if no resource with the given ID exists or if the resource does not belong to the specified lesson.
     */
    @Transactional
    public void deleteLessonResource(UUID resourceId, UUID lessonId) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found with ID: " + resourceId));

        if (!resource.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource with ID: " + resourceId + " does not belong to lesson with ID: " + lessonId);
        }

        lessonResourceRepository.delete(resource);
    }

    /**
     * Retrieves a list of lesson resources associated with a specific lesson ID.
     *
     * @param lessonId The ID of the lesson for which to retrieve the associated resources.
     * @return A list of LessonResource objects associated with the specified lesson ID.
     * @throws ResponseStatusException NOT_FOUND if no lesson with the given ID exists.
     */
    public List<ResponseLessonResourceDto> getResourcesByLessonId(UUID lessonId) {
        if(!lessonRepository.existsById(lessonId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found with ID: " + lessonId);
        }

        List<LessonResource> resources = lessonResourceRepository.findByLessonId(lessonId);

        return lessonResourceMapper.toLessonResourcesDTOGetList(resources);
    }

    /**
     * Updates the details of a lesson resource based on its ID and the associated lesson ID.
     *
     * @param lessonId        The ID of the lesson to which the resource is associated.
     * @param resourceId      The ID of the lesson resource to be updated.
     * @param UpdateLessonResourceDto A UpdateLessonResourceDto object containing the updated details for the lesson resource. Only non-null fields will be updated.
     * @return The updated LessonResource object after saving it to the database.
     * @throws ResponseStatusException NOT_FOUND if no lesson with the given ID exists, if no resource with the given ID exists, or if the resource does not belong to the specified lesson.
     */
    @Transactional
    public ResponseLessonResourceDto updateLessonResource(UUID lessonId, UUID resourceId, UpdateLessonResourceDto updatedResourceDTOPatch) {
        if(!lessonRepository.existsById(lessonId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found with ID: " + lessonId);
        }

        LessonResource existingResource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found with ID: " + resourceId));

        if(!existingResource.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource with ID: " + resourceId + " does not belong to lesson with ID: " + lessonId);
        }

        lessonResourceMapper.updateLessonResourceFromDto(updatedResourceDTOPatch, existingResource);

        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(existingResource));
    }
}
