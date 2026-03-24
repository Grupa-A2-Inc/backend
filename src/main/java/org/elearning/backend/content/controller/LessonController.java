package org.elearning.backend.content.controller;


import org.elearning.backend.content.dto.LessonDTOMetadata;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.service.LessonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/api/lesson-test")
    public String testMe() {
        return "LessonController functioneaza!";
    }

    @PostMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<Lesson> createNewLesson(
            @RequestBody Lesson newLesson,
            @PathVariable UUID chapterID){
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createNewLesson(newLesson, chapterID));
    }

    @DeleteMapping("/api/lessons/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable UUID id){
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<List<Lesson>> getAllLessonsFromChapterID(@PathVariable UUID chapterID){
        return ResponseEntity.ok(lessonService.getAllLessonsFromChapter(chapterID));
    }

    @GetMapping("/api/lessons/{id}/content")
    public ResponseEntity<String> getLessonContent(
            @PathVariable UUID id){
        return ResponseEntity.ok(lessonService.getLessonContent(id));
    }

    @PatchMapping("/api/lessons/{id}/metadata")
    public ResponseEntity<Lesson> updateLessonMetadata(
            @PathVariable UUID id,
            @RequestBody LessonDTOMetadata lessonDTOMetadata){
        return ResponseEntity.ok(lessonService.updateLessonMetadata(id, lessonDTOMetadata));
    }

    @PatchMapping("/api/lessons/{id}/content")
    public ResponseEntity<Lesson> updateLessonContent(
            @PathVariable UUID id,
            @RequestBody String markdownContent){
        return ResponseEntity.ok(lessonService.updateLessonMarkdownContent(id, markdownContent));
    }



}
