package org.elearning.backend.classroom;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class ClassroomMembershipRepositoryTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ClassroomMembershipRepository membershipRepository;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    ClassroomRepository classroomRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @MockitoBean
    JavaMailSender javaMailSender;

    private Organization organization;
    private Classroom classroom;
    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        cleanupAll();

        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), 'membership-owner@test.com', 'hash', 'Owner', 'One', 2, 'ACTIVE')"
        );
        jdbcTemplate.execute(
                "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                        "VALUES (" +
                        "gen_random_uuid(), " +
                        "'Scoala Membership Test', 'Romania', 'Cluj', 'Scoala', " +
                        "(SELECT id FROM users WHERE email = 'membership-owner@test.com'))"
        );
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status, organization_id) " +
                        "VALUES (gen_random_uuid(), 'membership-teacher@test.com', 'hash', 'Teacher', 'One', 3, 'ACTIVE', " +
                        "(SELECT id FROM organizations WHERE name = 'Scoala Membership Test'))"
        );
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status, organization_id) " +
                        "VALUES (gen_random_uuid(), 'membership-student@test.com', 'hash', 'Student', 'One', 4, 'ACTIVE', " +
                        "(SELECT id FROM organizations WHERE name = 'Scoala Membership Test'))"
        );

        String orgId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM organizations WHERE name = 'Scoala Membership Test'",
                String.class
        );
        organization = organizationRepository.getReferenceById(UUID.fromString(orgId));

        String teacherId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM users WHERE email = 'membership-teacher@test.com'",
                String.class
        );
        teacher = userRepository.getReferenceById(UUID.fromString(teacherId));

        String studentId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM users WHERE email = 'membership-student@test.com'",
                String.class
        );
        student = userRepository.getReferenceById(UUID.fromString(studentId));

        Classroom c = new Classroom();
        c.setOrganization(organization);
        c.setName("Membership Test Class");
        c.setDescription("Test");
        classroom = classroomRepository.save(c);
    }

    @AfterEach
    void tearDown() {
        cleanupAll();
    }

    private void cleanupAll() {
        jdbcTemplate.execute("DELETE FROM classroom_memberships");
        jdbcTemplate.execute("DELETE FROM classrooms");
        // Nullify the circular reference first
        jdbcTemplate.execute("UPDATE users SET organization_id = NULL WHERE email IN ('membership-owner@test.com', 'membership-teacher@test.com', 'membership-student@test.com')");
        jdbcTemplate.execute("DELETE FROM organizations WHERE name = 'Scoala Membership Test'");
        jdbcTemplate.execute("DELETE FROM users WHERE email IN ('membership-owner@test.com', 'membership-teacher@test.com', 'membership-student@test.com')");
    }

    // --- Schema tests ---

    @Test
    void shouldHaveClassroomMembershipsTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tables).contains("classroom_memberships");
    }

    @Test
    void shouldHaveCorrectColumnsForClassroomMemberships() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'classroom_memberships'",
                String.class
        );
        assertThat(columns).contains("id", "classroom_id", "user_id", "membership_type", "created_at");
    }

    @Test
    void shouldHaveIndexesForClassroomMemberships() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'classroom_memberships'",
                String.class
        );
        assertThat(indexes).contains(
                "uq_classroom_user",
                "idx_memberships_classroom_id",
                "idx_memberships_user_id",
                "idx_memberships_user_type"
        );
    }

    @Test
    void shouldEnforceUniqueConstraintForSameUserInSameClassroom() {
        saveMembership(classroom, teacher, MembershipType.TEACHER);

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classroom_memberships (id, classroom_id, user_id, membership_type) " +
                                "VALUES (gen_random_uuid(), '" + classroom.getId() + "', '" + teacher.getId() + "', 'TEACHER')"
                )
        );
    }

    @Test
    void shouldAllowSameUserInDifferentClassrooms() {
        Classroom second = new Classroom();
        second.setOrganization(organization);
        second.setName("Second Class");
        second = classroomRepository.save(second);

        ClassroomMembership m1 = new ClassroomMembership();
        m1.setClassroom(classroom);
        m1.setUser(student);
        m1.setMembershipType(MembershipType.STUDENT);

        ClassroomMembership m2 = new ClassroomMembership();
        m2.setClassroom(second);
        m2.setUser(student);
        m2.setMembershipType(MembershipType.STUDENT);

        assertDoesNotThrow(() -> {
            membershipRepository.saveAndFlush(m1);
            membershipRepository.saveAndFlush(m2);
        });
    }

    @Test
    void shouldEnforceForeignKeyOnClassroomId() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classroom_memberships (id, classroom_id, user_id, membership_type) " +
                                "VALUES (gen_random_uuid(), gen_random_uuid(), '" + student.getId() + "', 'STUDENT')"
                )
        );
    }

    @Test
    void shouldEnforceForeignKeyOnUserId() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classroom_memberships (id, classroom_id, user_id, membership_type) " +
                                "VALUES (gen_random_uuid(), '" + classroom.getId() + "', gen_random_uuid(), 'STUDENT')"
                )
        );
    }

    // --- Repository query tests ---

    @Test
    void findAllByClassroomId_returnsAllMembersOfClassroom() {
        saveMembership(classroom, teacher, MembershipType.TEACHER);
        saveMembership(classroom, student, MembershipType.STUDENT);

        List<ClassroomMembership> members = membershipRepository.findAllByClassroomId(classroom.getId());

        assertThat(members).hasSize(2);
        assertThat(members).extracting(m -> m.getUser().getId())
                .containsExactlyInAnyOrder(teacher.getId(), student.getId());
    }

    @Test
    void findAllByClassroomIdAndMembershipType_returnsOnlyTeachers() {
        saveMembership(classroom, teacher, MembershipType.TEACHER);
        saveMembership(classroom, student, MembershipType.STUDENT);

        List<ClassroomMembership> teachers = membershipRepository
                .findAllByClassroomIdAndMembershipType(classroom.getId(), MembershipType.TEACHER);

        assertThat(teachers).hasSize(1);
        assertThat(teachers.get(0).getUser().getId()).isEqualTo(teacher.getId());
    }

    @Test
    void findAllByClassroomIdAndMembershipType_returnsOnlyStudents() {
        saveMembership(classroom, teacher, MembershipType.TEACHER);
        saveMembership(classroom, student, MembershipType.STUDENT);

        List<ClassroomMembership> students = membershipRepository
                .findAllByClassroomIdAndMembershipType(classroom.getId(), MembershipType.STUDENT);

        assertThat(students).hasSize(1);
        assertThat(students.get(0).getUser().getId()).isEqualTo(student.getId());
    }

    @Test
    void findAllByUserIdAndMembershipType_returnsClassroomsForStudent() {
        Classroom second = new Classroom();
        second.setOrganization(organization);
        second.setName("Second Class For Student");
        second = classroomRepository.save(second);

        saveMembership(classroom, student, MembershipType.STUDENT);
        saveMembership(second, student, MembershipType.STUDENT);
        saveMembership(classroom, teacher, MembershipType.TEACHER);

        List<ClassroomMembership> studentMemberships = membershipRepository
                .findAllByUserIdAndMembershipType(student.getId(), MembershipType.STUDENT);

        assertThat(studentMemberships).hasSize(2);
        assertThat(studentMemberships).extracting(m -> m.getClassroom().getId())
                .containsExactlyInAnyOrder(classroom.getId(), second.getId());
    }

    @Test
    void findAllByUserIdAndMembershipType_returnsClassroomsForTeacher() {
        saveMembership(classroom, teacher, MembershipType.TEACHER);
        saveMembership(classroom, student, MembershipType.STUDENT);

        List<ClassroomMembership> teacherMemberships = membershipRepository
                .findAllByUserIdAndMembershipType(teacher.getId(), MembershipType.TEACHER);

        assertThat(teacherMemberships).hasSize(1);
        assertThat(teacherMemberships.get(0).getClassroom().getId()).isEqualTo(classroom.getId());
    }

    @Test
    void findAllByUserId_returnsAllMembershipsForUser() {
        Classroom second = new Classroom();
        second.setOrganization(organization);
        second.setName("Second Class All");
        second = classroomRepository.save(second);

        saveMembership(classroom, student, MembershipType.STUDENT);
        saveMembership(second, student, MembershipType.STUDENT);

        List<ClassroomMembership> all = membershipRepository.findAllByUserId(student.getId());

        assertThat(all).hasSize(2);
    }

    @Test
    void existsByClassroomIdAndUserId_returnsTrueWhenMembershipExists() {
        saveMembership(classroom, student, MembershipType.STUDENT);

        boolean exists = membershipRepository.existsByClassroomIdAndUserId(classroom.getId(), student.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByClassroomIdAndUserId_returnsFalseWhenMembershipDoesNotExist() {
        boolean exists = membershipRepository.existsByClassroomIdAndUserId(classroom.getId(), student.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void createdAt_isPopulatedAutomatically() {
        ClassroomMembership membership = saveMembership(classroom, student, MembershipType.STUDENT);

        assertThat(membership.getCreatedAt()).isNotNull();
    }

    private ClassroomMembership saveMembership(Classroom c, User u, MembershipType type) {
        ClassroomMembership m = new ClassroomMembership();
        m.setClassroom(c);
        m.setUser(u);
        m.setMembershipType(type);
        return membershipRepository.saveAndFlush(m);
    }
}