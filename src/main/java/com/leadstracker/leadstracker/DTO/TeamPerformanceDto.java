package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.response.Statuses;

import java.util.List;
import java.util.Map;

public class TeamPerformanceDto {
    private Long teamId;
    private String teamName;
    private String teamLeadName;
    private int totalClientsAdded;
    private int teamTarget;
    private int numberOfClients;
    private double progressPercentage;
    private List<TeamMemberPerformanceDto> teamMembers;
    private Map<Statuses, Integer> clientStatus;
    private int numberOfTeamMembers;
    private String progressFraction;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
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

    public int getTotalClientsAdded() {
        return totalClientsAdded;
    }

    public void setTotalClientsAdded(int totalClientsAdded) {
        this.totalClientsAdded = totalClientsAdded;
    }

    public int getTeamTarget() {
        return teamTarget;
    }

    public void setTeamTarget(int teamTarget) {
        this.teamTarget = teamTarget;
    }

    public int getNumberOfClients() {
        return numberOfClients;
    }

    public void setNumberOfClients(int numberOfClients) {
        this.numberOfClients = numberOfClients;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public List<TeamMemberPerformanceDto> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMemberPerformanceDto> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public Map<Statuses, Integer> getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(Map<Statuses, Integer> clientStatus) {
        this.clientStatus = clientStatus;
    }

    public int getNumberOfTeamMembers() {
        return numberOfTeamMembers;
    }

    public void setNumberOfTeamMembers(int numberOfTeamMembers) {
        this.numberOfTeamMembers = numberOfTeamMembers;
    }

    public String getProgressFraction() {
        return progressFraction;
    }

    public void setProgressFraction(String progressFraction) {
        this.progressFraction = progressFraction;
    }
}
