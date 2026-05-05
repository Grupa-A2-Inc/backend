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
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.subscription.service.EntitlementService;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    private static final String CLASSROOM_NOT_FOUND = "Classroom not found: ";
    private static final String CLASSROOM_NAME_CONFLICT_TEMPLATE =
            "Classroom with name '%s' already exists in this organization";
    private static final String DEFAULT_CLASSROOM_SORT_FIELD = "name";
    private static final String DEFAULT_MEMBER_SORT_FIELD = "firstName";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String LAST_NAME_FIELD = "lastName";
    private static final String EMAIL_FIELD = "email";
    private static final String USER_SORT_PREFIX = "user.";
    private static final String ASC_SORT_DIRECTION = "asc";
    private static final String DESC_SORT_DIRECTION = "desc";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Set<String> CLASSROOM_ALLOWED_SORT_FIELDS = Set.of(
            DEFAULT_CLASSROOM_SORT_FIELD,
            CREATED_AT_FIELD
    );
    private static final Set<String> CLASSROOM_MEMBER_ALLOWED_SORT_FIELDS = Set.of(
            DEFAULT_MEMBER_SORT_FIELD,
            LAST_NAME_FIELD,
            EMAIL_FIELD
    );

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ClassroomMembershipRepository classroomMembershipRepository;
    private final ClassroomCourseRepository classroomCourseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EntitlementService entitlementService;

    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request, UUID requesterUserId) {
        Organization organization = getRequesterOrganization(requesterUserId);

        entitlementService.canCreateClassroom(organization.getId());

        if (classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName())) {
            throw new ClassroomConflictException(
                    CLASSROOM_NAME_CONFLICT_TEMPLATE.formatted(request.getName())
            );
        }

        Classroom classroom = new Classroom();
        classroom.setOrganization(organization);
        classroom.setName(request.getName().trim());
        classroom.setDescription(request.getDescription());

        return toResponse(classroomRepository.save(classroom));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ClassroomResponse> getMyOrganizationClassrooms(
            UUID requesterUserId,
            Integer page, Integer size,
            String search, String sortBy, String sortDir) {

        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        int pageVal = normalizePage(page);
        int sizeVal = normalizeSize(size);
        String field = normalizeSortField(sortBy, CLASSROOM_ALLOWED_SORT_FIELDS, DEFAULT_CLASSROOM_SORT_FIELD);
        String dir = normalizeSortDirection(sortDir);

        Sort sort = DESC_SORT_DIRECTION.equals(dir) ? Sort.by(field).descending() : Sort.by(field).ascending();
        Pageable pageable = PageRequest.of(pageVal, sizeVal, sort);

        Specification<Classroom> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId)
        );

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), like));
        }

        Page<Classroom> resultPage = classroomRepository.findAll(spec, pageable);

        return new PaginatedResponse<>(
                resultPage.getContent().stream().map(this::toResponse).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getClassroomById(UUID classroomId, UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException(CLASSROOM_NOT_FOUND + classroomId));

        return toResponse(classroom);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ClassroomResponse> getMyClassrooms(
            UUID userId,
            Integer page, Integer size,
            String search, String sortBy, String sortDir) {

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist: " + userId));

        int pageVal  = (page == null || page < 0)   ? 0  : page;
        int sizeVal  = (size == null || size <= 0)   ? 10 : size;
        String field = (sortBy != null && Set.of("name", "createdAt").contains(sortBy)) ? sortBy : "name";
        String dir   = (sortDir == null || sortDir.isBlank()) ? "asc" : sortDir.toLowerCase();

        Sort sort = dir.equals("desc")
                ? Sort.by("classroom." + field).descending()
                : Sort.by("classroom." + field).ascending();
        Pageable pageable = PageRequest.of(pageVal, sizeVal, sort);

        Specification<ClassroomMembership> spec = Specification.where(
                (root, query, cb) -> {
                    query.distinct(true);
                    return cb.equal(root.get("user").get("id"), userId);
                }
        );

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("classroom").get("name")), like));
        }

        Page<ClassroomMembership> resultPage = classroomMembershipRepository.findAll(spec, pageable);

        return new PaginatedResponse<>(
                resultPage.getContent().stream()
                        .map(m -> toResponse(m.getClassroom()))
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    @Transactional
    public ClassroomResponse patchClassroom(UUID classroomId, UpdateClassroomRequest request, UUID requesterUserId) {
        UUID organizationId = getRequesterOrganization(requesterUserId).getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException(CLASSROOM_NOT_FOUND + classroomId));

        if (request.getName() != null && !request.getName().isBlank()) {
            boolean duplicateName = classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.getName())
                    && !classroom.getName().equalsIgnoreCase(request.getName());

            if (duplicateName) {
                throw new ClassroomConflictException(
                        CLASSROOM_NAME_CONFLICT_TEMPLATE.formatted(request.getName())
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
                .orElseThrow(() -> new ClassroomNotFoundException(CLASSROOM_NOT_FOUND + classroomId));

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

    @Transactional(readOnly = true)
    public PaginatedResponse<ClassroomMemberResponse> listClassroomMembers(
            UUID classroomId, MembershipType membershipType,
            Integer page, Integer size,
            String search, String sortBy, String sortDir) {

        classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found"));

        int pageVal = normalizePage(page);
        int sizeVal = normalizeSize(size);
        String field = normalizeSortField(sortBy, CLASSROOM_MEMBER_ALLOWED_SORT_FIELDS, DEFAULT_MEMBER_SORT_FIELD);
        String dir = normalizeSortDirection(sortDir);

        Sort sort = DESC_SORT_DIRECTION.equals(dir)
                ? Sort.by(USER_SORT_PREFIX + field).descending()
                : Sort.by(USER_SORT_PREFIX + field).ascending();
        Pageable pageable = PageRequest.of(pageVal, sizeVal, sort);

        Specification<ClassroomMembership> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("classroom").get("id"), classroomId)
        );

        if (membershipType != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("membershipType"), membershipType));
        }

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("user").get(DEFAULT_MEMBER_SORT_FIELD)), like),
                    cb.like(cb.lower(root.get("user").get(LAST_NAME_FIELD)), like),
                    cb.like(cb.lower(root.get("user").get(EMAIL_FIELD)), like)
            ));
        }

        Page<ClassroomMembership> resultPage = classroomMembershipRepository.findAll(spec, pageable);

        return new PaginatedResponse<>(
                resultPage.getContent().stream()
                        .map(m -> new ClassroomMemberResponse(
                                m.getUser().getId(),
                                m.getUser().getEmail(),
                                m.getMembershipType()
                        ))
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }


    private Classroom getClassroomOrThrow(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found."));
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
    }

    private String normalizeSortField(String sortBy, Set<String> allowedFields, String defaultField) {
        return sortBy != null && allowedFields.contains(sortBy) ? sortBy : defaultField;
    }

    private String normalizeSortDirection(String sortDir) {
        return sortDir == null || sortDir.isBlank()
                ? ASC_SORT_DIRECTION
                : sortDir.toLowerCase(Locale.ROOT);
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
