package org.elearning.backend.auth.dto.response;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AuthResponseTest {

    @Test
    void allArgsConstructor_andSetters_exposeValues() {
        UserDataResponse user = new UserDataResponse(
                UUID.randomUUID(),
                "Ana",
                "Pop",
                "ana@example.com",
                RoleName.STUDENT,
                UserStatus.ACTIVE,
                UUID.randomUUID(),
                "Example Academy",
                "School",
                "Romania",
                "Cluj-Napoca",
                "0711111111",
                "Example Street 10"
        );

        AuthResponse response = new AuthResponse("Login successful", "access-token", "refresh-token", user);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser()).isSameAs(user);

        response.setMessage("Updated");
        response.setAccessToken("new-access");
        response.setRefreshToken("new-refresh");
        response.setUser(null);

        assertThat(response.getMessage()).isEqualTo("Updated");
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(response.getUser()).isNull();
    }

    @Test
    void messageOnlyConstructor_setsOnlyMessage() {
        AuthResponse response = new AuthResponse("Only message");

        assertThat(response.getMessage()).isEqualTo("Only message");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getUser()).isNull();
    }
}
