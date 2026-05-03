package org.elearning.backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.feedback.dto.LessonRatingFullStatsDto;
import org.elearning.backend.feedback.dto.ProfessorLessonRatingProjection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfessorRatingService {
    private final LessonRepository lessonRepository;

    public List<LessonRatingFullStatsDto> getAverageRatingsForAllLessons(UUID professorId) {
        List<ProfessorLessonRatingProjection> results = lessonRepository.getLessonsRatingForProfessor(professorId);
        List<LessonRatingFullStatsDto> returnedResults = new ArrayList<>();

        for (ProfessorLessonRatingProjection result : results) {
            UUID lessonId = result.getId();
            String title = result.getTitle();
            Double averageRating = result.getAvgRating();
            Long totalRatings = result.getTotalRatings();

            returnedResults.add(new LessonRatingFullStatsDto(lessonId, title, averageRating, totalRatings));
        }
        return returnedResults;
    }
}
