package org.elearning.backend.content.controller;

import org.elearning.backend.content.dto.ChapterDTOMetadata;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ChapterController {
    private final ChapterService chapterService;
    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/api/courses/{courseId}/chapters")
    public ResponseEntity<Chapter> createNewChapter(@PathVariable UUID courseId,
                                                    @RequestBody Chapter newChapter) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.createNewChapter(newChapter, courseId));
    }

    @DeleteMapping("/api/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable UUID id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/chapters/{id}")
    public ResponseEntity<Chapter> updateChapter(@PathVariable UUID id,
                                                 @RequestBody ChapterDTOMetadata chapterDTOMetadata) {
        return ResponseEntity.ok(chapterService.updateChapterMetadata(id, chapterDTOMetadata));
    }
}
