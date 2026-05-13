package org.elearning.backend.organization.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationDeletionService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TestRepository testRepository;

    @Transactional
    public void deleteOrganizationOwnedByAdmin(UUID ownerUserId) {
        Organization organization = organizationRepository.findFirstByOwnerId(ownerUserId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for owner: " + ownerUserId));

        List<User> users = userRepository.findByOrganizationId(organization.getId());
        Set<UUID> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Course> courses = courseRepository.findByCreatedByIn(userIds);
        Set<UUID> lessonIds = extractLessonIds(courses);

        if (!lessonIds.isEmpty()) {
            List<Test> testsForLessons = testRepository.findByLessonIdIn(lessonIds);
            if (!testsForLessons.isEmpty()) {
                testRepository.deleteAll(testsForLessons);
            }
        }

        if (!courses.isEmpty()) {
            courseRepository.deleteAll(courses);
        }

        List<Test> testsCreatedByUsers = testRepository.findByCreatedByIn(userIds);
        if (!testsCreatedByUsers.isEmpty()) {
            testRepository.deleteAll(testsCreatedByUsers);
        }

        User owner = organization.getOwner();
        for (User user : users) {
            if (!user.getId().equals(owner.getId())) {
                userRepository.delete(user);
            }
        }

        owner.setOrganization(null);
        userRepository.save(owner);

        organizationRepository.delete(organization);
        userRepository.delete(owner);
    }

    private Set<UUID> extractLessonIds(List<Course> courses) {
        Set<UUID> lessonIds = new LinkedHashSet<>();
        for (Course course : courses) {
            if (course.getChapters() == null) {
                continue;
            }
            for (Chapter chapter : course.getChapters()) {
                if (chapter.getLessons() == null) {
                    continue;
                }
                for (Lesson lesson : chapter.getLessons()) {
                    lessonIds.add(lesson.getId());
                }
            }
        }
        return lessonIds;
    }
}
