package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClassroomServiceListMembersTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ClassroomMembershipRepository classroomMembershipRepository;

    @InjectMocks
    private ClassroomService classroomService;

    @Test
    void listClassroomMembers_shouldReturnAllMembers_whenRoleIsNull() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership studentMembership = buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, "student@test.com", RoleName.STUDENT), MembershipType.STUDENT);
        ClassroomMembership teacherMembership = buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, "teacher@test.com", RoleName.TEACHER), MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomId(classroomId))
                .thenReturn(List.of(studentMembership, teacherMembership));

        List<ClassroomMemberResponse> result = classroomService.listClassroomMembers(classroomId, null);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ClassroomMemberResponse::getEmail)
                .containsExactlyInAnyOrder("student@test.com", "teacher@test.com");

        verify(classroomMembershipRepository).findAllByClassroomId(classroomId);
        verify(classroomMembershipRepository, never())
                .findAllByClassroomIdAndMembershipType(any(), any());
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyStudents_whenRoleIsStudent() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership studentMembership1 = buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, "s1@test.com", RoleName.STUDENT), MembershipType.STUDENT);
        ClassroomMembership studentMembership2 = buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, "s2@test.com", RoleName.STUDENT), MembershipType.STUDENT);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(classroomId, MembershipType.STUDENT))
                .thenReturn(List.of(studentMembership1, studentMembership2));

        List<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.STUDENT);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ClassroomMemberResponse::getMembershipType)
                .containsOnly(MembershipType.STUDENT);

        verify(classroomMembershipRepository)
                .findAllByClassroomIdAndMembershipType(classroomId, MembershipType.STUDENT);
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyTeachers_whenRoleIsTeacher() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership teacherMembership = buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, "teacher@test.com", RoleName.TEACHER), MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(classroomId, MembershipType.TEACHER))
                .thenReturn(List.of(teacherMembership));

        List<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.TEACHER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("teacher@test.com");
        assertThat(result.get(0).getMembershipType()).isEqualTo(MembershipType.TEACHER);
    }

    @Test
    void listClassroomMembers_shouldThrow_whenClassroomDoesNotExist() {
        UUID classroomId = UUID.randomUUID();

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertThrows(ClassroomNotFoundException.class,
                () -> classroomService.listClassroomMembers(classroomId, null));

        verify(classroomMembershipRepository, never()).findAllByClassroomId(any());
        verify(classroomMembershipRepository, never()).findAllByClassroomIdAndMembershipType(any(), any());
    }

    private Classroom buildClassroom(UUID classroomId, UUID orgId) {
        Organization organization = new Organization();
        organization.setId(orgId);

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setOrganization(organization);

        return classroom;
    }

    private User buildUser(UUID userId, UUID orgId, String email, RoleName roleName) {
        Organization organization = new Organization();
        organization.setId(orgId);

        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setOrganization(organization);
        user.setRole(role);

        return user;
    }

    private ClassroomMembership buildMembership(Classroom classroom, User user, MembershipType membershipType) {
        ClassroomMembership membership = new ClassroomMembership();
        membership.setClassroom(classroom);
        membership.setUser(user);
        membership.setMembershipType(membershipType);
        return membership;
    }
}
