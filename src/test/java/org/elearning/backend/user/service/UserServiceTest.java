package org.elearning.backend.user.service;

import org.elearning.backend.common.exception.DuplicateResourceException;
import org.elearning.backend.common.exception.ResourceNotFoundException;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

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

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(request));
    }

    @Test
    void getUserById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
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

        UserResponse response = userService.updateUser(id, request);

        assertEquals("ion.nou@scoala.ro", response.getEmail());
        assertEquals("Popescu", response.getLastName());
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

        assertThrows(ResourceNotFoundException.class,
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

        assertThrows(ResourceNotFoundException.class,
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

        assertThrows(ResourceNotFoundException.class,
                () -> userService.createUser(request));
    }
}