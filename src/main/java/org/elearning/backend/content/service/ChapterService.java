package org.elearning.backend.content.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.elearning.backend.content.dto.ChapterDtoPost;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;

    public ChapterService(ChapterRepository chapterRepository, CourseRepository courseRepository) {
        this.chapterRepository = chapterRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Returns every chapter from a given course
     * @param courseId the specified course's id
     * @return a list of chapters
     */
    public List<Chapter> getAllChaptersFromCourse(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found with ID: " + courseId);
        }
        return chapterRepository.findChapterOrderByIndex(courseId);
    }

    /**
     * Creates a new chapter inside a course. If the course doesn't exist, it will throw an exception instead.
     *
     * @param courseId        the specified course's id
     * @param newChapterTitle the new chapter's title
     */
    public Chapter createNewChapter(UUID courseId, String newChapterTitle) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
        Chapter newChapter = new Chapter();
        newChapter.setCourse(course);
        int lastOrderIndex = chapterRepository.findLastOrderIndex(courseId).orElse(0) + 1;
        newChapter.setOrderIndex(lastOrderIndex);
        newChapter.setTitle(newChapterTitle);
        chapterRepository.save(newChapter);
        return newChapter;
    }

    /**
     * Changes the title of a chapter if it exists. Otherwise, it throws an exception
     * @param chapterId the chapter's id
     * @param newTitle the new title
     */
    @Transactional
    public void updateChapterTitle(UUID chapterId, String newTitle){
        chapterRepository.updateChapterTitle(chapterId, newTitle);
    }

    /**
     * Updates the order index of a chapter inside a course. If either the chapter or the parent course doesn't exist,
     * or the new index goes out of bounds, it will throw an exception instead.
     * @param chapterId the chapter's id
     * @param newOrderIndex the new index value
     */
    @Transactional
    public void updateChapterOrder(UUID chapterId, int newOrderIndex){
        UUID courseId = chapterRepository.findCourseIdFromId(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        int lastOrderIndex = chapterRepository.findLastOrderIndex(courseId).orElse(1);
        int previousOrderIndex = chapterRepository.findOrderIndexFromId(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Index not found"));

        if(newOrderIndex > lastOrderIndex || newOrderIndex <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order index out of range");
        }

        chapterRepository.updateChapterOrderIndex(chapterId, newOrderIndex, courseId);
        if(previousOrderIndex==newOrderIndex){
            return;
        }
        if(previousOrderIndex>newOrderIndex) {
            chapterRepository.repairChapterOrderIndexAfterOrderChangeBigger(previousOrderIndex, newOrderIndex, courseId, chapterId);
        }
        else {
            chapterRepository.repairChapterOrderIndexAfterOrderChangeSmaller(previousOrderIndex, newOrderIndex, courseId, chapterId);
        }

    }

    /**
     * Deletes a chapter from a course. If either the chapter or the parent course doesn't exist, it will throw an exception instead.
     * @param chapterId the chapter's id
     */
    @Transactional
    public void deleteChapter(UUID chapterId){
        if(!chapterRepository.existsById(chapterId)) {
            throw new IllegalArgumentException("Chapter not found with ID: " + chapterId);
        }

        UUID courseId = chapterRepository.findCourseIdFromId(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found for chapter with ID: " + chapterId));

        Integer orderIndex = chapterRepository.findOrderIndexFromId(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Order index not found for chapter ID: " + chapterId));

        chapterRepository.repairChapterOrderIndexAfterDeletion(orderIndex, courseId);
        chapterRepository.deleteById(chapterId);

    }

    /**
     * Updates the chapter's metadata (title and order index) based on the provided ChapterDTOPost object.
     * If the chapter doesn't exist, it will throw an exception instead.
     * @param chapterId the chapter's id
     * @param chapterDTOPost a ChapterDTOPost object containing the updated metadata for the chapter
     * @return
     */
    @Transactional
    public Chapter updateChapterMetadata(UUID chapterId, ChapterDtoPost chapterDTOPost) {
        if (!chapterRepository.existsById(chapterId)) {
            throw new IllegalArgumentException("Chapter not found with ID: " + chapterId);
        }
        if (chapterDTOPost.getOrderIndex() != null) {
            updateChapterOrder(chapterId, chapterDTOPost.getOrderIndex());
        }
        if (chapterDTOPost.getTitle() != null) {
            updateChapterTitle(chapterId, chapterDTOPost.getTitle());
        }

        chapterRepository.flush();
        return chapterRepository.findById(chapterId).orElse(new Chapter());
    }

}
