package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassroomCourseRepository extends JpaRepository<ClassroomCourse, UUID> {
    List<ClassroomCourse> findAllByClassroomId(UUID classroomId);

    boolean existsByClassroomIdAndCourseId(UUID classroomId, UUID courseId);
}