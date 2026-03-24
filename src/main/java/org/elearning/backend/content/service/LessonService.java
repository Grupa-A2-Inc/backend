package org.elearning.backend.content.service;

import jakarta.transaction.Transactional;
import org.elearning.backend.content.dto.LessonDTOMetadata;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }


    public List<Lesson> getAllLessonsFromChapter(UUID chapterID){
        return lessonRepository.findLessonOrderByIndex(chapterID);
    }


    public String getLessonContent(UUID lessonID){
        if(!lessonRepository.existsById(lessonID)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        return lessonRepository.findContentMarkdown(lessonID);
    }

    public Lesson createNewLesson(Lesson newLesson, UUID chapterID){
        newLesson.setChapterID(chapterID);
        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(0) + 1;
        newLesson.setOrderIndex(lastOrderIndex);
        lessonRepository.save(newLesson);
        return newLesson;
    }

    @Transactional
    public Lesson updateLessonMarkdownContent(UUID lessonID, String markdownContent){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        lessonRepository.updateLessonContentMarkdown(lessonID, markdownContent);
        return lessonRepository.findById(lessonID).orElse(new Lesson());
    }

    @Transactional
    public void updateLessonTitle(UUID lessonID, String newTitle){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        lessonRepository.updateLessonTitle(lessonID, newTitle);

    }

    @Transactional
    public void updateLessonOrder(UUID lessonID, int newOrderIndex){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(1);

        if(newOrderIndex > lastOrderIndex || newOrderIndex<0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order index out of range");
        }

        lessonRepository.updateLessonOrderIndex(lessonID, newOrderIndex, chapterID);
        lessonRepository.repairLessonOrderIndex(newOrderIndex, lessonID, chapterID);

    }

    @Transactional
    public Lesson updateLessonMetadata(UUID lessonID, LessonDTOMetadata lessonDTOMetadata){
        if(lessonDTOMetadata.getChapterIndex()!=null){
            updateLessonOrder(lessonID, lessonDTOMetadata.getChapterIndex());
        }
        if(lessonDTOMetadata.getTitle()!=null){
            updateLessonTitle(lessonID, lessonDTOMetadata.getTitle());
        }
        return lessonRepository.findById(lessonID).orElse(new Lesson());
    }

    @Transactional
    public void deleteLesson(UUID lessonID){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        lessonRepository.repairLessonOrderIndex(lessonRepository.findOrderIndexFromID(lessonID), lessonID, chapterID);
        lessonRepository.deleteLesson(lessonID);

    }

}
