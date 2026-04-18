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
        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.getParent(UUID.randomUUID()))
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
        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.getStudents(UUID.randomUUID()))
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
        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.addStudent(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Parent not found");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_studentNotFound_throwsException() {
        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.addStudent(parent.getId(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Student not found");

        verify(parentRepository, never()).save(any());
    }

    @Test
    void addStudent_alreadyHas2Parents_throwsException() {
        Parent parent1 = new Parent();
        Parent parent2 = new Parent();
        student.getParents().add(parent1);
        student.getParents().add(parent2);

        when(parentRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));
        when(studentRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> parentService.addStudent(parent.getId(), student.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Student already has two parents"); // exact mesajul din service

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
        when(parentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.removeStudent(UUID.randomUUID(), UUID.randomUUID()))
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
}