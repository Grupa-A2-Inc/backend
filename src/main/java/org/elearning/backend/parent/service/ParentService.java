package org.elearning.backend.parent.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.parent.repository.ParentRepository;
import org.elearning.backend.student.dto.StudentDTO;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentService {
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    public ParentDTO getParent(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(()-> new EntityNotFoundException("Parent not found"));

        return ParentDTO.builder()
                .id(parent.getId())
                .firstName(parent.getFirstName())
                .lastName(parent.getLastName())
                .email(parent.getEmail())
                .build();
    }

    public List<ParentDTO> getAllParents() {
        return parentRepository.findAll().stream()
                .map(parent -> ParentDTO.builder()
                        .id(parent.getId())
                        .firstName(parent.getFirstName())
                        .lastName(parent.getLastName())
                        .email(parent.getEmail())
                        .build())
                .toList();
    }

    public List<StudentDTO> getStudents(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent not found"));

        return parent.getStudents().stream()
                .map(student -> StudentDTO.builder()
                        .id(student.getId())
                        .firstName(student.getFirstName())
                        .lastName(student.getLastName())
                        .email(student.getEmail())
                        .build())
                .toList();
    }

    public void addStudent(UUID parentId, UUID studentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(()-> new EntityNotFoundException("Parent not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(()-> new EntityNotFoundException("Student not found"));

        if(student.getParents().size() >= 2) {
            throw new IllegalStateException("Student already has two parents");
        }

        parent.getStudents().add(student);
        parentRepository.save(parent);
    }

    public void removeStudent(UUID parentId, UUID studentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(()-> new EntityNotFoundException("Parent not found"));

        parent.getStudents().removeIf(s -> s.getId().equals(studentId));
        parentRepository.save(parent);
    }
}
