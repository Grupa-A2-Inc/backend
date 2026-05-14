package org.elearning.backend.feedback;

import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.feedback.service.QuestionAccessValidatorService;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.access.AccessService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAccessValidatorServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessService accessService;

    @Mock
    private Authentication authentication;

    private QuestionAccessValidatorService service;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        service = new QuestionAccessValidatorService(questionRepository, userRepository, accessService);
        studentId = UUID.randomUUID();
    }

    @Test
    void returnsFalseWhenCurrentUserIsMissing() {
        when(accessService.extractCurrentUser(authentication)).thenReturn(null);

        boolean result = service.hasStudentAccessToQuestion(authentication, 10);

        assertThat(result).isFalse();
        verifyNoInteractions(questionRepository, userRepository);
    }

    @Test
    void returnsFalseWhenCurrentUserIsNotStudent() {
        when(accessService.extractCurrentUser(authentication)).thenReturn(customUser(RoleName.TEACHER));

        boolean result = service.hasStudentAccessToQuestion(authentication, 10);

        assertThat(result).isFalse();
        verifyNoInteractions(questionRepository, userRepository);
    }

    @Test
    void returnsFalseWhenQuestionDoesNotExist() {
        when(accessService.extractCurrentUser(authentication)).thenReturn(customUser(RoleName.STUDENT));
        when(questionRepository.existsById(10)).thenReturn(false);

        boolean result = service.hasStudentAccessToQuestion(authentication, 10);

        assertThat(result).isFalse();
        verify(questionRepository).existsById(10);
        verifyNoInteractions(userRepository);
    }

    @Test
    void returnsFalseWhenStudentDoesNotExist() {
        when(accessService.extractCurrentUser(authentication)).thenReturn(customUser(RoleName.STUDENT));
        when(questionRepository.existsById(10)).thenReturn(true);
        when(userRepository.existsById(studentId)).thenReturn(false);

        boolean result = service.hasStudentAccessToQuestion(authentication, 10);

        assertThat(result).isFalse();
        verify(userRepository).existsById(studentId);
    }

    @Test
    void returnsRepositoryResultWhenStudentAndQuestionExist() {
        when(accessService.extractCurrentUser(authentication)).thenReturn(customUser(RoleName.STUDENT));
        when(questionRepository.existsById(10)).thenReturn(true);
        when(userRepository.existsById(studentId)).thenReturn(true);
        when(questionRepository.hasStudentAccessToQuestion(10, studentId)).thenReturn(true);

        boolean result = service.hasStudentAccessToQuestion(authentication, 10);

        assertThat(result).isTrue();
        verify(questionRepository).hasStudentAccessToQuestion(10, studentId);
    }

    private CustomUserDetails customUser(RoleName roleName) {
        User user = new User();
        user.setId(studentId);
        user.setEmail("student@test.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);

        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);

        return new CustomUserDetails(user);
    }
}
