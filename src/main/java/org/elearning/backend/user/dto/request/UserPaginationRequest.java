package org.elearning.backend.user.dto.request;

import org.elearning.backend.user.entity.UserStatus;

public record UserPaginationRequest(
        Integer page,
        Integer size,
        String search,
        String role,
        UserStatus status,
        String sortBy,
        String sortDir
) {
}
