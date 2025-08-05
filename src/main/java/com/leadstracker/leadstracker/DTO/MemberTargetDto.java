package com.leadstracker.leadstracker.DTO;

public class MemberTargetDto {

    private Long memberId;
    private int assignedTarget;

    public MemberTargetDto() {}

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public int getAssignedTarget() {
        return assignedTarget;
    }

    public void setAssignedTarget(int assignedTarget) {
        this.assignedTarget = assignedTarget;
    }
}
