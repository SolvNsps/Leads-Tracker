package com.leadstracker.leadstracker.response;

import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;

import java.time.LocalDateTime;
import java.util.List;

public class TeamRest {
    private Long teamId;
    private String name;
    private String teamLeadUserId;
    private String teamLeadName;
    private String teamLeadEmail;
    private String leadPhoneNumber;
    private String leadStaffId;
    private LocalDateTime createdDate;
    private PaginatedResponse<TeamMemberPerformanceDto> teamMembers;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getTeamLeadUserId() {
        return teamLeadUserId;
    }

    public void setTeamLeadUserId(String teamLeadUserId) {
        this.teamLeadUserId = teamLeadUserId;
    }

    public String getTeamLeadName() {
        return teamLeadName;
    }

    public void setTeamLeadName(String teamLeadName) {
        this.teamLeadName = teamLeadName;
    }

    public PaginatedResponse<TeamMemberPerformanceDto> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(PaginatedResponse<TeamMemberPerformanceDto> teamMembers) {
        this.teamMembers = teamMembers;
    }
}
