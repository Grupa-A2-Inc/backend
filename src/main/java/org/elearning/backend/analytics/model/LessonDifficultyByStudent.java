package org.elearning.backend.analytics.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lesson_difficulty_by_student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDifficultyByStudent {

    @EmbeddedId
    private LessonDifficultyKey id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lesson_title")
    private String lessonTitle;

    @Column(name = "my_personal_best_score")
    private BigDecimal myBestScore;

    @Column(name = "class_average_of_best")
    private BigDecimal classAverage;

    @Column(name = "gap")
    private BigDecimal gap;
}

