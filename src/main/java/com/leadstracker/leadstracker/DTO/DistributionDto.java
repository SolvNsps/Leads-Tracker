package com.leadstracker.leadstracker.DTO;

import java.util.List;
import java.util.Map;

public class DistributionDto {
    private Long teamTargetId;
    private Map<String, Integer> memberTargets;

    public DistributionDto(Long teamTargetId, Map<String, Integer> memberTargets) {
        this.teamTargetId = teamTargetId;
        this.memberTargets = memberTargets;
    }

    public Long getTeamTargetId() {
        return teamTargetId;
    }

    public void setTeamTargetId(Long teamTargetId) {
        this.teamTargetId = teamTargetId;
    }

    public Map<String, Integer> getMemberTargets() {
        return memberTargets;
    }

    public void setMemberTargets(Map<String, Integer> memberTargets) {
        this.memberTargets = memberTargets;
    }
}
