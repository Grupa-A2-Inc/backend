package org.elearning.backend.classroom.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.ModifyClassroomStudentsRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomConflictException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ClassroomMembershipRepository classroomMembershipRepository;
    private static final String CLASS_NOT_FOUND = "Classroom not found: ";

    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request, UUID requesterUserId) {
        Organization organization = getRequesterOrganization(requesterUserId);

        if (classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName())) {
            throw new ClassroomConflictException(
                    "Classroom with name '" + request.getName() + "' already exists in this organization"
            );
        }

        Classroom classroom = new Classroom();
        classroom.setOrganization(organization);
        classroom.setName(request.getName().trim());
        classroom.setDescription(request.getDescription());

        return toResponse(classroomRepository.save(classroom));
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> getMyOrganizationClassrooms(UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        return classroomRepository.findAllByOrganizationIdOrderByNameAsc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getClassroomById(UUID classroomId, UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException(CLASS_NOT_FOUND + classroomId));

        return toResponse(classroom);
    }

    @Transactional
    public ClassroomResponse patchClassroom(UUID classroomId, UpdateClassroomRequest request, UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException(CLASS_NOT_FOUND + classroomId));

        if (request.getName() != null && !request.getName().isBlank()) {
            boolean duplicateName = classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.getName())
                    && !classroom.getName().equalsIgnoreCase(request.getName());

            if (duplicateName) {
                throw new ClassroomConflictException(
                        "Classroom with name '" + request.getName() + "' already exists in this organization"
                );
            }

            classroom.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            classroom.setDescription(request.getDescription());
        }

        return toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    public void deleteClassroom(UUID classroomId, UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException(CLASS_NOT_FOUND + classroomId));

        classroomRepository.delete(classroom);
    }

    private Organization getRequesterOrganization(UUID requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist: " + requesterUserId));

        if (requester.getOrganization() == null) {
            throw new ClassroomBadRequestException("Authenticated user is not assigned to an organization");
        }

        UUID organizationId = requester.getOrganization().getId();

        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ClassroomBadRequestException("Organization not found: " + organizationId));
    }

    private ClassroomResponse toResponse(Classroom classroom) {
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getOrganization().getId(),
                classroom.getName(),
                classroom.getDescription(),
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }

    @Transactional
    public ClassroomResponse addClassroomStudents(UUID classroomId,
                                                  ModifyClassroomStudentsRequest request,
                                                  UUID currentUserId) {
        Classroom classroom = getClassroomOrThrow(classroomId);
        List<User> students = getValidStudentsForClassroom(request.getStudentIds(), classroom);

        for (User student : students) {
            boolean alreadyExists = classroomMembershipRepository
                    .existsByClassroomIdAndUserIdAndMembershipType(
                            classroomId, student.getId(), MembershipType.STUDENT
                    );

            if (!alreadyExists) {
                ClassroomMembership membership = new ClassroomMembership();
                membership.setClassroom(classroom);
                membership.setUser(student);
                membership.setMembershipType(MembershipType.STUDENT);
                classroomMembershipRepository.save(membership);
            }
        }

        return toResponse(classroom);
    }

    @Transactional
    public ClassroomResponse deleteClassroomStudents(UUID classroomId,
                                                     @Valid ModifyClassroomStudentsRequest request,
                                                     UUID currentUserId) {
        Classroom classroom = getClassroomOrThrow(classroomId);
        List<User> students = getValidStudentsForClassroom(request.getStudentIds(), classroom);

        for (User student : students) {
            classroomMembershipRepository.deleteByClassroomIdAndUserIdAndMembershipType(
                    classroomId,
                    student.getId(),
                    MembershipType.STUDENT
            );
        }

        return toResponse(classroom);
    }

    private Classroom getClassroomOrThrow(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found."));
    }

    private List<User> getValidStudentsForClassroom(Set<UUID> studentIds, Classroom classroom) {
        List<User> students = userRepository.findAllById(studentIds);

        if (students.size() != studentIds.size()) {
            throw new UserNotFoundException("One or more students do not exist.");
        }

        UUID classroomOrganizationId = classroom.getOrganization().getId();

        for (User student : students) {
            validateStudentCanBeModified(student, classroomOrganizationId);
        }

        return students;
    }

    private void validateStudentCanBeModified(User student, UUID classroomOrganizationId) {
        if (student.getRole().getName() != RoleName.STUDENT) {
            throw new ClassroomBadRequestException("User " + student.getId() + " is not a STUDENT.");
        }

        if (student.getOrganization() == null || !classroomOrganizationId.equals(student.getOrganization().getId())) {
            throw new ClassroomBadRequestException("User " + student.getId() + " is not in the same organization.");
        }
    }

    // TODO: aici pregatesc partea pentru auto enrollment
    private void handleStudentAddedToClassroom(Classroom classroom, User student) {
        throw new UnsupportedOperationException(
                "Auto enrollment is not implemented yet for classroom "
                        + classroom.getId()
                        + " and student "
                        + student.getId()
        );
    }
}