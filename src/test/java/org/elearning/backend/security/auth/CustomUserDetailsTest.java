package org.elearning.backend.security.auth;

import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void exposesWrappedUserFieldsAndAuthorities() {
        User user = makeUser(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getUser()).isSameAs(user);
        assertThat(userDetails.getUserId()).isEqualTo(user.getId());
        assertThat(userDetails.getUsername()).isEqualTo(user.getEmail());
        assertThat(userDetails.getPassword()).isEqualTo(user.getPasswordHash());
        assertThat(userDetails.getRoleName()).isEqualTo(RoleName.TEACHER);
        assertThat(userDetails.getOrganizationId()).isEqualTo(user.getOrganization().getId());
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_returnsFalseForNonActiveUser() {
        CustomUserDetails userDetails = new CustomUserDetails(makeUser(UserStatus.INACTIVE));

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void getOrganizationId_returnsNullWhenUserHasNoOrganization() {
        User user = makeUser(UserStatus.ACTIVE);
        user.setOrganization(null);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getOrganizationId()).isNull();
    }

    private User makeUser(UserStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("teacher@example.com");
        user.setPasswordHash("hashed");
        user.setRole(new Role(RoleName.TEACHER));
        user.setStatus(status);
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        user.setOrganization(organization);
        return user;
    }
}
