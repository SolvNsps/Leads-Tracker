package com.leadstracker.leadstracker.response;

import java.time.LocalDate;

public class UserTargetResponseDto {
    private Long userId;
    private String fullName;
    private int assignedTargetValue;
    private LocalDate dueDate;
    private LocalDate dateAssigned;
    private int progressAchieved; // total value achieved so far


    public UserTargetResponseDto() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public int getProgressAchieved() {
        return progressAchieved;
    }

    public void setProgressAchieved(int progressAchieved) {
        this.progressAchieved = progressAchieved;
    }


}
