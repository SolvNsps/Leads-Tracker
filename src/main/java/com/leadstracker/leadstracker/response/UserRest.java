package com.leadstracker.leadstracker.response;


import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;

import java.time.LocalDateTime;

public class UserRest {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phoneNumber;
    private String staffId;
    private String team;
    private LocalDateTime createdDate;

    private int targetValue;
    private int progress;
    private double progressPercentage;
    private String progressFraction;
//    private String teamName; // Optional -  if user is a Team Member
//    private TeamPerformanceDto teamPerformance;
//    private TeamMemberPerformanceDto memberPerformance;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getProgressFraction() {
        return progressFraction;
    }

    public void setProgressFraction(String progressFraction) {
        this.progressFraction = progressFraction;
    }
}
