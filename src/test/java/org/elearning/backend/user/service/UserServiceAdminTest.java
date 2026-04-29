package org.elearning.backend.user.service;

import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.exception.UserOrganizationNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserServiceAdminTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        Organization organization = new Organization();
        UUID organizationId = UUID.randomUUID();
        organization.setId(organizationId);

        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("user1@test.com");
        user1.setFirstName("User");
        user1.setLastName("One");
        user1.setRole(role);
        user1.setOrganization(organization);
        user1.setStatus(UserStatus.ACTIVE);

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("user2@test.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setRole(role);
        user2.setOrganization(organization);
        user2.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("user1@test.com", result.get(0).getEmail());
        assertEquals("user2@test.com", result.get(1).getEmail());

        verify(userRepository).findAll();
    }

    @Test
    void getCurrentOrganizationUsers_shouldReturnUsersFromCurrentOrganization() {
        UUID currentUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(organizationId);

        Role orgAdminRole = new Role();
        orgAdminRole.setName(RoleName.ORGANIZATION_ADMIN);

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("admin@test.com");
        currentUser.setFirstName("Admin");
        currentUser.setLastName("Org");
        currentUser.setRole(orgAdminRole);
        currentUser.setOrganization(organization);
        currentUser.setStatus(UserStatus.ACTIVE);

        User orgUser1 = new User();
        orgUser1.setId(UUID.randomUUID());
        orgUser1.setEmail("teacher@test.com");
        orgUser1.setFirstName("Teach");
        orgUser1.setLastName("Er");
        orgUser1.setRole(orgAdminRole);
        orgUser1.setOrganization(organization);
        orgUser1.setStatus(UserStatus.ACTIVE);

        User orgUser2 = new User();
        orgUser2.setId(UUID.randomUUID());
        orgUser2.setEmail("student@test.com");
        orgUser2.setFirstName("Stu");
        orgUser2.setLastName("Dent");
        orgUser2.setRole(orgAdminRole);
        orgUser2.setOrganization(organization);
        orgUser2.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userDetails.getUserId()).thenReturn(currentUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(orgUser1, orgUser2));

        List<UserResponse> result = userService.getCurrentOrganizationUsers();

        assertEquals(2, result.size());
        assertEquals("teacher@test.com", result.get(0).getEmail());
        assertEquals("student@test.com", result.get(1).getEmail());

        verify(userRepository).findById(currentUserId);
        verify(userRepository).findByOrganizationId(organizationId);
    }

    @Test
    void getCurrentOrganizationUsers_shouldThrowWhenCurrentUserNotFound() {
        UUID currentUserId = UUID.randomUUID();

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userDetails.getUserId()).thenReturn(currentUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getCurrentOrganizationUsers());

        verify(userRepository).findById(currentUserId);
        verify(userRepository, never()).findByOrganizationId(any());
    }

    @Test
    void getCurrentOrganizationUsers_shouldThrowWhenOrganizationIsNull() {
        UUID currentUserId = UUID.randomUUID();

        Role orgAdminRole = new Role();
        orgAdminRole.setName(RoleName.ORGANIZATION_ADMIN);

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("admin@test.com");
        currentUser.setFirstName("Admin");
        currentUser.setLastName("Org");
        currentUser.setRole(orgAdminRole);
        currentUser.setOrganization(null);
        currentUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userDetails.getUserId()).thenReturn(currentUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(UserOrganizationNotFoundException.class, () -> userService.getCurrentOrganizationUsers());

        verify(userRepository).findById(currentUserId);
        verify(userRepository, never()).findByOrganizationId(any());
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenRepositoryHasNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findAll();
        verify(userRepository, never()).findByOrganizationId(any());
    }
    @Test
    void getCurrentOrganizationUsers_shouldReturnEmptyList_whenOrganizationHasNoUsers() {
        UUID currentUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(organizationId);

        Role orgAdminRole = new Role();
        orgAdminRole.setName(RoleName.ORGANIZATION_ADMIN);

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("admin@test.com");
        currentUser.setFirstName("Admin");
        currentUser.setLastName("Org");
        currentUser.setRole(orgAdminRole);
        currentUser.setOrganization(organization);
        currentUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userDetails.getUserId()).thenReturn(currentUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

        List<UserResponse> result = userService.getCurrentOrganizationUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findById(currentUserId);
        verify(userRepository).findByOrganizationId(organizationId);
        verify(userRepository, never()).findAll();
    }
    @Test
    void getCurrentOrganizationUsers_shouldUseAuthenticatedUsersOrganizationId() {
        UUID currentUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(organizationId);

        Role orgAdminRole = new Role();
        orgAdminRole.setName(RoleName.ORGANIZATION_ADMIN);

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("admin@test.com");
        currentUser.setFirstName("Admin");
        currentUser.setLastName("Org");
        currentUser.setRole(orgAdminRole);
        currentUser.setOrganization(organization);
        currentUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userDetails.getUserId()).thenReturn(currentUserId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

        userService.getCurrentOrganizationUsers();

        verify(userRepository).findByOrganizationId(organizationId);
        verify(userRepository, never()).findAll();
    }

    @Test
    void getAllUsers_shouldNotUseSecurityContextAndShouldStillReturnUsers() {
        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("admin@test.com", result.get(0).getEmail());

        verify(userRepository).findAll();
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).findByOrganizationId(any());
    }
}
