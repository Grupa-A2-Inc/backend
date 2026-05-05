package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
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
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClassroomServiceGetMyClassroomsTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private ClassroomMembershipRepository classroomMembershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ClassroomCourseRepository classroomCourseRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private ClassroomService classroomService;

    @Test
    void getMyClassrooms_returnsClassroomsForStudent() {
        UUID userId = UUID.randomUUID();
        User student = buildUser(userId, RoleName.STUDENT);

        ClassroomMembership membership = buildMembership(student, MembershipType.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership), PageRequest.of(0, 10), 1L));

        PaginatedResponse<ClassroomResponse> result =
                classroomService.getMyClassrooms(userId, 0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Class");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getMyClassrooms_returnsClassroomsForTeacher() {
        UUID userId = UUID.randomUUID();
        User teacher = buildUser(userId, RoleName.TEACHER);

        ClassroomMembership membership = buildMembership(teacher, MembershipType.TEACHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(teacher));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership), PageRequest.of(0, 10), 1L));

        PaginatedResponse<ClassroomResponse> result =
                classroomService.getMyClassrooms(userId, 0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getMyClassrooms_returnsEmptyList_whenUserHasNoMemberships() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, RoleName.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L));

        PaginatedResponse<ClassroomResponse> result =
                classroomService.getMyClassrooms(userId, 0, 10, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void getMyClassrooms_throwsUserNotFound_whenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> classroomService.getMyClassrooms(userId, 0, 10, null, null, null));

        verify(classroomMembershipRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getMyClassrooms_withSearch_passesSpecToRepository() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, RoleName.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L));

        classroomService.getMyClassrooms(userId, 0, 10, "math", null, null);

        verify(classroomMembershipRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getMyClassrooms_returnsCorrectPaginationMetadata() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, RoleName.STUDENT);

        ClassroomMembership m1 = buildMembership(user, MembershipType.STUDENT);
        ClassroomMembership m2 = buildMembership(user, MembershipType.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 5), 2L));

        PaginatedResponse<ClassroomResponse> result =
                classroomService.getMyClassrooms(userId, 0, 5, null, null, null);

        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getMyClassrooms_withInvalidSortBy_fallsBackToName() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, RoleName.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L));

        assertThat(classroomService.getMyClassrooms(userId, 0, 10, null, "invalidField", "asc"))
                .isNotNull();
    }

    @Test
    void getMyClassrooms_withNullPageAndSize_usesDefaults() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, RoleName.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L));

        PaginatedResponse<ClassroomResponse> result =
                classroomService.getMyClassrooms(userId, null, null, null, null, null);

        assertThat(result).isNotNull();
        verify(classroomMembershipRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private User buildUser(UUID id, RoleName roleName) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());

        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setOrganization(org);
        user.setRole(role);
        return user;
    }

    private ClassroomMembership buildMembership(User user, MembershipType type) {
        Organization org = user.getOrganization();

        Classroom classroom = new Classroom();
        classroom.setId(UUID.randomUUID());
        classroom.setOrganization(org);
        classroom.setName("Test Class");
        classroom.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
        classroom.setUpdatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));

        ClassroomMembership membership = new ClassroomMembership();
        membership.setUser(user);
        membership.setClassroom(classroom);
        membership.setMembershipType(type);
        return membership;
    }
}