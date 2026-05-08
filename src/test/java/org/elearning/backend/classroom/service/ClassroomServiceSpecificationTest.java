package org.elearning.backend.classroom.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceSpecificationTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ClassroomMembershipRepository classroomMembershipRepository;
    @Mock private ClassroomCourseRepository classroomCourseRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private ClassroomService classroomService;

    @Test
    void getMyOrganizationClassrooms_executesOrganizationAndSearchSpecification() {
        User requester = userWithOrganization(RoleName.ORGANIZATION_ADMIN);
        UUID organizationId = requester.getOrganization().getId();

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(requester.getOrganization()));
        when(classroomRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        classroomService.getMyOrganizationClassrooms(requester.getId(), 0, 10, "Math", "name", "asc");

        ArgumentCaptor<Specification<Classroom>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(classroomRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Classroom> spec = captor.getValue();

        Root<Classroom> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> organizationPath = mock(Path.class);
        Path<Object> organizationIdPath = mock(Path.class);
        Path namePath = mock(Path.class);
        Expression<String> loweredName = mock(Expression.class);
        Predicate organizationPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("organization")).thenReturn(organizationPath);
        when(organizationPath.get("id")).thenReturn(organizationIdPath);
        when(root.get("name")).thenReturn(namePath);
        when(cb.equal(organizationIdPath, organizationId)).thenReturn(organizationPredicate);
        when(cb.lower(namePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%math%")).thenReturn(searchPredicate);
        when(cb.and(organizationPredicate, searchPredicate)).thenReturn(combinedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(combinedPredicate);
    }

    @Test
    void getMyClassrooms_executesUserAndSearchSpecification() {
        User user = userWithOrganization(RoleName.STUDENT);
        UUID userId = user.getId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        classroomService.getMyClassrooms(userId, 0, 10, "Math", "name", "asc");

        ArgumentCaptor<Specification<ClassroomMembership>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(classroomMembershipRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<ClassroomMembership> spec = captor.getValue();

        Root<ClassroomMembership> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Object> userIdPath = mock(Path.class);
        Path<Object> classroomPath = mock(Path.class);
        Path classroomNamePath = mock(Path.class);
        Expression<String> loweredName = mock(Expression.class);
        Predicate userPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("user")).thenReturn(userPath);
        when(userPath.get("id")).thenReturn(userIdPath);
        when(root.get("classroom")).thenReturn(classroomPath);
        when(classroomPath.get("name")).thenReturn(classroomNamePath);
        when(cb.equal(userIdPath, userId)).thenReturn(userPredicate);
        when(cb.lower(classroomNamePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%math%")).thenReturn(searchPredicate);
        when(cb.and(userPredicate, searchPredicate)).thenReturn(combinedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(combinedPredicate);
    }

    @Test
    void listClassroomMembers_executesClassroomMembershipTypeAndSearchSpecification() {
        UUID classroomId = UUID.randomUUID();

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(new Classroom()));
        when(classroomMembershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PaginatedResponse<?> response = classroomService.listClassroomMembers(
                classroomId,
                MembershipType.TEACHER,
                0,
                10,
                "teach",
                "email",
                "asc"
        );

        assertThat(response.getContent()).isEmpty();

        ArgumentCaptor<Specification<ClassroomMembership>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(classroomMembershipRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<ClassroomMembership> spec = captor.getValue();

        Root<ClassroomMembership> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> classroomPath = mock(Path.class);
        Path<Object> classroomIdPath = mock(Path.class);
        Path<Object> membershipTypePath = mock(Path.class);
        Path<Object> userPath = mock(Path.class);
        Path firstNamePath = mock(Path.class);
        Path lastNamePath = mock(Path.class);
        Path emailPath = mock(Path.class);
        Expression<String> loweredFirstName = mock(Expression.class);
        Expression<String> loweredLastName = mock(Expression.class);
        Expression<String> loweredEmail = mock(Expression.class);
        Predicate classroomPredicate = mock(Predicate.class);
        Predicate membershipPredicate = mock(Predicate.class);
        Predicate withMembershipPredicate = mock(Predicate.class);
        Predicate firstNamePredicate = mock(Predicate.class);
        Predicate lastNamePredicate = mock(Predicate.class);
        Predicate emailPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("classroom")).thenReturn(classroomPath);
        when(classroomPath.get("id")).thenReturn(classroomIdPath);
        when(root.get("membershipType")).thenReturn(membershipTypePath);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.get("firstName")).thenReturn(firstNamePath);
        when(userPath.get("lastName")).thenReturn(lastNamePath);
        when(userPath.get("email")).thenReturn(emailPath);

        when(cb.equal(classroomIdPath, classroomId)).thenReturn(classroomPredicate);
        when(cb.equal(membershipTypePath, MembershipType.TEACHER)).thenReturn(membershipPredicate);
        when(cb.and(classroomPredicate, membershipPredicate)).thenReturn(withMembershipPredicate);

        when(cb.lower(firstNamePath)).thenReturn(loweredFirstName);
        when(cb.lower(lastNamePath)).thenReturn(loweredLastName);
        when(cb.lower(emailPath)).thenReturn(loweredEmail);
        when(cb.like(loweredFirstName, "%teach%")).thenReturn(firstNamePredicate);
        when(cb.like(loweredLastName, "%teach%")).thenReturn(lastNamePredicate);
        when(cb.like(loweredEmail, "%teach%")).thenReturn(emailPredicate);
        when(cb.or(firstNamePredicate, lastNamePredicate, emailPredicate)).thenReturn(searchPredicate);
        when(cb.and(withMembershipPredicate, searchPredicate)).thenReturn(combinedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(combinedPredicate);
    }

    private User userWithOrganization(RoleName roleName) {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());

        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setOrganization(organization);
        return user;
    }
}
