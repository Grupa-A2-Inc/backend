package org.elearning.backend.classroom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "classroom_courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_classroom_course",
                columnNames = {"classroom_id", "course_id"}
        )
)
public class ClassroomCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "classroom_id", nullable = false)
    private UUID classroomId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private LocalDateTime assignedAt;
}