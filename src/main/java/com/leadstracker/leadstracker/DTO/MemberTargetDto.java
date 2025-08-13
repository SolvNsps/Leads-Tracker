package com.leadstracker.leadstracker.DTO;

public class MemberTargetDto {

    private String memberId;
    private int assignedTarget;

    public MemberTargetDto() {}

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getAssignedTarget() {
        return assignedTarget;
    }

    public void setAssignedTarget(int assignedTarget) {
        this.assignedTarget = assignedTarget;
    }
}
