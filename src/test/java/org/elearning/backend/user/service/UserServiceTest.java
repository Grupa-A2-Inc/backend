package org.elearning.backend.user.service;

import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.*;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.elearning.backend.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    void createUser_success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .password("parola123")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        Role role = new Role(RoleName.TEACHER);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("ion@scoala.ro");
        savedUser.setFirstName("Ion");
        savedUser.setLastName("Pop");
        savedUser.setRole(role);
        savedUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertEquals("ion@scoala.ro", response.getEmail());
        assertEquals("Ion", response.getFirstName());
        assertEquals(RoleName.TEACHER, response.getRoleName());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    @Test
    void createUser_duplicateEmail_throwsException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .build();

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.createUser(request));
    }

    @Test
    void getUserById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(id));
    }

    @Test
    void getAllUsers_returnsListOfUsers() {
        Role role = new Role(RoleName.TEACHER);

        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("ion@scoala.ro");
        user1.setFirstName("Ion");
        user1.setLastName("Pop");
        user1.setRole(role);
        user1.setStatus(UserStatus.ACTIVE);

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("ana@scoala.ro");
        user2.setFirstName("Ana");
        user2.setLastName("Pop");
        user2.setRole(role);
        user2.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());
        assertEquals("ion@scoala.ro", responses.get(0).getEmail());
        assertEquals("ana@scoala.ro", responses.get(1).getEmail());
    }

    @Test
    void updateUser_success() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("ion@scoala.ro");
        existingUser.setFirstName("Ion");
        existingUser.setLastName("Pop");
        existingUser.setRole(role);
        existingUser.setStatus(UserStatus.ACTIVE);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("ion.nou@scoala.ro")
                .firstName("Ion")
                .lastName("Popescu")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        var originalUpdatedAt = existingUser.getUpdatedAt();
        UserResponse response = userService.updateUser(id, request);

        assertEquals("ion.nou@scoala.ro", response.getEmail());
        assertEquals("Popescu", response.getLastName());
        assertTrue(!existingUser.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void deleteUser_success() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteUser_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser(id));
    }

    @Test
    void getUserById_success() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User user = new User();
        user.setId(id);
        user.setEmail("ion@scoala.ro");
        user.setFirstName("Ion");
        user.setLastName("Pop");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(id);

        assertEquals("ion@scoala.ro", response.getEmail());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    @Test
    void updateUser_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(id, request));
    }

    @Test
    void createUser_roleNotFound_throwsException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .password("parola123")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .build();

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.empty());

        assertThrows(UserRoleNotFoundException.class,
                () -> userService.createUser(request));
    }

    @Test
    void createUser_hashesPassword_andSavesUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ana@example.com")
                .password("parola123")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        Role role = new Role();
        role.setName(RoleName.STUDENT);

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("parola123")).thenReturn("HASHED_PASSWORD");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("ana@example.com");
        savedUser.setPasswordHash("HASHED_PASSWORD");
        savedUser.setFirstName("Ana");
        savedUser.setLastName("Pop");
        savedUser.setRole(role);
        savedUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User persistedUser = captor.getValue();

        assertEquals("HASHED_PASSWORD", persistedUser.getPasswordHash());
        assertNotEquals("parola123", persistedUser.getPasswordHash());
        verify(passwordEncoder).encode("parola123");

        assertEquals("ana@example.com", response.getEmail());
        assertEquals("Ana", response.getFirstName());
        assertEquals("Pop", response.getLastName());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    @Test
    void createUser_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ana@example.com")
                .password("parola123")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void createUser_throwsResourceNotFoundException_whenRoleDoesNotExist() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ana@example.com")
                .password("parola123")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .build();

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.empty());

        assertThrows(UserRoleNotFoundException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_withOrganization_success() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Scoala Nr. 1");

        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .password("parola123")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .organizationId(org.getId())
                .build();

        Role role = new Role(RoleName.TEACHER);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("ion@scoala.ro");
        savedUser.setFirstName("Ion");
        savedUser.setLastName("Pop");
        savedUser.setRole(role);
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setOrganization(org);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(role));
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertEquals("ion@scoala.ro", response.getEmail());
        assertEquals(org.getId(), response.getOrganizationId());
    }

    @Test
    void createUser_organizationNotFound_throwsException() {
        UUID orgId = UUID.randomUUID();

        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro")
                .password("parola123")
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.TEACHER)
                .organizationId(orgId)
                .build();

        Role role = new Role(RoleName.TEACHER);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(role));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(UserOrganizationNotFoundException.class,
                () -> userService.createUser(request));
    }

    @Test
    void updateUser_withOrganization_success() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Scoala Nr. 1");

        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("ion@scoala.ro");
        existingUser.setFirstName("Ion");
        existingUser.setLastName("Pop");
        existingUser.setRole(role);
        existingUser.setStatus(UserStatus.ACTIVE);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .organizationId(org.getId())
                .build();

        existingUser.setOrganization(org);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserResponse response = userService.updateUser(id, request);

        assertEquals(org.getId(), response.getOrganizationId());
    }

    @Test
    void updateUser_organizationNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("ion@scoala.ro");
        existingUser.setFirstName("Ion");
        existingUser.setLastName("Pop");
        existingUser.setRole(role);
        existingUser.setStatus(UserStatus.ACTIVE);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("ion@scoala.ro")
                .firstName("Ion")
                .lastName("Pop")
                .organizationId(orgId)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(UserOrganizationNotFoundException.class,
                () -> userService.updateUser(id, request));
    }

    @Test
    void getUsersByOrganizationId_returnsMappedUsers() {
        UUID organizationId = UUID.randomUUID();
        Role role = new Role(RoleName.STUDENT);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        user.setFirstName("Student");
        user.setLastName("One");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        Organization organization = new Organization();
        organization.setId(organizationId);
        user.setOrganization(organization);

        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(user));

        List<UserResponse> responses = userService.getUsersByOrganizationId(organizationId);

        assertEquals(1, responses.size());
        assertEquals(user.getId(), responses.get(0).getId());
        assertEquals(organizationId, responses.get(0).getOrganizationId());
        assertEquals(RoleName.STUDENT, responses.get(0).getRoleName());
    }

    @Test
    void changePassword_success() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User user = new User();
        user.setId(id);
        user.setEmail("ion@scoala.ro");
        user.setFirstName("Ion");
        user.setLastName("Pop");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed_old_password");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashed_old_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("hashed_new_password");

        userService.changePassword(id, request);

        verify(userRepository).save(user);
        assertEquals("hashed_new_password", user.getPasswordHash());
    }

    @Test
    void changePassword_userNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.changePassword(id, request));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User user = new User();
        user.setId(id);
        user.setEmail("ion@scoala.ro");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed_old_password");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed_old_password")).thenReturn(false);

        assertThrows(UserBadRequestException.class,
                () -> userService.changePassword(id, request));
    }

    @Test
    void changePassword_passwordsDoNotMatch_throwsException() {
        UUID id = UUID.randomUUID();
        Role role = new Role(RoleName.TEACHER);

        User user = new User();
        user.setId(id);
        user.setEmail("ion@scoala.ro");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed_old_password");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("differentPassword")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashed_old_password")).thenReturn(true);

        assertThrows(UserBadRequestException.class,
                () -> userService.changePassword(id, request));
    }

    @Test
    void importUsers_allSuccess_returnsFullSuccessReport() {
        Role role = new Role(RoleName.TEACHER);

        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("ion@scoala.ro").password("parola123")
                .firstName("Ion").lastName("Pop").roleName(RoleName.TEACHER).build();

        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("ana@scoala.ro").password("parola123")
                .firstName("Ana").lastName("Pop").roleName(RoleName.TEACHER).build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(req1, req2));

        User savedUser1 = new User();
        savedUser1.setId(UUID.randomUUID());
        savedUser1.setEmail("ion@scoala.ro");
        savedUser1.setFirstName("Ion");
        savedUser1.setLastName("Pop");
        savedUser1.setRole(role);
        savedUser1.setStatus(UserStatus.ACTIVE);

        User savedUser2 = new User();
        savedUser2.setId(UUID.randomUUID());
        savedUser2.setEmail("ana@scoala.ro");
        savedUser2.setFirstName("Ana");
        savedUser2.setLastName("Pop");
        savedUser2.setRole(role);
        savedUser2.setStatus(UserStatus.ACTIVE);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(userRepository.existsByEmail("ana@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser1)
                .thenReturn(savedUser2);

        BulkImportResponse response = userService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(2, response.getSucceeded());
        assertEquals(0, response.getFailed());
        assertTrue(response.getResults().stream().allMatch(UserImportResult::isSuccess));
    }

    @Test
    void importUsers_partialSuccess_returnsCorrectReport() {
        Role role = new Role(RoleName.TEACHER);

        CreateUserRequest validReq = CreateUserRequest.builder()
                .email("ion@scoala.ro").password("parola123")
                .firstName("Ion").lastName("Pop").roleName(RoleName.TEACHER).build();

        CreateUserRequest duplicateReq = CreateUserRequest.builder()
                .email("duplicat@scoala.ro").password("parola123")
                .firstName("Ana").lastName("Pop").roleName(RoleName.TEACHER).build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(validReq, duplicateReq));

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("ion@scoala.ro");
        savedUser.setFirstName("Ion");
        savedUser.setLastName("Pop");
        savedUser.setRole(role);
        savedUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(userRepository.existsByEmail("duplicat@scoala.ro")).thenReturn(true);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        BulkImportResponse response = userService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(1, response.getSucceeded());
        assertEquals(1, response.getFailed());

        UserImportResult failedResult = response.getResults().stream()
                .filter(r -> !r.isSuccess()).findFirst().orElseThrow();
        assertEquals("duplicat@scoala.ro", failedResult.getEmail());
        assertNotNull(failedResult.getErrorMessage());
    }

    @Test
    void importUsers_allFailed_returnsFullFailureReport() {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("ion@scoala.ro").build();
        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("ana@scoala.ro").build();

        CreateUserBulkRequest bulkRequest = new CreateUserBulkRequest(List.of(req1, req2));

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(true);
        when(userRepository.existsByEmail("ana@scoala.ro")).thenReturn(true);

        BulkImportResponse response = userService.importUsers(bulkRequest);

        assertEquals(2, response.getTotal());
        assertEquals(0, response.getSucceeded());
        assertEquals(2, response.getFailed());
        assertTrue(response.getResults().stream().noneMatch(UserImportResult::isSuccess));
    }

    @Test
    void tryCreateSingleUser_success_returnsSucceededResult() {
        Role role = new Role(RoleName.STUDENT);

        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro").password("parola123")
                .firstName("Ion").lastName("Pop").roleName(RoleName.STUDENT).build();

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("ion@scoala.ro");
        savedUser.setFirstName("Ion");
        savedUser.setLastName("Pop");
        savedUser.setRole(role);
        savedUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserImportResult result = userService.tryCreateSingleUser(request);

        assertTrue(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertNotNull(result.getUser());
        assertNull(result.getErrorMessage());
    }

    @Test
    void tryCreateSingleUser_duplicateEmail_returnsFailedResult() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ion@scoala.ro").build();

        when(userRepository.existsByEmail("ion@scoala.ro")).thenReturn(true);

        UserImportResult result = userService.tryCreateSingleUser(request);

        assertFalse(result.isSuccess());
        assertEquals("ion@scoala.ro", result.getEmail());
        assertNull(result.getUser());
        assertNotNull(result.getErrorMessage());
    }
}
