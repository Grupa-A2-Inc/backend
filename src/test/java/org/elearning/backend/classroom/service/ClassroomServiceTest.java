package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomConflictException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

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
}
