package org.elearning.backend.enrollment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_lesson_visited",
                columnNames = {"lesson_id", "student_id"}
        )
)
public class LessonProgress {

    @Id
    @Column(name = "id")
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @CreationTimestamp
    @Column(name = "visited_at", updatable = false)
    private LocalDateTime visitedAt;
}