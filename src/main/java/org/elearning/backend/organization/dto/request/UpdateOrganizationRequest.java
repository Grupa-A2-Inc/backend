package org.elearning.backend.organization.dto.request;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UpdateOrganizationRequest extends OrganizationRequestFields {
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
