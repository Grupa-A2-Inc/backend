package org.elearning.backend.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.parent.dto.ParentDTO;
import org.elearning.backend.parent.service.ParentService;
import org.elearning.backend.student.dto.StudentDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/parents")
@RequiredArgsConstructor
public class ParentController {
    private final ParentService parentService;

    @Operation(summary = "Get parent by id", description = "Returns a parent profile visible to administrators in the same organization")
    @ApiResponse(responseCode = "200", description = "Parent returned successfully",
            content = @Content(schema = @Schema(implementation = ParentDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent not found", content = @Content)
    @GetMapping("/{parentId}")
    @PreAuthorize("@accessService.canViewParent(authentication,#parentId)")
    public ResponseEntity<ParentDTO> getParent(@P("parentId") @PathVariable UUID parentId){
        return ResponseEntity.ok(parentService.getParent(parentId));
    }

    @Operation(summary = "Get all parents", description = "Returns all parents. Only platform administrators can access this endpoint")
    @ApiResponse(responseCode = "200", description = "Parents returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParentDTO.class))))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @GetMapping
    @PreAuthorize("@accessService.canViewAllParents(authentication)")
    public ResponseEntity<List<ParentDTO>> getAllParents(){
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @Operation(summary = "Assign student to parent", description = "Links a student to a parent. The caller must be an admin or an organization admin for both users")
    @ApiResponse(responseCode = "200", description = "Student assigned successfully", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent or student not found", content = @Content)
    @PostMapping("/{parentId}/students/{studentId}")
    @PreAuthorize("@accessService.canManageParentStudent(authentication,#parentId,#studentId)")
    public ResponseEntity<Void> addStudent(@P("parentId") @PathVariable UUID parentId,
                                           @P("studentId") @PathVariable UUID studentId){
        parentService.addStudent(parentId, studentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get parent's students", description = "Returns the students linked to a parent. Admins, organization admins in the same organization, and the parent can access this endpoint")
    @ApiResponse(responseCode = "200", description = "Students returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentDTO.class))))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent not found", content = @Content)
    @GetMapping("/{parentId}/students")
    @PreAuthorize("@accessService.canViewParentStudents(authentication,#parentId)")
    public ResponseEntity<List<StudentDTO>> getStudents(@P("parentId") @PathVariable UUID parentId) {
        return ResponseEntity.ok(parentService.getStudents(parentId));
    }

    @Operation(summary = "Remove student from parent", description = "Unlinks a student from a parent. The caller must be an admin or an organization admin for both users")
    @ApiResponse(responseCode = "204", description = "Student removed successfully", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent or student not found", content = @Content)
    @DeleteMapping("/{parentId}/students/{studentId}")
    @PreAuthorize("@accessService.canManageParentStudent(authentication,#parentId,#studentId)")
    public ResponseEntity<Void> removeStudent(@P("parentId") @PathVariable UUID parentId,
                                              @P("studentId") @PathVariable UUID studentId){
        parentService.removeStudent(parentId, studentId);
        return ResponseEntity.noContent().build();
    }
}
