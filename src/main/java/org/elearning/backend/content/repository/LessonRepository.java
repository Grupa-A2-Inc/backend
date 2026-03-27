package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.Lesson;
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
    @Modifying
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
    @Modifying
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
    @Modifying
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
    @Modifying
    @Query("UPDATE Lesson l SET l.title = :new_title WHERE l.id = :lessonID")
    void updateLessonTitle(@Param("lessonID") UUID lessonID, @Param("new_title") String newTitle);

    //Changes the order index of a lesson from a specific chapter
    @Modifying
    @Query("UPDATE Lesson l SET l.orderIndex = :order_index WHERE l.id = :lessonID AND l.chapter.id = :chapter_id")
    void updateLessonOrderIndex(@Param("lessonID") UUID lessonID,
                                @Param("order_index") int newOrderIndex,
                                @Param("chapter_id") UUID chapterID);

    //Updates the information inside content markdown
    @Modifying
    @Query("UPDATE Lesson l SET l.contentMarkdown = :content_md WHERE l.id = :lessonID")
    void updateLessonContentMarkdown(@Param("lessonID") UUID lessonID, @Param("content_md") String markdownContent);

    //Deletes a specific lesson
    @Modifying
    @Query("DELETE FROM Lesson l WHERE l.id = :lessonID")
    void deleteLesson(@Param("lessonID") UUID lessonID);

    /**
     * Retrieves all lessons associated with a specific course ID and eagerly fetches their associated lesson resources ordered by orderIndex.
     * @param courseId the unique identifier of the course
     * @return a list of Lesson entities, fully populated with their respective lesson resources
     */
    @Query("SELECT l FROM Lesson l LEFT JOIN FETCH l.lessonResources WHERE l.chapter.course.id = :courseId ORDER BY l.orderIndex ASC")
    List<Lesson> findLessonsWithResourcesByCourseId(@Param("courseId") UUID courseId);


}
