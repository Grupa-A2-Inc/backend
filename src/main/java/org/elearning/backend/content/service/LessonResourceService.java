package org.elearning.backend.content.service;

import org.elearning.backend.content.dto.ResponseLessonResourceDto;
import org.elearning.backend.content.dto.UpdateLessonResourceDto;
import org.elearning.backend.content.dto.CreateLessonResourceDto;
import org.elearning.backend.content.exception.InvalidResourceDataException;
import org.elearning.backend.content.exception.LessonNotFoundException;
import org.elearning.backend.content.exception.LessonResourceNotFoundException;
import org.elearning.backend.content.mapper.LessonResourceMapper;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
     * Creates a new lesson resource for a specific lesson. If the lesson doesn't exist, it will throw an exception instead.
     *
     * @param lessonResourceDTOPost the data for the new lesson resource
     * @param lessonId              the specified lesson's id
     * @return the created lesson resource in DTO format
     */
    public ResponseLessonResourceDto createNewLessonResource(CreateLessonResourceDto lessonResourceDTOPost, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));

        if (lessonResourceDTOPost.getTitle() == null || lessonResourceDTOPost.getUrl() == null) {
            throw new InvalidResourceDataException("Title or URL cannot be null!");
        }

        LessonResource lessonResource = lessonResourceMapper.toLessonResource(lessonResourceDTOPost);
        lessonResource.setLesson(lesson);

        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(lessonResource));
    }

    /**
     * Deletes a lesson resource by its ID and the associated lesson ID.
     * If the resource does not exist or its associated lesson ID is not equal to lessonId, an exception will be thrown.
     *
     * @param resourceId the ID of the lesson resource to be deleted
     * @param lessonId   the ID of the associated lesson
     */
    @Transactional
    public void deleteLessonResource(UUID resourceId, UUID lessonId) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new LessonResourceNotFoundException(resourceId));

        if (!resource.getLesson().getId().equals(lessonId)) {
            throw new LessonResourceNotFoundException(resourceId);
        }

        lessonResourceRepository.delete(resource);
    }

    /**
     * Retrieves a list of lesson resources associated with a specific lesson ID.
     * If the lesson does not exist, an exception will be thrown.
     *
     * @param lessonId the ID of the lesson for which to retrieve the associated resources
     * @return a list of lesson resources in DTO format associated with the specified lesson ID
     */
    public List<ResponseLessonResourceDto> getResourcesByLessonId(UUID lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        return lessonResourceMapper.toLessonResourcesDTOGetList(lessonResourceRepository.findByLessonId(lessonId));
    }

    /**
     * Updates an existing lesson resource with the provided data.
     * If the resource does not exist or its associated lesson ID is not equal to lessonId, an exception will be thrown.
     *
     * @param lessonId                 the ID of the associated lesson
     * @param resourceId               the ID of the lesson resource to be updated
     * @param updatedResourceDTOPatch the data for updating the lesson resource
     * @return the updated lesson resource in DTO format
     */
    @Transactional
    public ResponseLessonResourceDto updateLessonResource(UUID lessonId, UUID resourceId, UpdateLessonResourceDto updatedResourceDTOPatch) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }

        LessonResource existingResource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new LessonResourceNotFoundException(resourceId));

        if (!existingResource.getLesson().getId().equals(lessonId)) {
            throw new LessonResourceNotFoundException(resourceId);
        }

        lessonResourceMapper.updateLessonResourceFromDto(updatedResourceDTOPatch, existingResource);
        return lessonResourceMapper.toLessonResourceDTOGet(lessonResourceRepository.save(existingResource));
    }
}