package org.elearning.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CsrfResponse {
    private String csrfToken;
    private String headerName;
}
