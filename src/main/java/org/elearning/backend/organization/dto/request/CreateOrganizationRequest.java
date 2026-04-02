package org.elearning.backend.organization.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    private String name;
    private String country;
    private String city;
    private String organizationType;
    private String address;
    private String phoneNumber;
    private UUID ownerId;
}