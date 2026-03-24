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




    @Query("SELECT MAX(l.orderIndex) FROM Lesson l WHERE l.chapterID= :chapter_id")
    Optional<Integer> findLastOrderIndex(@Param("chapter_id") UUID chapterID);

    @Query("SELECT l FROM Lesson l WHERE l.chapterID = :chapter_id ORDER BY l.orderIndex ASC ")
    List<Lesson> findLessonOrderByIndex(@Param("chapter_id") UUID chapterID);

    @Modifying
    @Query("UPDATE Lesson l SET l.orderIndex = l.orderIndex-1 WHERE l.orderIndex >= :referenceIndex AND l.id != :referenceID AND l.chapterID = :chapter_id")
    void repairLessonOrderIndex(@Param("referenceIndex") int orderIndex,
                                @Param("referenceID") UUID lessonID,
                                @Param("chapter_id") UUID chapterID);


    @Query("SELECT l.chapterID FROM Lesson l WHERE l.id = :givenID")
    Optional<UUID> findChapterIdFromID(@Param("givenID") UUID lessonID);

    @Query("SELECT l.orderIndex FROM Lesson l WHERE l.id = :givenID")
    int findOrderIndexFromID(@Param("givenID") UUID lessonID);

    @Query("SELECT l.contentMarkdown FROM Lesson l WHERE l.id = :givenID")
    String findContentMarkdown(@Param("givenID") UUID lessonID);

    @Modifying
    @Query("UPDATE Lesson l SET l.title = :new_title WHERE l.id = :lessonID")
    void updateLessonTitle(@Param("lessonID") UUID lessonID, @Param("new_title") String newTitle);

    @Modifying
    @Query("UPDATE Lesson l SET l.orderIndex = :order_index WHERE l.id = :lessonID AND l.chapterID = :chapter_id")
    void updateLessonOrderIndex(@Param("lessonID") UUID lessonID,
                                @Param("order_index") int newOrderIndex,
                                @Param("chapter_id") UUID chapterID);

    @Modifying
    @Query("UPDATE Lesson l SET l.contentMarkdown = :content_md WHERE l.id = :lessonID")
    void updateLessonContentMarkdown(@Param("lessonID") UUID lessonID, @Param("content_md") String markdownContent);

    @Modifying
    @Query("DELETE FROM Lesson l WHERE l.id = :lessonID")
    void deleteLesson(@Param("lessonID") UUID lessonID);


}
