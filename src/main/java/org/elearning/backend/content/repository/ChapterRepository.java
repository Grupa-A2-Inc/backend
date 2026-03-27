package org.elearning.backend.content.repository;

import org.elearning.backend.content.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    /**
     * Returns the last order index inside a specific course
     * @param courseId the specified course's id
     * @return an integer representing the last order index
     */
    @Query("SELECT MAX(c.orderIndex) FROM Chapter c WHERE c.course.id = :course_id")
    Optional<Integer> findLastOrderIndex(@Param("course_id") UUID courseId);

    /**
     * Returns every single chapter from the chapters table from a specific course
     * @param courseId the specified course's id
     * @return a list of chapters
     */
    @Query("SELECT c FROM Chapter c WHERE c.course.id = :course_id ORDER BY c.orderIndex ASC")
    List<Chapter> findChapterOrderByIndex(@Param("course_id") UUID courseId);

    /**
     * Repairs the order index of elements inside a specific course after changing the order index of any chapter in the course
     * This will be used when the new order index is lesser than the previous value
     *
     * @param previousOrderIndex the previous value of the changed index
     * @param newOrderIndex      the new value of the changed index
     * @param courseId           the specified course's id
     * @param chapterId          the changed chapter's id
     */
    @Modifying
    @Query("UPDATE Chapter c " +
            "SET c.orderIndex = c.orderIndex + 1 " +
            "WHERE c.orderIndex >= :new_order_index " +
            "AND c.orderIndex <= :previous_order_index " +
            "AND c.course.id = :course_id " +
            "AND c.id != :chapter_id")
    void repairChapterOrderIndexAfterOrderChangeBigger(@Param("previous_order_index") int previousOrderIndex,
                                                       @Param("new_order_index") int newOrderIndex,
                                                       @Param("course_id") UUID courseId,
                                                       @Param("chapter_id") UUID chapterId);

    /**
     * Repairs the order index of elements inside a specific course after changing the order index of any chapter in the course
     * This will be used when the new order index is greater than the previous value
     *
     * @param previousOrderIndex the previous value of the changed index
     * @param newOrderIndex      the new value of the changed index
     * @param courseId           the specified course's id
     * @param chapterId          the changed chapter's id
     */
    @Modifying
    @Query("UPDATE Chapter c " +
            "SET c.orderIndex = c.orderIndex - 1 " +
            "WHERE c.orderIndex >= :previous_order_index " +
            "AND c.orderIndex <= :new_order_index " +
            "AND c.course.id = :course_id " +
            "AND c.id != :chapter_id")
    void repairChapterOrderIndexAfterOrderChangeSmaller(@Param("previous_order_index") int previousOrderIndex,
                                                        @Param("new_order_index") int newOrderIndex,
                                                        @Param("course_id") UUID courseId,
                                                        @Param("chapter_id") UUID chapterId);

    /**
     * Repairs the order index of elements inside a specific course after deleting any chapter in the course
     * @param targetOrderIndex the order index of the deleted chapter
     * @param courseId the specified course's id
     */
    @Modifying
    @Query("UPDATE Chapter c " +
            "SET c.orderIndex = c.orderIndex - 1 " +
            "WHERE c.orderIndex > :target_order_index AND c.course.id = :course_id")
    void repairChapterOrderIndexAfterDeletion(@Param("target_order_index") int targetOrderIndex,
                                              @Param("course_id") UUID courseId);

    /**
     * Returns the parent course of a given chapter
     * @param chapterId the specified chapter's id
     * @return a UUID representing the parent course's id
     */
    @Query("SELECT c.course.id FROM Chapter c WHERE c.id = :target_id")
    Optional<UUID> findCourseIdFromId(@Param("target_id") UUID chapterId);

    /**
     * Returns the order index of a given chapter
     * @param chapterId the specified chapter's id
     * @return an integer representing the order index
     */
    @Query("SELECT c.orderIndex FROM Chapter c WHERE c.id = :target_id")
    Optional<Integer> findOrderIndexFromId(@Param("target_id") UUID chapterId);

    /**
     * Changes a chapter's title
     * @param chapterId the specified chapter's id
     * @param newTitle the new title
     */
    @Modifying
    @Query("UPDATE Chapter c SET c.title = :new_title WHERE c.id = :target_id")
    void updateChapterTitle(@Param("target_id") UUID chapterId,
                            @Param("new_title") String newTitle);

    /**
     * Changes the order index of a chapter inside a course
     * @param chapterId the specified chapter's id
     * @param courseId the parent course's id
     * @param newOrderIndex the new
     */
    @Modifying
    @Query("UPDATE Chapter c SET c.orderIndex = :new_order_index WHERE c.id = :chapter_id AND c.course.id = :course_id")
    void updateChapterOrderIndex(@Param("chapter_id") UUID chapterId,
                                 @Param("new_order_index") int newOrderIndex,
                                 @Param("course_id") UUID courseId);

    /**
     * Retrieves a list of Chapter entities along with their associated lessons based on the provided course ID ordered by orderIndex.
     * @param courseId The UUID of the course to retrieve chapters and lessons for.
     * @return A list of Chapter entities, each containing its associated lessons, if found; otherwise, an empty list.
     */
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.lessons WHERE c.course.id = :courseId ORDER BY c.orderIndex ASC")
    List<Chapter> findChaptersWithLessonsByCourseId(@Param("courseId") UUID courseId);

}
