package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TestRepository extends JpaRepository<Test, UUID> {

    /** Finds test from a given lesson id
     *  A test can have AT MOST a single lesson assigned to it
     * @param lessonId lessonId of the wanted lesson
     * @return a test associated with the given lesson id or null if there's no such test
     */

    Optional<Test> findByLessonId(UUID lessonId);
    List<Test> findByLessonIdIn(Collection<UUID> lessonIds);
    List<Test> findByCreatedByIn(Collection<UUID> createdBy);

    /**
     * Verifies if a lesson has an assigned test or not
     * A lesson has AT MOST a single test associated with it
     * @param lessonId lessonId of wanted lesson
     * @return 0 - if there's no test associated with the lesson, 1 otherwise.
     */

    @Query("SELECT COUNT(*) FROM Test t WHERE t.lessonId = :lessonId")
    Integer lessonHasTest(@Param("lessonId") UUID lessonId);

    boolean existsByIdAndCreatedBy(UUID id, UUID createdBy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Test t SET t.status=:status WHERE t.id = :testId")
    void updateTestStatus(@Param("status")TestStatus status,
                          @Param("testId") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Test t SET t.title=:new_title WHERE t.id = :testId")
    void updateTestTitle(@Param("new_title")String title,
                          @Param("testId") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Test t SET t.description=:new_description WHERE t.id = :testId")
    void updateTestDescription(@Param("new_description")String description,
                         @Param("testId") UUID id);
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Test t SET t.timeLimitSec=:new_timeLimitSeconds WHERE t.id = :testId")
    void updateTestTimeLimitSeconds(@Param("new_timeLimitSeconds")Integer timeLimitSeconds,
                               @Param("testId") UUID id);
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Test t SET t.aiEnabled = :aiStatus WHERE t.id = :testId")
    void updateTestAiEnabled(@Param("aiStatus") Boolean aiStatus,
                               @Param("testId") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Test t WHERE t.id = :testId")
    void deleteTest(@Param("testId") UUID id);

    @Query("SELECT t.lessonId FROM Test t WHERE t.lessonId IN :lessonIds AND t.status = 'PUBLISHED'")
    Set<UUID> findLessonIdsWithPublishedTest(@Param("lessonIds") List<UUID> lessonIds);

    /**
     * Retrieves pairs of lesson and test identifiers for tests whose lesson IDs are in the given list.
     *
     * @param lessonIds the lesson UUIDs to filter tests by
     * @return a list of rows where each element is an Object[] containing two values: the lessonId (UUID) at index 0 and the testId (UUID) at index 1
     */
    @Query("SELECT t.lessonId, t.id FROM Test t WHERE t.lessonId IN :lessonIds")
    List<Object[]> findTestIdsByLessonIds(@Param("lessonIds") List<UUID> lessonIds);

}
