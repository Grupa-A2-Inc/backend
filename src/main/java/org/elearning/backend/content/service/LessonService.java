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

    //Equivalent expression: @Autowired. IntelliJ says it's safer to implement it this way, however.
    //JPA makes sure to create the repository automatically

    private final LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }


    //Returns every lesson from a given chapter
    public List<Lesson> getAllLessonsFromChapter(UUID chapterID){
        return lessonRepository.findLessonOrderByIndex(chapterID);
    }


    //Returns the content markdown of a lesson
    //Throws exception if the lesson does not exist.
    public String getLessonContent(UUID lessonID){
        if(!lessonRepository.existsById(lessonID)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        return lessonRepository.findContentMarkdown(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    }

    //Creates a new Lesson, with a mandatory associated chapter.
    //The order will be the biggest inside the lesson table
    //We return the saved Lesson for the ResponseEntity class inside LessonController
    //It makes sure to send the HTTP status and the update information
    public Lesson createNewLesson(Lesson newLesson, UUID chapterID){
        newLesson.setChapterID(chapterID);
        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(0) + 1;
        newLesson.setOrderIndex(lastOrderIndex);
        lessonRepository.save(newLesson);
        return newLesson;
    }

    //Every single update and deletion has a "@Transactional" Tag
    //Updates the information of the content markdown inside a lesson, if it exists.
    //We return the saved Lesson for the ResponseEntity class inside LessonController
    //It makes sure to send the HTTP status and the update information


    @Transactional
    public Lesson updateLessonMarkdownContent(UUID lessonID, String markdownContent){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        lessonRepository.updateLessonContentMarkdown(lessonID, markdownContent);
        return lessonRepository.findById(lessonID).orElse(new Lesson());
    }

    //Every single update and deletion has a "@Transactional" Tag
    //Updates the title of a lesson, if it exists. Otherwise, it throws an exception

    @Transactional
    public void updateLessonTitle(UUID lessonID, String newTitle){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        lessonRepository.updateLessonTitle(lessonID, newTitle);

    }

    // Every single update and deletion has a "@Transactional" Tag
    // Updates the order index of a lesson if:
    // * The lesson exists
    // * The lesson exists in a valid chapter
    // * The new index doesn't go out of bounds
    // If either condition isn't respected, we throw an exception
    // We make sure to repair the index order inside the repository. We have two cases:
    // 1. The previous index is bigger than the newer. In that case, we increment the order index of each element between them
    // 2. The previous index is small than the newer. In that case, we decrement the order index of each element between them


    @Transactional
    public void updateLessonOrder(UUID lessonID, int newOrderIndex){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        int lastOrderIndex = lessonRepository.findLastOrderIndex(chapterID).orElse(1);
        int previousOrderIndex = lessonRepository.findOrderIndexFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Index not found"));

        if(newOrderIndex > lastOrderIndex || newOrderIndex <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order index out of range");
        }

        lessonRepository.updateLessonOrderIndex(lessonID, newOrderIndex, chapterID);
        if(previousOrderIndex==newOrderIndex){
            return;
        }
        if(previousOrderIndex>newOrderIndex) {
            lessonRepository.repairLessonOrderIndexAfterOrderChangeBigger(previousOrderIndex, newOrderIndex, chapterID, lessonID);
        }
        else {
            lessonRepository.repairLessonOrderIndexAfterOrderChangeSmaller(previousOrderIndex, newOrderIndex, chapterID, lessonID);
        }

    }

    // Every single update and deletion has a "@Transactional" Tag
    // Updates lesson metadata that can be modified, that being the title and the order index
    // Will only update if the non-null parameters
    //We return the saved Lesson for the ResponseEntity class inside LessonController
    //It makes sure to send the HTTP status and the update information

    @Transactional
    public Lesson updateLessonMetadata(UUID lessonID, LessonDTOMetadata lessonDTOMetadata){

        if(!lessonRepository.existsById(lessonID)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        if(lessonDTOMetadata.getOrderIndex()!=null){
            updateLessonOrder(lessonID, lessonDTOMetadata.getOrderIndex());
        }
        if(lessonDTOMetadata.getTitle()!=null){
            updateLessonTitle(lessonID, lessonDTOMetadata.getTitle());
        }
        return lessonRepository.findById(lessonID).orElse(new Lesson());
    }

    // Every single update and deletion has a "@Transactional" Tag.
    // Deletes a lesson from the table if it exists, otherwise it throws and error.
    // It will also throw an exception if, for whatever reason, the lesson didn't have a valid chapterID or order index.
    // Once we delete the lesson, the method makes sure to repair the order of each element inside the table.
    // To do that, it increments every order index value situated after the previous deleted.
    // We return the saved Lesson for the ResponseEntity class inside LessonController
    // It makes sure to send the HTTP status and the update information

    @Transactional
    public void deleteLesson(UUID lessonID){
        if(!lessonRepository.existsById(lessonID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        UUID chapterID = lessonRepository.findChapterIdFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        Integer orderIndex = lessonRepository.findOrderIndexFromID(lessonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order Index not found"));

        lessonRepository.repairLessonOrderIndexAfterDeletion(orderIndex, chapterID);
        lessonRepository.deleteLesson(lessonID);

    }

}
