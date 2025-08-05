package com.leadstracker.leadstracker.request;

import com.leadstracker.leadstracker.DTO.MemberTargetDto;

import java.util.List;
import java.util.Map;

public class TargetDistributionRequest {

    private Long teamTargetId;
    private List<MemberTargetDto> memberTargets;

    public Long getTeamTargetId() {
        return teamTargetId;
    }

    public void setTeamTargetId(Long teamTargetId) {
        this.teamTargetId = teamTargetId;
    }

    public List<MemberTargetDto> getMemberTargets() {
        return memberTargets;
    }

    public void setMemberTargets(List<MemberTargetDto> memberTargets) {
        this.memberTargets = memberTargets;
    }
}
