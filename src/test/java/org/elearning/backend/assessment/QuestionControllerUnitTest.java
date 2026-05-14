package org.elearning.backend.assessment;

import org.elearning.backend.assessment.controller.QuestionController;
import org.elearning.backend.assessment.dto.question_dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionResponseDto;
import org.elearning.backend.assessment.service.QuestionService;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionControllerUnitTest {

    @Mock
    private QuestionService questionService;

    private QuestionController controller;

    @BeforeEach
    void setUp() {
        controller = new QuestionController(questionService);
    }

    @Test
    void deleteQuestionUsesCustomUserDetailsUserId() {
        UUID testId = UUID.randomUUID();
        Integer questionId = 4;
        UUID professorId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteQuestion(testId, questionId, customUser(professorId));

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(questionService).deleteQuestion(testId, questionId, professorId);
    }

    @Test
    void createQuestionFallsBackToUsernameUuid() {
        UUID testId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UserDetails user = org.springframework.security.core.userdetails.User.withUsername(professorId.toString())
                .password("ignored")
                .roles("PROFESSOR")
                .build();
        QuestionRequestDto request = new QuestionRequestDto();
        QuestionResponseDto dto = QuestionResponseDto.builder().build();
        when(questionService.createQuestion(testId, request, professorId)).thenReturn(dto);

        ResponseEntity<QuestionResponseDto> response = controller.createQuestion(testId, request, user);

        assertThat(response.getBody()).isSameAs(dto);
        verify(questionService).createQuestion(testId, request, professorId);
    }

    private CustomUserDetails customUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("teacher@test.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(RoleName.TEACHER);
        user.setRole(role);
        return new CustomUserDetails(user);
    }
}
