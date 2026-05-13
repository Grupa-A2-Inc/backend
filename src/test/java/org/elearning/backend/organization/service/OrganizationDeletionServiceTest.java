package org.elearning.backend.organization.service;

import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OrganizationDeletionServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TestRepository testRepository;

    @InjectMocks
    private OrganizationDeletionService organizationDeletionService;

    @Test
    void deleteOrganizationOwnedByAdmin_deletesOrganizationTreeInOrder() {
        UUID ownerId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        User owner = user(ownerId, RoleName.ORGANIZATION_ADMIN);
        User teacher = user(teacherId, RoleName.TEACHER);

        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setOwner(owner);
        owner.setOrganization(organization);
        teacher.setOrganization(organization);

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setCreatedBy(teacherId);
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        Chapter chapter = new Chapter();
        chapter.setLessons(List.of(lesson));
        course.setChapters(List.of(chapter));

        org.elearning.backend.assessment.model.Test lessonTest = new org.elearning.backend.assessment.model.Test();
        lessonTest.setId(UUID.randomUUID());
        org.elearning.backend.assessment.model.Test userOwnedTest = new org.elearning.backend.assessment.model.Test();
        userOwnedTest.setId(UUID.randomUUID());

        when(organizationRepository.findFirstByOwnerId(ownerId)).thenReturn(Optional.of(organization));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(owner, teacher));
        when(courseRepository.findByCreatedByIn(any())).thenReturn(List.of(course));
        when(testRepository.findByLessonIdIn(Set.of(lessonId))).thenReturn(List.of(lessonTest));
        when(testRepository.findByCreatedByIn(any())).thenReturn(List.of(userOwnedTest));

        organizationDeletionService.deleteOrganizationOwnedByAdmin(ownerId);

        assertThat(owner.getOrganization()).isNull();

        InOrder ordered = inOrder(testRepository, courseRepository, userRepository, organizationRepository);
        ordered.verify(testRepository).deleteAll(List.of(lessonTest));
        ordered.verify(courseRepository).deleteAll(List.of(course));
        ordered.verify(testRepository).deleteAll(List.of(userOwnedTest));
        ordered.verify(userRepository).delete(teacher);
        ordered.verify(userRepository).save(owner);
        ordered.verify(organizationRepository).delete(organization);
        ordered.verify(userRepository).delete(owner);
    }

    @Test
    void deleteOrganizationOwnedByAdmin_missingOrganization_throws() {
        UUID ownerId = UUID.randomUUID();
        when(organizationRepository.findFirstByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationDeletionService.deleteOrganizationOwnedByAdmin(ownerId))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessage("Organization not found for owner: " + ownerId);

        verify(userRepository, never()).findByOrganizationId(ownerId);
    }

    @Test
    void deleteOrganizationOwnedByAdmin_skipsLessonAndTestDeletionWhenCourseGraphIsEmpty() {
        UUID ownerId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        User owner = user(ownerId, RoleName.ORGANIZATION_ADMIN);
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setOwner(owner);
        owner.setOrganization(organization);

        Course noChaptersCourse = new Course();
        noChaptersCourse.setId(UUID.randomUUID());
        noChaptersCourse.setChapters(null);

        Chapter chapterWithoutLessons = new Chapter();
        chapterWithoutLessons.setLessons(null);
        Course chapterWithoutLessonsCourse = new Course();
        chapterWithoutLessonsCourse.setId(UUID.randomUUID());
        chapterWithoutLessonsCourse.setChapters(List.of(chapterWithoutLessons));

        when(organizationRepository.findFirstByOwnerId(ownerId)).thenReturn(Optional.of(organization));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(owner));
        when(courseRepository.findByCreatedByIn(any())).thenReturn(List.of(noChaptersCourse, chapterWithoutLessonsCourse));
        when(testRepository.findByCreatedByIn(any())).thenReturn(List.of());

        organizationDeletionService.deleteOrganizationOwnedByAdmin(ownerId);

        verify(testRepository, never()).findByLessonIdIn(any());
        verify(testRepository, never()).deleteAll(any());
        verify(courseRepository).deleteAll(List.of(noChaptersCourse, chapterWithoutLessonsCourse));
        verify(userRepository).save(owner);
        verify(organizationRepository).delete(organization);
        verify(userRepository).delete(owner);
        verify(userRepository, times(1)).delete(owner);
    }

    @Test
    void deleteOrganizationOwnedByAdmin_skipsLessonTestDeletionWhenNoTestsMatchLessons() {
        UUID ownerId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        User owner = user(ownerId, RoleName.ORGANIZATION_ADMIN);
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setOwner(owner);
        owner.setOrganization(organization);

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        Chapter chapter = new Chapter();
        chapter.setLessons(List.of(lesson));
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setChapters(List.of(chapter));

        when(organizationRepository.findFirstByOwnerId(ownerId)).thenReturn(Optional.of(organization));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(owner));
        when(courseRepository.findByCreatedByIn(any())).thenReturn(List.of(course));
        when(testRepository.findByLessonIdIn(Set.of(lessonId))).thenReturn(List.of());
        when(testRepository.findByCreatedByIn(any())).thenReturn(List.of());

        organizationDeletionService.deleteOrganizationOwnedByAdmin(ownerId);

        verify(testRepository).findByLessonIdIn(Set.of(lessonId));
        verify(testRepository, never()).deleteAll(any());
        verify(courseRepository).deleteAll(List.of(course));
        verify(organizationRepository).delete(organization);
        verify(userRepository).delete(owner);
    }

    private User user(UUID id, RoleName roleName) {
        User user = new User();
        user.setId(id);
        user.setRole(new Role(roleName));
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setFirstName(roleName.name());
        user.setLastName("User");
        return user;
    }
}
