package org.elearning.backend.parent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.student.dto.StudentDTO;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public class ParentDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private List<StudentDTO> students;
}
