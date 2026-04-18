package org.elearning.backend.student.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class StudentDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
}
