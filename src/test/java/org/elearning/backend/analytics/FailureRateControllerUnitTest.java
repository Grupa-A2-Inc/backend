package org.elearning.backend.analytics;

import org.elearning.backend.analytics.controller.FailureRateController;
import org.elearning.backend.analytics.dto.statistics.teacher.TestFailureRateChartDTO;
import org.elearning.backend.analytics.service.FailureRateService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailureRateControllerUnitTest {

    @Mock
    private FailureRateService failureRateService;

    private FailureRateController controller;

    @BeforeEach
    void setUp() {
        controller = new FailureRateController(failureRateService);
    }

    @Test
    void getFailureRateChartDataUsesCustomUserDetailsUserId() {
        UUID courseId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        CustomUserDetails user = customUser(professorId);
        List<TestFailureRateChartDTO> body = List.of(new TestFailureRateChartDTO(List.of()));
        when(failureRateService.getFailureCharts(courseId, professorId)).thenReturn(body);

        ResponseEntity<List<TestFailureRateChartDTO>> response = controller.getFailureRateChartData(courseId, user);

        assertThat(response.getBody()).isSameAs(body);
        verify(failureRateService).getFailureCharts(courseId, professorId);
    }

    @Test
    void getFailureRateChartDataFallsBackToUsernameUuid() {
        UUID courseId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UserDetails user = org.springframework.security.core.userdetails.User.withUsername(professorId.toString())
                .password("ignored")
                .roles("TEACHER")
                .build();
        when(failureRateService.getFailureCharts(courseId, professorId)).thenReturn(List.of());

        ResponseEntity<List<TestFailureRateChartDTO>> response = controller.getFailureRateChartData(courseId, user);

        assertThat(response.getBody()).isEmpty();
        verify(failureRateService).getFailureCharts(courseId, professorId);
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
