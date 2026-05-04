package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ClassroomCourseRepository extends JpaRepository<ClassroomCourse, UUID>, JpaSpecificationExecutor<ClassroomCourse> {
    List<ClassroomCourse> findAllByClassroomId(UUID classroomId);
    List<ClassroomCourse> findAllByClassroomIdOrderByAssignedAtAsc(UUID classroomId);

    boolean existsByClassroomIdAndCourseId(UUID classroomId, UUID courseId);
}