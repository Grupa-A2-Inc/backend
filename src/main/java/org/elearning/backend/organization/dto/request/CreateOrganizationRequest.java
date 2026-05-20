package org.elearning.backend.organization.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateOrganizationRequest extends OrganizationRequestFields {
    private UUID ownerId;

    public CreateOrganizationRequest(
            String name,
            String country,
            String city,
            String organizationType,
            String address,
            String phoneNumber,
            UUID ownerId
    ) {
        super(name, country, city, organizationType, address, phoneNumber);
        this.ownerId = ownerId;
    }
}
