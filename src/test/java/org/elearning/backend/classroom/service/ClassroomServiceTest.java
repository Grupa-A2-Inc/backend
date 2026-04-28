package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.dto.request.ModifyClassroomMembersRequest;
import org.elearning.backend.classroom.exception.ClassroomConflictException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

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
    void createClassroom_savesClassroomForRequesterOrganization() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        CreateClassroomRequest request = new CreateClassroomRequest("  Class A  ", "Primary class");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName()))
                .thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(invocation -> {
            Classroom classroom = invocation.getArgument(0);
            classroom.setId(UUID.randomUUID());
            classroom.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
            classroom.setUpdatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
            return classroom;
        });

        ClassroomResponse response = classroomService.createClassroom(request, requester.getId());

        ArgumentCaptor<Classroom> captor = ArgumentCaptor.forClass(Classroom.class);
        verify(classroomRepository).save(captor.capture());

        Classroom saved = captor.getValue();
        assertThat(saved.getOrganization()).isSameAs(organization);
        assertThat(saved.getName()).isEqualTo("Class A");
        assertThat(saved.getDescription()).isEqualTo("Primary class");

        assertThat(response.getOrganizationId()).isEqualTo(organization.getId());
        assertThat(response.getName()).isEqualTo("Class A");
        assertThat(response.getDescription()).isEqualTo("Primary class");
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 24, 10, 0));
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 24, 10, 0));
    }

    @Test
    void createClassroom_throwsConflictWhenNameAlreadyExistsInOrganization() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        CreateClassroomRequest request = new CreateClassroomRequest("Class A", "Duplicate");
        UUID requesterId = requester.getId();

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName()))
                .thenReturn(true);

        ClassroomConflictException exception = assertThrows(
                ClassroomConflictException.class,
                () -> classroomService.createClassroom(request, requesterId)
        );

        assertThat(exception.getMessage()).contains("Class A");
        verify(classroomRepository, never()).save(any(Classroom.class));
    }

    @Test
    void createClassroom_throwsWhenRequesterUserDoesNotExist() {
        UUID requesterId = UUID.randomUUID();
        CreateClassroomRequest request = new CreateClassroomRequest("Class A", null);
        when(userRepository.findById(requesterId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> classroomService.createClassroom(request, requesterId)
        );

        assertThat(exception.getMessage()).contains(requesterId.toString());
    }

    @Test
    void createClassroom_throwsWhenRequesterHasNoOrganization() {
        User requester = makeUserWithoutOrganization();
        UUID requesterId = requester.getId();
        CreateClassroomRequest request = new CreateClassroomRequest("Class A", null);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));

        ClassroomBadRequestException exception = assertThrows(
                ClassroomBadRequestException.class,
                () -> classroomService.createClassroom(request, requesterId)
        );

        assertThat(exception.getMessage()).isEqualTo("Authenticated user is not assigned to an organization");
    }

    @Test
    void createClassroom_throwsWhenRequesterOrganizationCannotBeLoaded() {
        User requester = makeUserWithOrganization();
        UUID requesterId = requester.getId();
        UUID organizationId = requester.getOrganization().getId();
        CreateClassroomRequest request = new CreateClassroomRequest("Class A", null);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        ClassroomBadRequestException exception = assertThrows(
                ClassroomBadRequestException.class,
                () -> classroomService.createClassroom(request, requesterId)
        );

        assertThat(exception.getMessage()).contains(organizationId.toString());
    }

    @Test
    void getMyOrganizationClassrooms_returnsMappedClassrooms() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom first = makeClassroom(organization, "A", "First");
        Classroom second = makeClassroom(organization, "B", "Second");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findAllByOrganizationIdOrderByNameAsc(organization.getId()))
                .thenReturn(List.of(first, second));

        List<ClassroomResponse> responses = classroomService.getMyOrganizationClassrooms(requester.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("A");
        assertThat(responses.get(1).getDescription()).isEqualTo("Second");
    }

    @Test
    void getClassroomById_returnsClassroomFromRequesterOrganization() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "A", "Details");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));

        ClassroomResponse response = classroomService.getClassroomById(classroom.getId(), requester.getId());

        assertThat(response.getId()).isEqualTo(classroom.getId());
        assertThat(response.getOrganizationId()).isEqualTo(organization.getId());
        assertThat(response.getName()).isEqualTo("A");
    }

    @Test
    void getClassroomById_throwsWhenClassroomIsMissing() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        UUID classroomId = UUID.randomUUID();
        UUID requesterId = requester.getId();

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, organization.getId()))
                .thenReturn(Optional.empty());

        ClassroomNotFoundException exception = assertThrows(
                ClassroomNotFoundException.class,
                () -> classroomService.getClassroomById(classroomId, requesterId)
        );

        assertThat(exception.getMessage()).contains(classroomId.toString());
    }

    @Test
    void patchClassroom_updatesNameAndDescription() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Old", "Old desc");
        UpdateClassroomRequest request = new UpdateClassroomRequest("  New Name  ", "New desc");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName()))
                .thenReturn(false);
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.patchClassroom(classroom.getId(), request, requester.getId());

        assertThat(classroom.getName()).isEqualTo("New Name");
        assertThat(classroom.getDescription()).isEqualTo("New desc");
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getDescription()).isEqualTo("New desc");
    }

    @Test
    void patchClassroom_allowsSameNameWithDifferentCase() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Class A", "Old desc");
        UpdateClassroomRequest request = new UpdateClassroomRequest("class a", "Updated desc");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName()))
                .thenReturn(true);
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.patchClassroom(classroom.getId(), request, requester.getId());

        assertThat(classroom.getName()).isEqualTo("class a");
        assertThat(response.getName()).isEqualTo("class a");
        assertThat(classroom.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    void patchClassroom_ignoresBlankNameAndNullDescription() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Keep", "Keep desc");
        UpdateClassroomRequest request = new UpdateClassroomRequest("   ", null);

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.patchClassroom(classroom.getId(), request, requester.getId());

        assertThat(classroom.getName()).isEqualTo("Keep");
        assertThat(classroom.getDescription()).isEqualTo("Keep desc");
        assertThat(response.getName()).isEqualTo("Keep");
    }

    @Test
    void patchClassroom_updatesDescriptionWhenNameIsNull() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Keep", "Old desc");
        UpdateClassroomRequest request = new UpdateClassroomRequest(null, "New desc");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.patchClassroom(classroom.getId(), request, requester.getId());

        assertThat(classroom.getName()).isEqualTo("Keep");
        assertThat(classroom.getDescription()).isEqualTo("New desc");
        assertThat(response.getName()).isEqualTo("Keep");
        assertThat(response.getDescription()).isEqualTo("New desc");
    }

    @Test
    void patchClassroom_throwsConflictWhenRenamingToAnotherExistingClassroom() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Old", "Old desc");
        UpdateClassroomRequest request = new UpdateClassroomRequest("New", "New desc");
        UUID requesterId = requester.getId();
        UUID classroomId = classroom.getId();

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, organization.getId()))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), request.getName()))
                .thenReturn(true);

        ClassroomConflictException exception = assertThrows(
                ClassroomConflictException.class,
                () -> classroomService.patchClassroom(classroomId, request, requesterId)
        );

        assertThat(exception.getMessage()).contains("New");
        verify(classroomRepository, never()).save(classroom);
    }

    @Test
    void patchClassroom_throwsWhenClassroomIsMissing() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        UUID classroomId = UUID.randomUUID();
        UUID requesterId = requester.getId();
        UpdateClassroomRequest request = new UpdateClassroomRequest("X", "Y");

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, organization.getId()))
                .thenReturn(Optional.empty());

        ClassroomNotFoundException exception = assertThrows(
                ClassroomNotFoundException.class,
                () -> classroomService.patchClassroom(classroomId, request, requesterId)
        );

        assertThat(exception.getMessage()).contains(classroomId.toString());
    }

    @Test
    void deleteClassroom_removesExistingClassroom() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        Classroom classroom = makeClassroom(organization, "Class A", null);

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroom.getId(), organization.getId()))
                .thenReturn(Optional.of(classroom));

        classroomService.deleteClassroom(classroom.getId(), requester.getId());

        verify(classroomRepository).delete(classroom);
    }

    @Test
    void deleteClassroom_throwsWhenClassroomIsMissing() {
        User requester = makeUserWithOrganization();
        Organization organization = requester.getOrganization();
        UUID classroomId = UUID.randomUUID();
        UUID requesterId = requester.getId();

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, organization.getId()))
                .thenReturn(Optional.empty());

        ClassroomNotFoundException exception = assertThrows(
                ClassroomNotFoundException.class,
                () -> classroomService.deleteClassroom(classroomId, requesterId)
        );

        assertThat(exception.getMessage()).contains(classroomId.toString());
    }

    private User makeUserWithOrganization() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("org-admin@example.com");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));
        user.setStatus(UserStatus.ACTIVE);

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        user.setOrganization(organization);

        return user;
    }

    private User makeUserWithoutOrganization() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("org-admin@example.com");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Classroom makeClassroom(Organization organization, String name, String description) {
        Classroom classroom = new Classroom();
        classroom.setId(UUID.randomUUID());
        classroom.setOrganization(organization);
        classroom.setName(name);
        classroom.setDescription(description);
        classroom.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
        classroom.setUpdatedAt(LocalDateTime.of(2026, 4, 24, 11, 0));
        return classroom;
    }

    @Test
    void modifyClassroomStudents_shouldAddStudents_whenAllAreValid() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student1 = buildUser(student1Id, orgId, RoleName.STUDENT);
        User student2 = buildUser(student2Id, orgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(student1Id, student2Id));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student1, student2));
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, student1Id, MembershipType.STUDENT)).thenReturn(false);
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, student2Id, MembershipType.STUDENT)).thenReturn(false);

        classroomService.addClassroomMembers(classroomId, request, requesterId);

        ArgumentCaptor<ClassroomMembership> captor = ArgumentCaptor.forClass(ClassroomMembership.class);
        verify(classroomMembershipRepository, times(2)).save(captor.capture());

        List<ClassroomMembership> savedMemberships = captor.getAllValues();

        ClassroomMembership first = savedMemberships.get(0);
        ClassroomMembership second = savedMemberships.get(1);

        org.junit.jupiter.api.Assertions.assertEquals(MembershipType.STUDENT, first.getMembershipType());
        org.junit.jupiter.api.Assertions.assertEquals(MembershipType.STUDENT, second.getMembershipType());

        verify(classroomMembershipRepository, never())
                .deleteByClassroomIdAndUserIdAndMembershipType(any(), any(), any());
    }

    @Test
    void modifyClassroomStudents_shouldNotCreateDuplicateMemberships() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student = buildUser(studentId, orgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student));
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, studentId, MembershipType.STUDENT)).thenReturn(true);

        classroomService.addClassroomMembers(classroomId, request, requesterId);

        verify(classroomMembershipRepository, never()).save(any(ClassroomMembership.class));
    }

    @Test
    void modifyClassroomStudents_shouldThrow_whenOneOrMoreUsersDoNotExist() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student1 = buildUser(student1Id, orgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(student1Id, student2Id));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student1));

        assertThrows(UserNotFoundException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never()).save(any());
    }

    @Test
    void addClassroomMembers_shouldThrow_whenUserHasUnsupportedRole() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User parent = buildUser(parentId, orgId, RoleName.PARENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(parentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(parent));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never()).save(any());
    }

    @Test
    void modifyClassroomStudents_shouldThrow_whenUserIsFromAnotherOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID classroomOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, classroomOrgId);
        User student = buildUser(studentId, otherOrgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never()).save(any());
    }

    @Test
    void deleteClassroomStudents_shouldDeleteMemberships_whenTheyExist() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student1 = buildUser(student1Id, orgId, RoleName.STUDENT);
        User student2 = buildUser(student2Id, orgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(student1Id, student2Id));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student1, student2));
        classroomService.deleteClassroomMembers(classroomId, request, requesterId);

        verify(classroomMembershipRepository).deleteByClassroomIdAndUserIdAndMembershipType(
                classroomId, student1Id, MembershipType.STUDENT);
        verify(classroomMembershipRepository).deleteByClassroomIdAndUserIdAndMembershipType(
                classroomId, student2Id, MembershipType.STUDENT);
        verify(classroomMembershipRepository, never()).save(any(ClassroomMembership.class));
    }

    @Test
    void deleteClassroomStudents_shouldAttemptDelete_whenMembershipDoesNotExist() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student = buildUser(studentId, orgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student));
        assertDoesNotThrow(() -> classroomService.deleteClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository)
                .deleteByClassroomIdAndUserIdAndMembershipType(classroomId, studentId, MembershipType.STUDENT);
    }

    @Test
    void deleteClassroomStudents_shouldThrow_whenUserIsNotStudent() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User teacher = buildUser(teacherId, orgId, RoleName.PARENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(teacherId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(teacher));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.deleteClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never())
                .deleteByClassroomIdAndUserIdAndMembershipType(any(), any(), any());
    }

    @Test
    void deleteClassroomStudents_shouldThrow_whenUserIsFromAnotherOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID classroomOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, classroomOrgId);
        User student = buildUser(studentId, otherOrgId, RoleName.STUDENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.deleteClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never())
                .deleteByClassroomIdAndUserIdAndMembershipType(any(), any(), any());
    }

    @Test
    void modifyClassroomStudents_shouldThrow_whenClassroomDoesNotExist() {
        UUID classroomId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertThrows(ClassroomNotFoundException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));
    }

    @Test
    void addClassroomMembers_shouldAddStudentAndTeacher_whenAllAreValid() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student = buildUser(UUID.randomUUID(), orgId, RoleName.STUDENT);
        User teacher = buildUser(UUID.randomUUID(), orgId, RoleName.TEACHER);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(student.getId(), teacher.getId()));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student, teacher));
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, student.getId(), MembershipType.STUDENT)).thenReturn(false);
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, teacher.getId(), MembershipType.TEACHER)).thenReturn(false);

        classroomService.addClassroomMembers(classroomId, request, requesterId);

        ArgumentCaptor<ClassroomMembership> captor = ArgumentCaptor.forClass(ClassroomMembership.class);
        verify(classroomMembershipRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(ClassroomMembership::getMembershipType)
                .containsExactlyInAnyOrder(MembershipType.STUDENT, MembershipType.TEACHER);
    }

    @Test
    void addClassroomMembers_shouldThrow_whenMemberHasUnsupportedRole() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User parent = buildUser(UUID.randomUUID(), orgId, RoleName.PARENT);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(parentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(parent));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never()).save(any());
    }

    @Test
    void deleteClassroomMembers_shouldDeleteStudentAndTeacherMemberships() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student = buildUser(UUID.randomUUID(), orgId, RoleName.STUDENT);
        User teacher = buildUser(UUID.randomUUID(), orgId, RoleName.TEACHER);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(student.getId(), teacher.getId()));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student, teacher));

        classroomService.deleteClassroomMembers(classroomId, request, requesterId);

        verify(classroomMembershipRepository).deleteByClassroomIdAndUserIdAndMembershipType(
                classroomId, student.getId(), MembershipType.STUDENT);
        verify(classroomMembershipRepository).deleteByClassroomIdAndUserIdAndMembershipType(
                classroomId, teacher.getId(), MembershipType.TEACHER);
    }

    @Test
    void listClassroomMembers_shouldReturnAllMembers_whenRoleIsNull() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership studentMembership =
                buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, RoleName.STUDENT), MembershipType.STUDENT);
        ClassroomMembership teacherMembership =
                buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, RoleName.TEACHER), MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomId(classroomId))
                .thenReturn(List.of(studentMembership, teacherMembership));

        List<ClassroomMemberResponse> result = classroomService.listClassroomMembers(classroomId, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClassroomMemberResponse::getMembershipType)
                .containsExactlyInAnyOrder(MembershipType.STUDENT, MembershipType.TEACHER);
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyStudents_whenRoleIsStudent() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership membership =
                buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, RoleName.STUDENT), MembershipType.STUDENT);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(classroomId, MembershipType.STUDENT))
                .thenReturn(List.of(membership));

        List<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.STUDENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMembershipType()).isEqualTo(MembershipType.STUDENT);
    }

    @Test
    void listClassroomMembers_shouldReturnOnlyTeachers_whenRoleIsTeacher() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        ClassroomMembership membership =
                buildMembership(classroom, buildUser(UUID.randomUUID(), orgId, RoleName.TEACHER), MembershipType.TEACHER);

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(classroomId, MembershipType.TEACHER))
                .thenReturn(List.of(membership));

        List<ClassroomMemberResponse> result =
                classroomService.listClassroomMembers(classroomId, MembershipType.TEACHER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMembershipType()).isEqualTo(MembershipType.TEACHER);
    }

    @Test
    void addClassroomMembers_shouldThrow_whenUserHasNoOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);
        User student = buildUser(studentId, orgId, RoleName.STUDENT);
        student.setOrganization(null);

        ModifyClassroomMembersRequest request =
                new ModifyClassroomMembersRequest(Set.of(studentId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(userRepository.findAllById(request.getMemberIds())).thenReturn(List.of(student));

        assertThrows(ClassroomBadRequestException.class,
                () -> classroomService.addClassroomMembers(classroomId, request, requesterId));

        verify(classroomMembershipRepository, never()).save(any());
    }

    @Test
    void handleStudentAddedToClassroom_shouldThrowUnsupportedOperationException() throws Exception {
        Method method = ClassroomService.class.getDeclaredMethod(
                "handleStudentAddedToClassroom",
                Classroom.class,
                User.class
        );
        method.setAccessible(true);

        Classroom classroom = buildClassroom(UUID.randomUUID(), UUID.randomUUID());
        User student = buildUser(UUID.randomUUID(), classroom.getOrganization().getId(), RoleName.STUDENT);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(classroomService, classroom, student)
        );

        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(exception.getCause().getMessage()).contains(classroom.getId().toString(), student.getId().toString());
    }

    @Test
    void resolveMembershipType_shouldThrowForUnsupportedRole() throws Exception {
        Method method = ClassroomService.class.getDeclaredMethod("resolveMembershipType", User.class);
        method.setAccessible(true);

        User parent = buildUser(UUID.randomUUID(), UUID.randomUUID(), RoleName.PARENT);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(classroomService, parent)
        );

        assertThat(exception.getCause()).isInstanceOf(ClassroomBadRequestException.class);
        assertThat(exception.getCause().getMessage()).contains(parent.getId().toString());
    }

    private Classroom buildClassroom(UUID classroomId, UUID orgId) {
        Organization organization = new Organization();
        organization.setId(orgId);

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setOrganization(organization);

        return classroom;
    }

    private User buildUser(UUID userId, UUID orgId, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        Organization organization = new Organization();
        organization.setId(orgId);

        User user = new User();
        user.setId(userId);
        user.setRole(role);
        user.setOrganization(organization);

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
