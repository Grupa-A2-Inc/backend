package org.elearning.backend.organization.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationResponse {
    private UUID id;
    private String name;
    private UUID ownerId;
    private String ownerEmail;
}