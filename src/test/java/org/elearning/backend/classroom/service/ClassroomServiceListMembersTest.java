package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClassroomServiceListMembersTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ClassroomMembershipRepository classroomMembershipRepository;
    @Mock private ClassroomCourseRepository classroomCourseRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private ClassroomService classroomService;

    @Test
    void listClassroomMembers_shouldReturnAllMembers_whenMembershipTypeIsNull() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        ClassroomMembership student = buildMembership(classroom,
                buildUser(UUID.randomUUID(), orgId, "student@test.com", RoleName.STUDENT),
                MembershipType.STUDENT);
        ClassroomMembership teacher = buildMembership(classroom,
                buildUser(UUID.randomUUID(), orgId, "teacher@test.com", RoleName.TEACHER),
                MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student, teacher)));

        PaginatedResponse<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, null, 0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(ClassroomMemberResponse::getEmail)
                .containsExactlyInAnyOrder("student@test.com", "teacher@test.com");
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyStudents_whenMembershipTypeIsStudent() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        ClassroomMembership student = buildMembership(classroom,
                buildUser(UUID.randomUUID(), orgId, "s1@test.com", RoleName.STUDENT),
                MembershipType.STUDENT);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student)));

        PaginatedResponse<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.STUDENT, 0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMembershipType()).isEqualTo(MembershipType.STUDENT);
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyTeachers_whenMembershipTypeIsTeacher() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        ClassroomMembership teacher = buildMembership(classroom,
                buildUser(UUID.randomUUID(), orgId, "teacher@test.com", RoleName.TEACHER),
                MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(teacher)));

        PaginatedResponse<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.TEACHER, 0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("teacher@test.com");
    }

    @Test
    void listClassroomMembers_shouldThrow_whenClassroomDoesNotExist() {
        UUID classroomId = UUID.randomUUID();
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertThrows(ClassroomNotFoundException.class,
                () -> classroomService.listClassroomMembers(classroomId, null, 0, 10, null, null, null));

        verify(classroomMembershipRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listClassroomMembers_shouldReturnCorrectPaginationMetadata() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        ClassroomMembership member = buildMembership(classroom,
                buildUser(UUID.randomUUID(), orgId, "s@test.com", RoleName.STUDENT),
                MembershipType.STUDENT);

        // simulăm page 0, size 5, total 1
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member),
                        org.springframework.data.domain.PageRequest.of(0, 5), 1L));

        PaginatedResponse<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, null, 0, 5, null, null, null);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void listClassroomMembers_withSearch_passesSpecificationToRepository() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        classroomService.listClassroomMembers(classroomId, null, 0, 10, "ion", null, null);

        // verificăm că repository-ul a fost apelat cu Specification (logica de filtrare e în spec)
        verify(classroomMembershipRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listClassroomMembers_withInvalidSortBy_fallsBackToFirstName() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Classroom classroom = buildClassroom(classroomId, orgId);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // nu aruncă excepție — fallback la "firstName"
        assertThat(classroomService.listClassroomMembers(
                classroomId, null, 0, 10, null, "invalidField", "asc"))
                .isNotNull();
    }

    // helpers
    private Classroom buildClassroom(UUID id, UUID orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        Classroom c = new Classroom();
        c.setId(id);
        c.setOrganization(org);
        return c;
    }

    private User buildUser(UUID id, UUID orgId, String email, RoleName roleName) {
        Organization org = new Organization();
        org.setId(orgId);
        Role role = new Role();
        role.setName(roleName);
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName(email.split("@")[0]);
        u.setLastName("Test");
        u.setOrganization(org);
        u.setRole(role);
        return u;
    }

    private ClassroomMembership buildMembership(Classroom c, User u, MembershipType type) {
        ClassroomMembership m = new ClassroomMembership();
        m.setClassroom(c);
        m.setUser(u);
        m.setMembershipType(type);
        return m;
    }
}