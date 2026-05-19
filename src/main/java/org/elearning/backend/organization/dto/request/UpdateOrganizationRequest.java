package org.elearning.backend.organization.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class UpdateOrganizationRequest extends BaseOrganizationRequest {
    public UpdateOrganizationRequest(
            String name,
            String country,
            String city,
            String organizationType,
            String address,
            String phoneNumber
    ) {
        super(name, country, city, organizationType, address, phoneNumber);
    }
}
