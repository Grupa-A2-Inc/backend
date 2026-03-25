package org.elearning.backend.content.service;

import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.repository.ChapterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;

    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public List<Chapter> getAllChaptersFromCourse(UUID courseId) {
        return chapterRepository.findChapterOrderByIndex(courseId);
    }

    public Chapter createNewChapter(Chapter newChapter, UUID courseId) {

    }
}
