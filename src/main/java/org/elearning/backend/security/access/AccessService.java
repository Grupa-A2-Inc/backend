package org.elearning.backend.security.access;


import lombok.RequiredArgsConstructor;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("accessService")
@RequiredArgsConstructor
public class AccessService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public boolean canCreateUser(Authentication authentication, CreateUserRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if(currentUser == null) {
            return false;
        }

        if(currentUser.getRoleName() == RoleName.ADMIN) {
            return true;
        }

        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(request.getOrganizationId());
    }

    public boolean canViewUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if(currentUser == null) {
            return false;
        }
        if(currentUser.getRoleName() == RoleName.ADMIN) {
            return true;
        }

        if(currentUser.getUserId().equals(targetUserId)) {
            return true;
        }
        if(currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        return targetUser != null && targetUser.getOrganization() != null
                && currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canEditUser(Authentication authentication, UUID targetUserId) {
        return canViewUser(authentication, targetUserId);
    }

    public boolean canViewOrganization(Authentication authentication, UUID organizationId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if(currentUser == null) {
            return false;
        }

        if(currentUser.getRoleName() == RoleName.ADMIN) {
            return true;
        }

        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(organizationId)
                && organizationRepository.existsById(organizationId);

    }

    public boolean canEditOrganization(Authentication authentication, UUID organizationId) {
        return  canViewOrganization(authentication, organizationId);
    }

    public boolean canDeleteUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if(currentUser == null) {
            return false;
        }

        if(currentUser.getRoleName() == RoleName.ADMIN) {
            return true;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null || targetUser.getOrganization() == null) {
            return false;
        }

        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canChangePassword(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (currentUser.getRoleName() == RoleName.ADMIN) {
            return true;
        }

        return currentUser.getUserId().equals(targetUserId);
    }

    public CustomUserDetails extractCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails;
    }
}
