package org.elearning.backend.enrollment;

import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.exception.CourseEnrollmentNotFoundException;
import org.elearning.backend.enrollment.exception.CourseHasNotBeenFinalizedException;
import org.elearning.backend.enrollment.exception.CourseMustBePublicException;
import org.elearning.backend.enrollment.exception.StudentAccessForbiddenException;
import org.elearning.backend.enrollment.exception.CertificateGenerationException;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.service.CertificateGeneratorService;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateGeneratorServiceTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;

    private CertificateGeneratorService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new CertificateGeneratorService(
                courseEnrollmentRepository, courseRepository, userRepository);
    }

    @Test
    void generateCertificatePdf_enrollmentNotFound_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThrows(CourseEnrollmentNotFoundException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_wrongStudent_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(UUID.randomUUID()); // alt student
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(StudentAccessForbiddenException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_courseNotCompleted_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(null);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(CourseHasNotBeenFinalizedException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_privateCourse_throwsException() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(LocalDateTime.now());
        when(enrollment.getCourseId()).thenReturn(courseId);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        Course course = mock(Course.class);
        when(course.getVisibility()).thenReturn(CourseVisibility.PRIVATE);
        when(course.getId()).thenReturn(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(CourseMustBePublicException.class,
                () -> service.generateCertificatePdf(enrollmentId, studentId));
    }

    @Test
    void generateCertificatePdf_success_returnsByteArray() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(studentId);
        when(enrollment.getCompletedAt()).thenReturn(LocalDateTime.now());
        when(enrollment.getCourseId()).thenReturn(courseId);
        when(courseEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        Course course = mock(Course.class);
        when(course.getVisibility()).thenReturn(CourseVisibility.PUBLIC);
        when(course.getTitle()).thenReturn("Spring Boot Masterclass");
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        User student = mock(User.class);
        when(student.getFirstName()).thenReturn("Ion");
        when(student.getLastName()).thenReturn("Popescu");
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        byte[] result = service.generateCertificatePdf(enrollmentId, studentId);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    void constructor_missingQuadrillionFont_throwsIllegalArgumentException() {
        TestableCertificateGeneratorService.resources = new HashMap<>();
        TestableCertificateGeneratorService.resources.put("/fonts/Quadrillion-Sb.otf", null);
        TestableCertificateGeneratorService.resources.put("/fonts/Jastyka.ttf", fontStream());

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TestableCertificateGeneratorService(courseEnrollmentRepository, courseRepository, userRepository));

        Assertions.assertTrue(exception.getMessage().contains("Quadrillion-Sb.otf"));
    }

    @Test
    void constructor_missingJastykaFont_throwsIllegalArgumentException() {
        TestableCertificateGeneratorService.resources = new HashMap<>();
        TestableCertificateGeneratorService.resources.put("/fonts/Quadrillion-Sb.otf", fontStream());
        TestableCertificateGeneratorService.resources.put("/fonts/Jastyka.ttf", null);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TestableCertificateGeneratorService(courseEnrollmentRepository, courseRepository, userRepository));

        Assertions.assertTrue(exception.getMessage().contains("Jastyka.ttf"));
    }

    @Test
    void loadFont_skipsRecreatingFontsWhenAlreadyInitialized() throws Exception {
        BaseFont existing = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        setField(service, "titleFont", existing);
        setField(service, "certificationFont", existing);
        setField(service, "signatureFont", existing);
        setField(service, "applicationNameFont", existing);

        invokePrivate(service, "loadFont");

        Assertions.assertSame(existing, getField(service, "titleFont"));
        Assertions.assertSame(existing, getField(service, "certificationFont"));
        Assertions.assertSame(existing, getField(service, "signatureFont"));
        Assertions.assertSame(existing, getField(service, "applicationNameFont"));
    }

    @Test
    void generateImage_whenImageReadFails_throwsIOException() throws Exception {
        TestableCertificateGeneratorService failingService =
                new TestableCertificateGeneratorService(courseEnrollmentRepository, courseRepository, userRepository);
        failingService.imageStream = new ThrowingInputStream();

        InvocationTargetException exception = Assertions.assertThrows(InvocationTargetException.class,
                () -> invokePrivate(failingService, "generateImage", mock(PdfContentByte.class), 10f));

        Assertions.assertTrue(exception.getCause() instanceof IOException);
        Assertions.assertTrue(exception.getCause().getMessage().contains("Image could not be loaded"));
    }

    @Test
    void generatePdf_whenWriterCreationFails_wrapsException() throws Exception {
        TestableCertificateGeneratorService failingService =
                new TestableCertificateGeneratorService(courseEnrollmentRepository, courseRepository, userRepository);
        failingService.failWriter = true;

        InvocationTargetException exception = Assertions.assertThrows(InvocationTargetException.class,
                () -> invokePrivate(failingService, "generatePdf",
                        UUID.randomUUID(), "Student Name", "Course Name", LocalDateTime.now()));

        Assertions.assertTrue(exception.getCause() instanceof CertificateGenerationException);
    }

    private Object invokePrivate(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] argumentTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof PdfContentByte) {
                argumentTypes[i] = PdfContentByte.class;
            } else if (args[i] instanceof Float) {
                argumentTypes[i] = float.class;
            } else {
                argumentTypes[i] = args[i].getClass();
            }
        }
        Method method = CertificateGeneratorService.class.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = CertificateGeneratorService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String name) throws Exception {
        java.lang.reflect.Field field = CertificateGeneratorService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static InputStream fontStream() {
        return new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    }

    private static final class ThrowingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("broken stream");
        }
    }

    private static class TestableCertificateGeneratorService extends CertificateGeneratorService {
        private static Map<String, InputStream> resources;
        private InputStream imageStream;
        private boolean failWriter;

        TestableCertificateGeneratorService(CourseEnrollmentRepository courseEnrollmentRepository,
                                            CourseRepository courseRepository,
                                            UserRepository userRepository) throws IOException {
            super(courseEnrollmentRepository, courseRepository, userRepository);
        }

        @Override
        protected InputStream openResource(String path) {
            if (PATH_SITE_LOGO.equals(path) && imageStream != null) {
                return imageStream;
            }
            if (resources != null && resources.containsKey(path)) {
                return resources.get(path);
            }
            return super.openResource(path);
        }

        @Override
        protected PdfWriter createPdfWriter(Document document, ByteArrayOutputStream generatedPdfBytes) throws DocumentException {
            if (failWriter) {
                throw new DocumentException("writer failed");
            }
            return super.createPdfWriter(document, generatedPdfBytes);
        }
    }

    private static final String PATH_SITE_LOGO = "/images/crap_logo.png";
}
