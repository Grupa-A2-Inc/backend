package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.feedback.dto.LessonVisibilityAndOwnerDto;
import org.elearning.backend.feedback.dto.ProfessorLessonRatingProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    // Returns the last order index inside a specific chapter
    @Query("SELECT MAX(l.orderIndex) FROM Lesson l WHERE l.chapter.id = :chapter_id")
    Optional<Integer> findLastOrderIndex(@Param("chapter_id") UUID chapterID);

    // Returns every single chapter from the lesson table as long as they are from the same chapter, order by their order index
    @Query("SELECT l FROM Lesson l WHERE l.chapter.id  = :chapter_id ORDER BY l.orderIndex ASC ")
    List<Lesson> findLessonOrderByIndex(@Param("chapter_id") UUID chapterID);

    // Repairs the order index of elements inside a specific chapter, after changing the order index of any lesson
    // Increments the order indexes between the new value and the previous, if the new value is lesser than the previous

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l " +
            "SET l.orderIndex = l.orderIndex + 1 " +
            "WHERE ( l.orderIndex >= :newOrderIndex " +
            "AND l.orderIndex <= :previousOrderIndex) " +
            "AND l.chapter.id = :chapter_id " +
            "AND l.id != :lesson_id")
    void repairLessonOrderIndexAfterOrderChangeBigger(@Param("previousOrderIndex") int previousOrderIndex,
                                @Param("newOrderIndex") int newOrderIndex,
                                @Param("chapter_id") UUID chapterID, @Param("lesson_id") UUID lessonID);

    // Decrements the order indexes between the new value and the previous, if the new value is bigger than the previous

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l " +
            "SET l.orderIndex = l.orderIndex - 1 " +
            "WHERE ( l.orderIndex <= :newOrderIndex " +
            "AND l.orderIndex >= :previousOrderIndex) " +
            "AND l.chapter.id = :chapter_id " +
            "AND l.id != :lesson_id")
    void repairLessonOrderIndexAfterOrderChangeSmaller(@Param("previousOrderIndex") int previousOrderIndex,
                                                      @Param("newOrderIndex") int newOrderIndex,
                                                      @Param("chapter_id") UUID chapterID, @Param("lesson_id") UUID lessonID);

    // Repairs the order index of elements inside a specific chapter, after changing the order index of any lesson
    // Every element after the deleted one will have its order index decremented

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l SET l.orderIndex = l.orderIndex - 1 " +
            "WHERE l.orderIndex > :referenceOrderIndex AND l.chapter.id = :chapter_id")
    void repairLessonOrderIndexAfterDeletion(@Param("referenceOrderIndex") int referenceOrderIndex,
                                                @Param("chapter_id") UUID chapterID);


    //Returns the chapter of a given lesson
    @Query("SELECT l.chapter.id FROM Lesson l WHERE l.id = :givenID")
    Optional<UUID> findChapterIdFromID(@Param("givenID") UUID lessonID);

    //Returns the order index of a lesson
    @Query("SELECT l.orderIndex FROM Lesson l WHERE l.id = :givenID")
    Optional<Integer> findOrderIndexFromID(@Param("givenID") UUID lessonID);

    //Returns the content markdown of a given lesson
    @Query("SELECT l.contentMarkdown FROM Lesson l WHERE l.id = :givenID")
    Optional<String> findContentMarkdown(@Param("givenID") UUID lessonID);

    //Changes the title of a lesson
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l SET l.title = :title WHERE l.id = :id")
    void updateLessonTitle(@Param("id") UUID id, @Param("title") String title);

    //Changes the order index of a lesson from a specific chapter

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l SET l.orderIndex = :order_index WHERE l.id = :lessonID AND l.chapter.id = :chapter_id")
    void updateLessonOrderIndex(@Param("lessonID") UUID lessonID,
                                @Param("order_index") int newOrderIndex,
                                @Param("chapter_id") UUID chapterID);

    //Updates the information inside content markdown
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l SET l.contentMarkdown = :content WHERE l.id = :id")
    void updateLessonContentMarkdown(@Param("id") UUID id, @Param("content") String content);

    //Deletes a specific lesson

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Lesson l WHERE l.id = :lessonID")
    void deleteLesson(@Param("lessonID") UUID lessonID);

    /**
     * Retrieves all lessons associated with a specific course ID and eagerly fetches their associated lesson resources ordered by orderIndex.
     * @param courseId the unique identifier of the course
     * @return a list of Lesson entities, fully populated with their respective lesson resources
     */
    @Query("SELECT l FROM Lesson l LEFT JOIN FETCH l.lessonResources WHERE l.chapter.course.id = :courseId ORDER BY l.orderIndex ASC")
    List<Lesson> findLessonsWithResourcesByCourseId(@Param("courseId") UUID courseId);

    /**
     * Gets all lesson IDs associated with the specified course.
     *
     * @param courseId the UUID of the course whose lessons are queried
     * @return a list of lesson UUIDs belonging to the course, or an empty list if none exist
     */
    @Query("""
        SELECT l.id FROM Lesson l
        WHERE l.chapter.course.id = :courseId
    """)
    List<UUID> findAllLessonIdsByCourseId(@Param("courseId") UUID courseId);

    /**
     * Checks whether the course that contains the specified lesson was created by the given professor.
     *
     * @param lessonId    the UUID of the lesson to check
     * @param professorId the UUID of the professor to verify as the course creator
     * @return            `true` if the lesson's course was created by the professor identified by `professorId`, `false` otherwise
     */
    @Query("SELECT COUNT(l) > 0 FROM Lesson l " +
            "JOIN l.chapter ch " +
            "JOIN ch.course c " +
            "WHERE l.id = :lessonId AND c.createdBy = :professorId")
    boolean isLessonOwnedByProfessor(@Param("lessonId") UUID lessonId, @Param("professorId") UUID professorId);

    /**
     * Checks whether a student is enrolled in the course that contains the specified lesson.
     *
     * @param lessonId  the UUID of the lesson whose course enrollment will be checked
     * @param studentId the UUID of the student to verify enrollment for
     * @return `true` if the student is enrolled in the lesson's course, `false` otherwise
     */
    @Query("SELECT COUNT(l) > 0 FROM Lesson l " +
            "JOIN l.chapter ch " +
            "JOIN CourseEnrollment ce ON ce.courseId = ch.course.id " +
            "WHERE l.id = :lessonId AND ce.studentId = :studentId")
    boolean isStudentEnrolledInLessonCourse(@Param("lessonId") UUID lessonId, @Param("studentId") UUID studentId);

    @Query("SELECT new org.elearning.backend.feedback.dto.LessonVisibilityAndOwnerDto(c.visibility, c.createdBy, l.title) " +
            "FROM Lesson l " +
            "LEFT JOIN l.chapter ch " +
            "LEFT JOIN ch.course c " +
            "WHERE l.id = :lessonId")
    LessonVisibilityAndOwnerDto getLessonVisibilityAndOwner(@Param("lessonId") UUID lessonId);

    @Query(nativeQuery = true, value = "" +
            "SELECT l.id, l.title, AVG(lr.rating) as \"avgRating\", COUNT(lr.id) as \"totalRatings\" " +
            "FROM lessons l " +
            "LEFT JOIN lesson_ratings lr on lr.lesson_id = l.id " +
            "JOIN chapters ch ON l.chapter_id = ch.id " +
            "JOIN courses co ON ch.course_id = co.id " +
            "WHERE co.created_by = :professorId " +
            "GROUP BY l.id, l.title " +
            "ORDER BY \"avgRating\" ASC NULLS LAST")
    List<ProfessorLessonRatingProjection> getLessonsRatingForProfessor(@Param("professorId") UUID professorId);
}
