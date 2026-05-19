package org.elearning.backend.organization.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
@NoArgsConstructor
public class CreateOrganizationRequest extends BaseOrganizationRequest {
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
