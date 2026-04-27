package org.elearning.backend.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LessonDifficultyKey implements Serializable {
    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "student_id")
    private UUID studentId;
}
