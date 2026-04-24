package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto;
import org.elearning.backend.analytics.model.LessonDifficultyByStudent;
import org.elearning.backend.analytics.model.LessonDifficultyKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LessonDifficultyByStudentRepository extends JpaRepository<LessonDifficultyByStudent, LessonDifficultyKey> {

    @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto(
             ld.id.lessonId,
             ld.lessonTitle,
             ld.myBestScore,
             ld.classAverage,
             ld.gap
             )
            FROM LessonDifficultyByStudent ld
            WHERE ld.courseId = :courseId
            AND ld.id.studentId = :studentId
            AND (ld.myBestScore < :passingGrade
            OR ld.gap > :problemGap)
            ORDER BY ld.gap DESC
        """)
    List<DifficultyLessonDto> getLessonDifficultyList(@Param("courseId") UUID courseId,
                                                      @Param("studentId") UUID studentId,
                                                      @Param("passingGrade") BigDecimal passingGrade,
                                                      @Param("problemGap") BigDecimal problemGap,
                                                      Pageable pageable);
}
