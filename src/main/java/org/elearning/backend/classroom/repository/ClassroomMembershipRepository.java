package org.elearning.backend.classroom.repository;

import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ClassroomMembershipRepository extends JpaRepository<ClassroomMembership, UUID>, JpaSpecificationExecutor<ClassroomMembership> {

    // toti membrii unei clase
    List<ClassroomMembership> findAllByClassroomId(UUID classroomId);

    List<ClassroomMembership> findAllByClassroomIdAndMembershipType(UUID classroomId, MembershipType membershipType);

    // clase student/prof
    List<ClassroomMembership> findAllByUserIdAndMembershipType(UUID userId, MembershipType membershipType);

    // toate clasele unui user indiferent de tip
    List<ClassroomMembership> findAllByUserId(UUID userId);

    boolean existsByClassroomIdAndUserId(UUID classroomID, UUID userId);

    boolean existsByClassroomIdAndUserIdAndMembershipType(UUID classroomId, UUID userId, MembershipType membershipType);

    void deleteByClassroomIdAndUserIdAndMembershipType(UUID classroomId, UUID userId, MembershipType membershipType);

    MembershipType getClassroomMembershipById(UUID id);
}
