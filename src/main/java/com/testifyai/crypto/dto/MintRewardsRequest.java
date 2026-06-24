package com.testifyai.crypto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MintRewardsRequest
{
    @NotEmpty
    @Valid
    private List<StudentRewardRequest> rewards;

    public List<StudentRewardRequest> getRewards() {
        return rewards;
    }

    public void setRewards(List<StudentRewardRequest> rewards) {
        this.rewards = rewards;
    }
}