package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.response.PaginatedResponse;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TeamDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String teamLeadId;
    private String teamLeadName;
    private String teamLeadEmail;
    private String leadPhoneNumber;
    private String leadStaffId;
    private java.time.LocalDateTime createdDate;
    private PaginatedResponse<TeamMemberPerformanceDto> teamMembers;
    private int totalClientsSubmitted;
    private int totalTarget;
    private double progressPercentage;
    private Map<String, Integer> teamClientStatus;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamLeadName() {
        return teamLeadName;
    }

    public void setTeamLeadName(String teamLeadName) {
        this.teamLeadName = teamLeadName;
    }

    public String getTeamLeadId() {
        return teamLeadId;
    }

    public void setTeamLeadId(String teamLeadId) {
        this.teamLeadId = teamLeadId;
    }

    public String getTeamLeadEmail() {
        return teamLeadEmail;
    }

    public void setTeamLeadEmail(String teamLeadEmail) {
        this.teamLeadEmail = teamLeadEmail;
    }

    public String getLeadPhoneNumber() {
        return leadPhoneNumber;
    }

    public void setLeadPhoneNumber(String leadPhoneNumber) {
        this.leadPhoneNumber = leadPhoneNumber;
    }

    public String getLeadStaffId() {
        return leadStaffId;
    }

    public void setLeadStaffId(String leadStaffId) {
        this.leadStaffId = leadStaffId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public PaginatedResponse<TeamMemberPerformanceDto> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(PaginatedResponse<TeamMemberPerformanceDto> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public int getTotalClientsSubmitted() {
        return totalClientsSubmitted;
    }

    public void setTotalClientsSubmitted(int totalClientsSubmitted) {
        this.totalClientsSubmitted = totalClientsSubmitted;
    }

    public int getTotalTarget() {
        return totalTarget;
    }

    public void setTotalTarget(int totalTarget) {
        this.totalTarget = totalTarget;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Map<String, Integer> getTeamClientStatus() {
        return teamClientStatus;
    }

    public void setTeamClientStatus(Map<String, Integer> teamClientStatus) {
        this.teamClientStatus = teamClientStatus;
    }
}
