package com.leadstracker.leadstracker.response;

import java.time.LocalDate;

public class UserTargetResponseDto {
    private String userId;
    private String fullName;
    private int assignedTargetValue;
    private LocalDate dueDate;
    private LocalDate dateAssigned;
    private String progressAchieved;
    private int progressPercentage;

    public UserTargetResponseDto() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAssignedTargetValue() {
        return assignedTargetValue;
    }

    public void setAssignedTargetValue(int assignedTargetValue) {
        this.assignedTargetValue = assignedTargetValue;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getDateAssigned() {
        return dateAssigned;
    }

    public void setDateAssigned(LocalDate dateAssigned) {
        this.dateAssigned = dateAssigned;
    }

    public String getProgressAchieved() {
        return progressAchieved;
    }

    public void setProgressAchieved(String progressAchieved) {
        this.progressAchieved = progressAchieved;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
