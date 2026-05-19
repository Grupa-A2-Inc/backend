package org.elearning.backend.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.elearning.backend.common.validation.ValidPhoneNumber;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Organization type is required")
    private String organizationType;

    private String address;

    @ValidPhoneNumber(message = "Phone number format is invalid")
    private String phoneNumber;

    private UUID ownerId;
}
