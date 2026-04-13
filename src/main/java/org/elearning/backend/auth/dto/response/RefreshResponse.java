package org.elearning.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class RefreshResponse {
    private String accessToken;
}
