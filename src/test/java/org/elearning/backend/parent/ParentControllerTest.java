package org.elearning.backend.parent;

import org.elearning.backend.parent.controller.ParentController;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.service.ParentService;
import org.elearning.backend.student.dto.StudentDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentControllerTest {

    @Mock
    private ParentService parentService;

    @InjectMocks
    private ParentController parentController;

    @Test
    void getParent_returnsParentFromService() {
        UUID parentId = UUID.randomUUID();
        ParentDTO parent = ParentDTO.builder()
                .id(parentId)
                .firstName("Ion")
                .lastName("Pop")
                .email("ion@example.com")
                .build();
        when(parentService.getParent(parentId)).thenReturn(parent);

        ResponseEntity<ParentDTO> response = parentController.getParent(parentId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(parent);
        verify(parentService).getParent(parentId);
    }

    @Test
    void getAllParents_returnsParentsFromService() {
        ParentDTO parent = ParentDTO.builder()
                .id(UUID.randomUUID())
                .firstName("Ana")
                .lastName("Pop")
                .email("ana@example.com")
                .build();
        when(parentService.getAllParents()).thenReturn(List.of(parent));

        ResponseEntity<List<ParentDTO>> response = parentController.getAllParents();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(parent);
        verify(parentService).getAllParents();
    }

    @Test
    void addStudent_delegatesAndReturns200() {
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        ResponseEntity<Void> response = parentController.addStudent(parentId, studentId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNull();
        verify(parentService).addStudent(parentId, studentId);
    }

    @Test
    void getStudents_returnsStudentsFromService() {
        UUID parentId = UUID.randomUUID();
        StudentDTO student = StudentDTO.builder()
                .id(UUID.randomUUID())
                .firstName("Maria")
                .lastName("Pop")
                .email("maria@example.com")
                .build();
        when(parentService.getStudents(parentId)).thenReturn(List.of(student));

        ResponseEntity<List<StudentDTO>> response = parentController.getStudents(parentId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(student);
        verify(parentService).getStudents(parentId);
    }

    @Test
    void removeStudent_delegatesAndReturns204() {
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        ResponseEntity<Void> response = parentController.removeStudent(parentId, studentId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
        verify(parentService).removeStudent(parentId, studentId);
    }
}
