package org.elearning.backend.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elearning.backend.common.validation.ValidPhoneNumber;

@Getter
@NoArgsConstructor
public abstract class OrganizationRequestFields {
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

    protected OrganizationRequestFields(
            String name,
            String country,
            String city,
            String organizationType,
            String address,
            String phoneNumber
    ) {
        this.name = name;
        this.country = country;
        this.city = city;
        this.organizationType = organizationType;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }
}
