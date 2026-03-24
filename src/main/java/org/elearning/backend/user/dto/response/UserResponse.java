package org.elearning.backend.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.UserStatus;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private RoleName roleName;
    private UserStatus status;
}
