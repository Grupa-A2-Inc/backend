package org.elearning.backend.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Parents",
        description = "Endpoints for retrieving parent records and managing relationships between parents and students. " +
                "These operations rely heavily on the distinction between platform-wide ADMIN permissions and organization-scoped ORGANIZATION_ADMIN permissions. " +
                "Some read operations may also be available to the parent account itself when the access rule allows it."
)
@RequestMapping("api/v1/parents")
@RequiredArgsConstructor
public class ParentController {
    private final ParentService parentService;

    @Operation(
            summary = "Get parent by id",
            description = "Returns a single parent profile identified by its UUID. The authorization model is scope-aware: a platform ADMIN can inspect parent records globally, " +
                    "while an ORGANIZATION_ADMIN is expected to stay within the boundaries of their own organization. This endpoint is intended for administrative lookup and support workflows."
    )
    @ApiResponse(responseCode = "200", description = "Parent returned successfully",
            content = @Content(schema = @Schema(implementation = ParentDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent not found", content = @Content)
    @GetMapping("/{parentId}")
    @PreAuthorize("@accessService.canViewParent(authentication,#parentId)")
    public ResponseEntity<ParentDTO> getParent(@P("parentId") @PathVariable UUID parentId){
        return ResponseEntity.ok(parentService.getParent(parentId));
    }

    @Operation(
            summary = "Get all parents",
            description = "Returns the full list of parent records across the platform. This endpoint is intentionally reserved for the global ADMIN role and is not the organization-scoped equivalent. " +
                    "ORGANIZATION_ADMIN users should not expect tenant-wide listing rights here, because the operation is defined as a platform-level administrative view."
    )
    @ApiResponse(responseCode = "200", description = "Parents returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParentDTO.class))))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @GetMapping
    @PreAuthorize("@accessService.canViewAllParents(authentication)")
    public ResponseEntity<List<ParentDTO>> getAllParents(){
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @Operation(
            summary = "Assign student to parent",
            description = "Creates a relationship between a parent and a student. The caller must satisfy the access rule for both records involved in the operation. " +
                    "A platform ADMIN may manage these links across the platform, whereas an ORGANIZATION_ADMIN may do so only when both the parent and the student belong to the same organization " +
                    "that the administrator manages. This distinction prevents cross-organization family-link changes."
    )
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

    @Operation(
            summary = "Get parent's students",
            description = "Returns the students currently linked to the specified parent. Access may be granted through different paths: a platform ADMIN can read broadly, " +
                    "an ORGANIZATION_ADMIN can read within the same organization, and the parent account itself may also be allowed to view its own linked students. " +
                    "This makes the endpoint broader than a pure admin-only endpoint, while still preserving organization boundaries."
    )
    @ApiResponse(responseCode = "200", description = "Students returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentDTO.class))))
    @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @ApiResponse(responseCode = "404", description = "Parent not found", content = @Content)
    @GetMapping("/{parentId}/students")
    @PreAuthorize("@accessService.canViewParentStudents(authentication,#parentId)")
    public ResponseEntity<List<StudentDTO>> getStudents(@P("parentId") @PathVariable UUID parentId) {
        return ResponseEntity.ok(parentService.getStudents(parentId));
    }

    @Operation(
            summary = "Remove student from parent",
            description = "Removes the relationship between a parent and a student. The authorization model mirrors the assignment endpoint: a platform ADMIN can operate globally, " +
                    "while an ORGANIZATION_ADMIN can remove links only when both records are inside the administrator's organization. This keeps parent-student relationship changes tenant-safe."
    )
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
