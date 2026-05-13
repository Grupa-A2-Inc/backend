package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.TokenBlackListService;
import org.elearning.backend.feedback.controller.ProfessorRatingController;
import org.elearning.backend.feedback.dto.LessonRatingFullStatsDto;
import org.elearning.backend.feedback.service.ProfessorRatingService;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessorRatingController.class)
@ActiveProfiles("test")
class ProfessorRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorRatingService professorRatingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenBlackListService tokenBlackListService;

    private UUID teacherId;
    private CustomUserDetails teacherDetails;
    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        teacherDetails = createUserDetails(teacherId, RoleName.TEACHER);
    }

    @Test
    void getAverageRatingsForAllLessonsReturnsRatingsForTeacher() throws Exception {
        UUID lessonId = UUID.randomUUID();
        when(professorRatingService.getAverageRatingsForAllLessons(teacherId))
                .thenReturn(List.of(new LessonRatingFullStatsDto(
                        lessonId,
                        "Lesson title",
                        4.75,
                        8L
                )));

        mockMvc.perform(get("/api/v1/professors/me/lessons/ratings")
                        .with(user(teacherDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].title").value("Lesson title"))
                .andExpect(jsonPath("$[0].averageRating").value(4.75))
                .andExpect(jsonPath("$[0].totalRatings").value(8));

        verify(professorRatingService).getAverageRatingsForAllLessons(teacherId);
    }

    @Test
    void getAverageRatingsForAllLessonsReturnsUnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/professors/me/lessons/ratings"))
                .andExpect(status().isUnauthorized());
    }

    private CustomUserDetails createUserDetails(UUID userId, RoleName roleName) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(roleName);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(roleName.name().toLowerCase() + "@test.com");
        when(user.getPasswordHash()).thenReturn("hashed");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getRole()).thenReturn(role);

        return new CustomUserDetails(user);
    }
}
