package org.elearning.backend.auth.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.UserStatus;

import java.util.UUID;

@Getter
@Setter
public class UserDataResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private RoleName role;
    private UserStatus status;
    private UUID organizationId;

    public UserDataResponse(UUID id, String firstName, String lastName,
                            String email, RoleName role, UserStatus status,
                            UUID organizationId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.organizationId = organizationId;
    }
}