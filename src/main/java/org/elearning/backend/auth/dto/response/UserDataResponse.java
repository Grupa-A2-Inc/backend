package org.elearning.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.UserStatus;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserDataResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private RoleName role;
    private UserStatus status;
    private UUID organizationId;
    private String organizationName;
    private String organizationType;
    private String country;
    private String city;
    private String organizationPhoneNumber;
    private String organizationAddress;
}