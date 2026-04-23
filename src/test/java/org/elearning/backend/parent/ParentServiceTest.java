package org.elearning.backend.parent;

import jakarta.persistence.EntityNotFoundException;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.parent.repository.ParentRepository;
import org.elearning.backend.parent.service.ParentService;
import org.elearning.backend.student.dto.StudentDTO;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ParentService parentService;

    private Parent parent;
    private Student student;

    @BeforeEach
    void setUp() {
        parent = new Parent();
        parent.setId(UUID.randomUUID());
        parent.setFirstName("Ion");
        parent.setLastName("Popescu");
        parent.setEmail("ion@test.com");

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFirstName("Maria");
        student.setLastName("Popescu");
        student.setEmail("maria@test.com");
    }

    @Test
    void getParent_success() {
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        ParentDTO result = parentService.getParent(parent.getId());

        assertThat(result.getId()).isEqualTo(parent.getId());
        assertThat(result.getFirstName()).isEqualTo("Ion");
        assertThat(result.getLastName()).isEqualTo("Popescu");
        assertThat(result.getEmail()).isEqualTo("ion@test.com");
    }

    @Test
    void getParent_notFound_throwsException() {
        UUID parentId = UUID.randomUUID();

        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.getParent(parentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Parent not found");
    }

    @Test
    void getAllParents_success() {
        when(parentRepository.findAll())
                .thenReturn(List.of(parent));

        List<ParentDTO> result = parentService.getAllParents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("ion@test.com");
        assertThat(result.get(0).getFirstName()).isEqualTo("Ion");
        assertThat(result.get(0).getLastName()).isEqualTo("Popescu");
    }

    @Test
    void getAllParents_empty() {
        when(parentRepository.findAll())
                .thenReturn(List.of());

        List<ParentDTO> result = parentService.getAllParents();

        assertThat(result).isEmpty();
    }

    @Test
    void getStudents_success() {
        parent.getStudents().add(student);
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        List<StudentDTO> result = parentService.getStudents(parent.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("maria@test.com");
        assertThat(result.get(0).getFirstName()).isEqualTo("Maria");
    }

    @Test
    void getStudents_parentNotFound_throwsException() {
        UUID parentId = UUID.randomUUID();

        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.getStudents(parentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Parent not found");
    }

    @Test
    void getStudents_empty() {
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        List<StudentDTO> result = parentService.getStudents(parent.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void addStudent_success() {
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));

        parentService.addStudent(parent.getId(), student.getId());

        assertThat(parent.getStudents()).contains(student);
        verify(parentRepository).save(parent);
    }

    @Test
    void addStudent_parentNotFound_throwsException() {
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Parent not found");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_studentNotFound_throwsException() {
        UUID studentId = UUID.randomUUID();
        UUID parentId = parent.getId();

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Student not found");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_alreadyHas2Parents_throwsException() {
        Parent parent1 = new Parent();
        Parent parent2 = new Parent();
        UUID parentId = parent.getId();
        UUID studentId = student.getId();

        student.getParents().add(parent1);
        student.getParents().add(parent2);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Student already has two parents");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void removeStudent_success() {
        parent.getStudents().add(student);
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        parentService.removeStudent(parent.getId(), student.getId());

        assertThat(parent.getStudents()).doesNotContain(student);
        verify(parentRepository).save(parent);
    }

    @Test
    void removeStudent_parentNotFound_throwsException() {
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.removeStudent(parentId, studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Parent not found");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void removeStudent_studentNotInList_doesNothing() {
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        parentService.removeStudent(parent.getId(), student.getId());

        assertThat(parent.getStudents()).isEmpty();
        verify(parentRepository).save(parent);
    }
<<<<<<< Updated upstream
}
=======

    @Test
    void addStudent_parentNotHaveParentRole_throwsException() {
        UUID parentId = parent.getId();
        UUID studentId = student.getId();
        Role teacherRole = new Role(RoleName.TEACHER);
        parent.setRole(teacherRole);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not a parent");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_studentNotHaveStudentRole_throwsException() {
        UUID parentId = parent.getId();
        UUID studentId = student.getId();
        Role teacherRole = new Role(RoleName.TEACHER);
        parent.setRole(new Role(RoleName.PARENT));
        student.setRole(teacherRole);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not a student");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_differentOrganization_throwsException() {
        UUID parentId = parent.getId();
        UUID studentId = student.getId();
        Organization org1 = new Organization();
        org1.setId(UUID.randomUUID());

        Organization org2 = new Organization();
        org2.setId(UUID.randomUUID());

        parent.setRole(new Role(RoleName.PARENT));
        parent.setOrganization(org1);

        student.setRole(new Role(RoleName.STUDENT));
        student.setOrganization(org2);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent and student are not in the same organization");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_parentWithoutOrganization_throwsException() {
        UUID parentId = parent.getId();
        UUID studentId = student.getId();
        parent.setOrganization(null);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent and student are not in the same organization");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_studentWithoutOrganization_throwsException() {
        UUID parentId = parent.getId();
        UUID studentId = student.getId();
        student.setOrganization(null);

        when(parentRepository.findById(parentId))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parentId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent and student are not in the same organization");

        verify(parentRepository, never()).save(any());
    }
}
>>>>>>> Stashed changes
