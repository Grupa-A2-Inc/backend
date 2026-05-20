package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.elearning.backend.classroom.entity.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClassroomCourseRepository extends JpaRepository<ClassroomCourse, UUID>, JpaSpecificationExecutor<ClassroomCourse> {
    List<ClassroomCourse> findAllByClassroomId(UUID classroomId);
    List<ClassroomCourse> findAllByClassroomIdOrderByAssignedAtAsc(UUID classroomId);

    boolean existsByClassroomIdAndCourseId(UUID classroomId, UUID courseId);

    @Query("""
            SELECT COUNT(cc) > 0
            FROM ClassroomCourse cc, ClassroomMembership cm
            WHERE cc.classroomId = cm.classroom.id
              AND cm.user.id = :studentId
              AND cm.membershipType = :membershipType
              AND cc.courseId = :courseId
            """)
    boolean existsCourseAssignedToUserThroughAnyClassroom(
            @Param("studentId") UUID studentId,
            @Param("membershipType") MembershipType membershipType,
            @Param("courseId") UUID courseId
    );
}
