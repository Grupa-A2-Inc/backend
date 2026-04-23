package org.elearning.backend.user.dto.request;

<<<<<<< Updated upstream
=======
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
>>>>>>> Stashed changes
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elearning.backend.role.entity.RoleName;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private RoleName roleName;
    private UUID organizationId;
}