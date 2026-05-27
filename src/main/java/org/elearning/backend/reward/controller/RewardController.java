package org.elearning.backend.reward.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.reward.dto.CalculateRewardCycleRequest;
import org.elearning.backend.reward.dto.RewardConfigRequest;
import org.elearning.backend.reward.dto.RewardConfigResponse;
import org.elearning.backend.reward.dto.RewardCycleResponse;
import org.elearning.backend.reward.dto.StudentRewardResponse;
import org.elearning.backend.reward.dto.StudentWalletRequest;
import org.elearning.backend.reward.dto.StudentWalletResponse;
import org.elearning.backend.reward.dto.funding.StablecoinFundingResponse;
import org.elearning.backend.reward.dto.funding.StablecoinPaymentRequest;
import org.elearning.backend.reward.service.RewardConfigService;
import org.elearning.backend.reward.service.RewardDistributionService;
import org.elearning.backend.reward.service.RewardFundingService;
import org.elearning.backend.reward.service.StudentWalletService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rewards")
public class RewardController {

    private final RewardConfigService rewardConfigService;
    private final RewardDistributionService rewardDistributionService;
    private final RewardFundingService rewardFundingService;
    private final StudentWalletService studentWalletService;

    @PutMapping("/organizations/{organizationId}/config")
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    public ResponseEntity<RewardConfigResponse> upsertConfig(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody RewardConfigRequest request
    ) {
        return ResponseEntity.ok(rewardConfigService.upsertConfig(organizationId, request));
    }

    @GetMapping("/organizations/{organizationId}/config")
    @PreAuthorize("@accessService.canViewOrganization(authentication, #organizationId)")
    public ResponseEntity<RewardConfigResponse> getConfig(
            @P("organizationId") @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(rewardConfigService.getConfig(organizationId));
    }

    @PutMapping("/students/me/wallet")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentWalletResponse> upsertMyWallet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudentWalletRequest request
    ) {
        return ResponseEntity.ok(studentWalletService.upsertWallet(userDetails.getUserId(), request));
    }

    @PostMapping("/cycles/{organizationId}/calculate")
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    public ResponseEntity<RewardCycleResponse> calculateCycle(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody CalculateRewardCycleRequest request
    ) {
        return ResponseEntity.ok(rewardDistributionService.calculateCycle(organizationId, request));
    }

    @PostMapping("/organizations/{organizationId}/mock-payment")
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    public ResponseEntity<StablecoinFundingResponse> mockOrganizationPayment(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody StablecoinPaymentRequest request
    ) {
        return ResponseEntity.ok(rewardFundingService.mockSepoliaPayment(organizationId, request));
    }

    @PostMapping("/organizations/{organizationId}/funding/stablecoin")
    @PreAuthorize("@accessService.canEditOrganization(authentication, #organizationId)")
    public ResponseEntity<StablecoinFundingResponse> fundWithStablecoinProvider(
            @P("organizationId") @PathVariable UUID organizationId,
            @Valid @RequestBody StablecoinPaymentRequest request
    ) {
        return ResponseEntity.ok(rewardFundingService.fundWithStablecoinProvider(organizationId, request));
    }

    @PostMapping("/cycles/{cycleId}/mint")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RewardCycleResponse> mintCycle(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(rewardDistributionService.mintCycle(cycleId));
    }

    @GetMapping("/cycles/{cycleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RewardCycleResponse> getCycle(@PathVariable UUID cycleId) {
        return ResponseEntity.ok(rewardDistributionService.getCycle(cycleId));
    }

    @GetMapping("/organizations/{organizationId}/latest")
    @PreAuthorize("@accessService.canViewOrganization(authentication, #organizationId)")
    public ResponseEntity<RewardCycleResponse> getLatestCycle(
            @P("organizationId") @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(rewardDistributionService.getLatestCycleForOrganization(organizationId));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN', 'TEACHER') or (hasRole('STUDENT') and #studentId == principal.userId)")
    public ResponseEntity<List<StudentRewardResponse>> getStudentRewards(@PathVariable UUID studentId) {
        return ResponseEntity.ok(rewardDistributionService.getStudentRewardHistory(studentId));
    }
}
