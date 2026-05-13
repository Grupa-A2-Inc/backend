package org.elearning.backend.assessment;

import org.elearning.backend.assessment.controller.TestResultController;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.service.TestResultService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestResultControllerUnitTest {

    @Mock
    private TestResultService testResultService;

    private TestResultController controller;

    @BeforeEach
    void setUp() {
        controller = new TestResultController(testResultService);
    }

    @Test
    void getBestAttemptUsesCustomUserDetailsUserId() {
        UUID testId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        CustomUserDetails currentUser = customUser(studentId);
        AttemptStatusDTO dto = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .attemptNumber(1)
                .score(BigDecimal.TEN)
                .scorePercent(BigDecimal.TEN)
                .passed(true)
                .status(AttemptStatus.DONE)
                .build();
        when(testResultService.getBestTestAttempt(testId, studentId)).thenReturn(dto);

        ResponseEntity<AttemptStatusDTO> response = controller.getBestAttempt(testId, currentUser);

        assertThat(response.getBody()).isSameAs(dto);
        verify(testResultService).getBestTestAttempt(testId, studentId);
    }

    @Test
    void getBestAttemptFallsBackToUsernameWhenPrincipalIsPlainUserDetails() {
        UUID testId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UserDetails currentUser = org.springframework.security.core.userdetails.User.withUsername(studentId.toString())
                .password("ignored")
                .roles("STUDENT")
                .build();
        AttemptStatusDTO dto = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .attemptNumber(2)
                .score(BigDecimal.ONE)
                .scorePercent(BigDecimal.ONE)
                .passed(false)
                .status(AttemptStatus.DONE)
                .build();
        when(testResultService.getBestTestAttempt(testId, studentId)).thenReturn(dto);

        ResponseEntity<AttemptStatusDTO> response = controller.getBestAttempt(testId, currentUser);

        assertThat(response.getBody()).isSameAs(dto);
        verify(testResultService).getBestTestAttempt(testId, studentId);
    }

    private CustomUserDetails customUser(UUID studentId) {
        User user = new User();
        user.setId(studentId);
        user.setEmail("student@test.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);

        Role role = new Role();
        role.setName(RoleName.STUDENT);
        user.setRole(role);

        return new CustomUserDetails(user);
    }
}
