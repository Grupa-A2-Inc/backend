package org.elearning.backend.enrollment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(
        name = "course_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enrollment",
                columnNames = {"course_id", "student_id"}
        )
)
public class CourseEnrollment {

    @Id
    @Column(name = "id")
    @GeneratedValue
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // null = în progres

    @OneToMany(mappedBy = "courseEnrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonProgress> lessonProgresses = new ArrayList<>();
}
