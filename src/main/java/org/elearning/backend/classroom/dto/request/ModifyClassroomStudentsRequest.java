package org.elearning.backend.classroom.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModifyClassroomStudentsRequest {

    @NotEmpty(message = "Student ids are required")
    private Set<@NotNull UUID> studentIds;

}
