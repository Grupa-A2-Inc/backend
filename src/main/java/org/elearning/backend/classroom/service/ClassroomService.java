package org.elearning.backend.classroom.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.ModifyClassroomMembersRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomConflictException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
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
    private final ClassroomCourseRepository classroomCourseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

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
    public ClassroomResponse addClassroomMembers(UUID classroomId,
                                                  ModifyClassroomMembersRequest request,
                                                  UUID currentUserId) {
        Classroom classroom = getClassroomOrThrow(classroomId);

        //metoda cu getValid trebuie modificata
        List<User> members = getValidMembersForClassroom(request.getMemberIds(), classroom);

        for (User member : members) {

            //poate fi ori student, ori teacher
            MembershipType membershipType = resolveMembershipType(member);

            boolean alreadyExists = classroomMembershipRepository
                    .existsByClassroomIdAndUserIdAndMembershipType(
                            classroomId, member.getId(), membershipType
                    );

            if (!alreadyExists) {
                ClassroomMembership membership = new ClassroomMembership();
                membership.setClassroom(classroom);
                membership.setUser(member);
                membership.setMembershipType(membershipType);
                classroomMembershipRepository.save(membership);

                if (membershipType == MembershipType.STUDENT) {
                    handleStudentAddedToClassroom(classroom, member);
                }
            }
        }

        return toResponse(classroom);
    }

    @Transactional
    public ClassroomResponse deleteClassroomMembers(UUID classroomId,
                                                     ModifyClassroomMembersRequest request,
                                                     UUID currentUserId) {
        Classroom classroom = getClassroomOrThrow(classroomId);
        List<User> members = getValidMembersForClassroom(request.getMemberIds(), classroom);

        for (User member : members) {

            MembershipType membershipType = resolveMembershipType(member);

            classroomMembershipRepository.deleteByClassroomIdAndUserIdAndMembershipType(
                    classroomId,
                    member.getId(),
                    membershipType
            );
        }

        return toResponse(classroom);
    }

    public List<ClassroomMemberResponse> listClassroomMembers(UUID classroomId, MembershipType membershipType){

        classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found"));

        List<ClassroomMembership> classroomMemberships;
        if(membershipType == null){
            classroomMemberships = classroomMembershipRepository.findAllByClassroomId(classroomId);
        }
        else{
           classroomMemberships =  classroomMembershipRepository.findAllByClassroomIdAndMembershipType(classroomId, membershipType);
        }

        return classroomMemberships.stream()
                .map(membership -> new ClassroomMemberResponse(
                        membership.getUser().getId(),
                        membership.getUser().getEmail(),
                        membership.getMembershipType()
                ))
                .toList();

    }


    private Classroom getClassroomOrThrow(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found."));
    }

    private List<User> getValidMembersForClassroom(Set<UUID> memberIds, Classroom classroom) {
        List<User> members = userRepository.findAllById(memberIds);

        if (members.size() != memberIds.size()) {
            throw new UserNotFoundException("One or more members do not exist.");
        }

        UUID classroomOrganizationId = classroom.getOrganization().getId();

        for (User member : members) {
            validateMemberCanBeModified(member, classroomOrganizationId);
        }

        return members;
    }

    private void validateMemberCanBeModified(User member, UUID classroomOrganizationId) {

        if (member.getOrganization() == null
                || !classroomOrganizationId.equals(member.getOrganization().getId())) {
            throw new ClassroomBadRequestException(
                    "User " + member.getId() + " is not in the same organization."
            );
        }

        RoleName roleName = member.getRole().getName();
        if (roleName != RoleName.STUDENT && roleName != RoleName.TEACHER) {
            throw new ClassroomBadRequestException(
                    "User " + member.getId() + " is neither STUDENT nor TEACHER."
            );
        }
    }

    private void handleStudentAddedToClassroom(Classroom classroom, User student) {
        List<ClassroomCourse> classroomCourses = classroomCourseRepository
                .findAllByClassroomId(classroom.getId());

        for (ClassroomCourse classroomCourse : classroomCourses) {
            UUID courseId = classroomCourse.getCourseId();
            UUID studentId = student.getId();

            if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                CourseEnrollment enrollment = new CourseEnrollment();
                enrollment.setCourseId(courseId);
                enrollment.setStudentId(studentId);
                courseEnrollmentRepository.save(enrollment);
            }
        }
    }

    private MembershipType resolveMembershipType(User member) {
        RoleName roleName = member.getRole().getName();

        return switch (roleName) {
            case STUDENT -> MembershipType.STUDENT;
            case TEACHER -> MembershipType.TEACHER;
            default -> throw new ClassroomBadRequestException(
                    "User " + member.getId() + " cannot be added to classroom."
            );
        };
    }
}
