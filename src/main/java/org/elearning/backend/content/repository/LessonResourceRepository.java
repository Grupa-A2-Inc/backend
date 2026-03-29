package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing LessonResource entities.
 * This interface extends JpaRepository, providing basic CRUD operations and additional query methods for LessonResource entities.
 * It allows for retrieving lesson resources based on their associated lesson ID.
 */
@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {

    /**
     * Retrieves a list of LessonResource entities associated with a specific lesson ID.
     * Automatically implemented by Spring Data JPA based on the method name convention.
     * The method name "findByLessonId" indicates that it will query the database for LessonResource entities where the associated lesson's ID matches the provided lessonId parameter.
     *
     * @param lessonId The ID of the lesson for which to retrieve the associated resources.
     * @return A list of LessonResource entities associated with the specified lesson ID.
     */
    List<LessonResource> findByLessonId(UUID lessonId);
}
