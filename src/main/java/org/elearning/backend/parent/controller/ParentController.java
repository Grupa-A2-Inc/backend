package org.elearning.backend.parent.controller;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.service.ParentService;
import org.elearning.backend.student.dto.StudentDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/parents")
@RequiredArgsConstructor
public class ParentController {
    private final ParentService parentService;

    @GetMapping("/{parentId}")
    public ResponseEntity<ParentDTO> getParent(@PathVariable UUID parentId){
        return ResponseEntity.ok(parentService.getParent(parentId));
    }

    @GetMapping
    public ResponseEntity<List<ParentDTO>> getAllParents(){
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @PostMapping("/{parentId}/students/{studentId}")
    public ResponseEntity<Void> addStudent(@PathVariable UUID parentId, @PathVariable UUID studentId){
        parentService.addStudent(parentId, studentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{parentId}/students")
    public ResponseEntity<List<StudentDTO>> getStudents(@PathVariable UUID parentId) {
        return ResponseEntity.ok(parentService.getStudents(parentId));
    }

    @DeleteMapping("/{parentId}/students/{studentId}")
    public ResponseEntity<Void> removeStudent(@PathVariable UUID parentId, @PathVariable UUID studentId){
        parentService.removeStudent(parentId, studentId);
        return ResponseEntity.noContent().build();
    }
}
