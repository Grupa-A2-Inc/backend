package org.elearning.backend.parent.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.parent.repository.ParentRepository;
import org.elearning.backend.role.entity.RoleName;
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
    private static final String PARENT_NOT_FOUND = "Parent not found";

    public ParentDTO getParent(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(()-> new EntityNotFoundException(PARENT_NOT_FOUND));

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
                .orElseThrow(() -> new EntityNotFoundException(PARENT_NOT_FOUND));

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
                .orElseThrow(() -> new EntityNotFoundException("Parent not found"));

        if (!parent.getRole().getName().equals(RoleName.PARENT)) {
            throw new IllegalArgumentException("User is not a parent");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        if (!student.getRole().getName().equals(RoleName.STUDENT)) {
            throw new IllegalArgumentException("User is not a student");
        }

        if (parent.getOrganization() == null || student.getOrganization() == null ||
                !parent.getOrganization().getId().equals(student.getOrganization().getId())) {
            throw new IllegalArgumentException("Parent and student are not in the same organization");
        }

        if (student.getParents().size() >= 2) {
            throw new IllegalStateException("Student already has two parents");
        }

        parent.getStudents().add(student);
        parentRepository.save(parent);
    }

    public void removeStudent(UUID parentId, UUID studentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(()-> new EntityNotFoundException(PARENT_NOT_FOUND));

        parent.getStudents().removeIf(s -> s.getId().equals(studentId));
        parentRepository.save(parent);
    }
}
