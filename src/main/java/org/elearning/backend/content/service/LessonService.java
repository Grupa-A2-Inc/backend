package org.elearning.backend.content.service;

import jakarta.transaction.Transactional;
import org.elearning.backend.content.dto.LessonDtoEntity;
import org.elearning.backend.content.dto.LessonDtoMetadata;
import org.elearning.backend.content.dto.LessonDtoPost;
import org.elearning.backend.content.exception.ChapterNotFoundException;
import org.elearning.backend.content.exception.InvalidOrderIndexException;
import org.elearning.backend.content.exception.LessonNotFoundException;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;

    public LessonService(LessonRepository lessonRepository, ChapterRepository chapterRepository) {
        this.lessonRepository = lessonRepository;
        this.chapterRepository = chapterRepository;
    }

    public List<LessonDtoEntity> getAllLessonsFromChapter(UUID chapterID) {
        if (!chapterRepository.existsById(chapterID)) {
            throw new ChapterNotFoundException(chapterID);
        }
        List<Lesson> allLessonsFromChapter = lessonRepository.findLessonOrderByIndex(chapterID);
        List<LessonDtoEntity> allLessonsFromChapterDTO = new ArrayList<>();
        for (Lesson eachLesson : allLessonsFromChapter) {
            allLessonsFromChapterDTO.add(new LessonDtoEntity(eachLesson));
        }
        return allLessonsFromChapterDTO;
    }

    public String getLessonContent(UUID lessonID) {
        if (!lessonRepository.existsById(lessonID)) {
            throw new LessonNotFoundException(lessonID);
        }
        return lessonRepository.findContentMarkdown(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    }

    public LessonDtoEntity createNewLesson(LessonDtoPost modifiableLessonData, UUID chapterID) {
        Chapter chapter = chapterRepository.findById(chapterID)
                .orElseThrow(() -> new ChapterNotFoundException(chapterID));

        if (modifiableLessonData.getTitle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be null");
        }

        Lesson newLesson = new Lesson();
        newLesson.setTitle(modifiableLessonData.getTitle());
        newLesson.setContentMarkdown(modifiableLessonData.getContentMarkdown());
        newLesson.setChapter(chapter);
        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(0) + 1;
        newLesson.setOrderIndex(lastOrderIndex);
        lessonRepository.save(newLesson);
        return new LessonDtoEntity(newLesson);
    }

    @Transactional
    public LessonDtoEntity updateLessonMarkdownContent(UUID lessonID, String markdownContent) {
        if (!lessonRepository.existsById(lessonID)) {
            throw new LessonNotFoundException(lessonID);
        }
        lessonRepository.updateLessonContentMarkdown(lessonID, markdownContent);
        return new LessonDtoEntity(lessonRepository.findById(lessonID)
                .orElseThrow(() -> new LessonNotFoundException(lessonID)));
    }

    private void updateLessonTitle(UUID lessonID, String newTitle) {
        lessonRepository.updateLessonTitle(lessonID, newTitle);
    }

    private void updateLessonOrder(UUID lessonID, int newOrderIndex) {
        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ChapterNotFoundException(lessonID));

        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(1);
        int previousOrderIndex = lessonRepository.findOrderIndexFromID(lessonID)
                .orElseThrow(() -> new InvalidOrderIndexException("Order index not found for lesson ID: " + lessonID));

        if (newOrderIndex > lastOrderIndex || newOrderIndex <= 0) {
            throw new InvalidOrderIndexException("Order index out of range");
        }

        lessonRepository.updateLessonOrderIndex(lessonID, newOrderIndex, chapterID);
        if (previousOrderIndex == newOrderIndex) return;

        if (previousOrderIndex > newOrderIndex) {
            lessonRepository.repairLessonOrderIndexAfterOrderChangeBigger(previousOrderIndex, newOrderIndex, chapterID, lessonID);
        } else {
            lessonRepository.repairLessonOrderIndexAfterOrderChangeSmaller(previousOrderIndex, newOrderIndex, chapterID, lessonID);
        }
    }

    @Transactional
    public LessonDtoEntity updateLessonMetadata(UUID lessonID, LessonDtoMetadata lessonDTOMetadata) {
        if (!lessonRepository.existsById(lessonID)) {
            throw new LessonNotFoundException(lessonID);
        }
        if (lessonDTOMetadata.getOrderIndex() != null) {
            updateLessonOrder(lessonID, lessonDTOMetadata.getOrderIndex());
        }
        if (lessonDTOMetadata.getTitle() != null) {
            updateLessonTitle(lessonID, lessonDTOMetadata.getTitle());
        }
        lessonRepository.flush();
        return new LessonDtoEntity(lessonRepository.findById(lessonID)
                .orElseThrow(() -> new LessonNotFoundException(lessonID)));
    }

    @Transactional
    public void deleteLesson(UUID lessonID) {
        if (!lessonRepository.existsById(lessonID)) {
            throw new LessonNotFoundException(lessonID);
        }

        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ChapterNotFoundException(lessonID));

        Integer orderIndex = lessonRepository.findOrderIndexFromID(lessonID)
                .orElseThrow(() -> new InvalidOrderIndexException("Order index not found for lesson ID: " + lessonID));

        lessonRepository.repairLessonOrderIndexAfterDeletion(orderIndex, chapterID);
        lessonRepository.deleteLesson(lessonID);
    }
}