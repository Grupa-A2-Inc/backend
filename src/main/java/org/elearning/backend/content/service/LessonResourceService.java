package org.elearning.backend.content.service;

import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.transaction.annotation.Transactional;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;

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

    public LessonResourceService(LessonResourceRepository lessonResourceRepository, LessonRepository lessonRepository) {
        this.lessonResourceRepository = lessonResourceRepository;
        this.lessonRepository = lessonRepository;
    }

    /**
     * Creates a new lesson resource and associates it with a specific lesson ID.
     *
     * @param lessonResource The LessonResource object containing the details of the resource to be created.
     * @param lessonId       The ID of the lesson to which the resource will be associated.
     * @return The created LessonResource object with its generated ID and associated lesson ID.
     * @throws IllegalArgumentException if no lesson with the given ID exists.
     */
    public LessonResource createNewLessonResource(LessonResource lessonResource, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with ID: " + lessonId));

        lessonResource.setLesson(lesson);

        return lessonResourceRepository.save(lessonResource);
    }

    /**
     * Deletes a lesson resource based on its ID and the associated lesson ID.
     *
     * @param resourceId The ID of the lesson resource to be deleted.
     * @param lessonId   The ID of the lesson to which the resource is associated.
     * @throws IllegalArgumentException if no resource with the given ID exists or if the resource does not belong to the specified lesson.
     */
    @Transactional
    public void deleteLessonResource(UUID resourceId, UUID lessonId) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        if (!resource.getLesson().getId().equals(lessonId)) {
            throw new IllegalArgumentException("Resource does not belong to the specified lesson!");
        }

        lessonResourceRepository.delete(resource);
    }

    /**
     * Retrieves a list of lesson resources associated with a specific lesson ID.
     *
     * @param lessonId The ID of the lesson for which to retrieve the associated resources.
     * @return A list of LessonResource objects associated with the specified lesson ID.
     * @throws IllegalArgumentException if no lesson with the given ID exists.
     */
    public List<LessonResource> getResourcesByLessonId(UUID lessonId) {
        if(!lessonRepository.existsById(lessonId)){
            throw new IllegalArgumentException("Lesson not found with ID: " + lessonId);
        }

        return lessonResourceRepository.findByLessonId(lessonId);
    }

    /**
     * Updates the details of a lesson resource based on its ID and the associated lesson ID.
     *
     * @param lessonId        The ID of the lesson to which the resource is associated.
     * @param resourceId      The ID of the lesson resource to be updated.
     * @param updatedResource A LessonResource object containing the updated details of the resource.
     * @return The updated LessonResource object after saving it to the database.
     * @throws IllegalArgumentException if no lesson with the given ID exists, if no resource with the given ID exists, or if the resource does not belong to the specified lesson.
     */
    @Transactional
    public LessonResource updateLessonResource(UUID lessonId, UUID resourceId, LessonResource updatedResource) {
        if(!lessonRepository.existsById(lessonId)){
            throw new IllegalArgumentException("Lesson not found with ID: " + lessonId);
        }

        LessonResource existingResource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with ID: " + resourceId));

        if(!existingResource.getLesson().getId().equals(lessonId)) {
            throw new IllegalArgumentException("Resource does not belong to the specified lesson!");
        }

        if(updatedResource.getTitle() != null) {
            existingResource.setTitle(updatedResource.getTitle());
        }
        if(updatedResource.getUrl() != null) {
            existingResource.setUrl(updatedResource.getUrl());
        }

        return lessonResourceRepository.save(existingResource);
    }
}
