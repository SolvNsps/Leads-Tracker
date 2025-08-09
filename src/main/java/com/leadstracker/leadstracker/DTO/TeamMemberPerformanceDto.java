package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.response.Statuses;

import java.util.Map;

public class TeamMemberPerformanceDto {
    private String memberId;
    private String memberName;
    private int totalClientsSubmitted;
//    private int numberOfClients;
    private int target;
    private double progressPercentage;
    private Map<Statuses, Integer> clientStatus;
    private String progressFraction;
    private String email;
    private String teamName;
    private String teamLeadName;

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public int getTotalClientsSubmitted() {
        return totalClientsSubmitted;
    }

    public void setTotalClientsSubmitted(int totalClientsSubmitted) {
        this.totalClientsSubmitted = totalClientsSubmitted;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Map<Statuses, Integer> getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(Map<Statuses, Integer> clientStatus) {
        this.clientStatus = clientStatus;
    }

    public String getProgressFraction() {
        return progressFraction;
    }

    public void setProgressFraction(String progressFraction) {
        this.progressFraction = progressFraction;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLeadName() {
        return teamLeadName;
    }

    public void setTeamLeadName(String teamLeadName) {
        this.teamLeadName = teamLeadName;
    }
}
