package org.elearning.backend.enrollment;

import org.elearning.backend.auth.service.TokenBlacklistService;
import org.elearning.backend.enrollment.controller.CertificateGeneratorController;
import org.elearning.backend.enrollment.exception.*;
import org.elearning.backend.enrollment.service.CertificateGeneratorService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CertificateGeneratorController.class)
class CertificateGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificateGeneratorService certificateGeneratorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private UUID enrollmentId;
    private UUID studentId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        enrollmentId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getName()).thenReturn(RoleName.STUDENT);

        User user = mock(User.class);
        when(user.getId()).thenReturn(studentId);
        when(user.getEmail()).thenReturn("student@test.com");
        when(user.getPasswordHash()).thenReturn("hashed");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getRole()).thenReturn(role);

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generatePdfSuccess() throws Exception {
        byte[] fakePdf = new byte[]{1, 2, 3, 4};
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"certificate.pdf\""))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    void generatePdfEnrollmentNotFoundReturns404() throws Exception {
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenThrow(new CourseEnrollmentNotFoundException(enrollmentId));

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void generatePdfWrongStudentReturns403() throws Exception {
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenThrow(new StudentAccessForbiddenException(studentId));

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatePdfCourseNotCompletedReturns403() throws Exception {
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenThrow(new CourseHasNotBeenFinalizedException(enrollmentId));

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatePdfPrivateCourseReturns403() throws Exception {
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenThrow(new CourseMustBePublicException(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatePdfGenerationFailsReturns500() throws Exception {
        when(certificateGeneratorService.generateCertificatePdf(enrollmentId, studentId))
                .thenThrow(new CertificateGenerationException(enrollmentId, new RuntimeException("iText error")));

        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId)
                        .with(user(userDetails)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generatePdfUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/{id}/certificat", enrollmentId))
                .andExpect(status().isUnauthorized());
    }
}
