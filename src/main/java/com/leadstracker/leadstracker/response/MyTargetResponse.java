package com.leadstracker.leadstracker.response;

import java.time.LocalDate;

public class MyTargetResponse {
    private Integer totalTargetValue;
    private LocalDate dueDate;
    private LocalDate assignedDate;
    private Integer progressRemaining;
    private Integer progressPercentage;
    private Integer progressValue;


    public Integer getTotalTargetValue() {
        return totalTargetValue;
    }

    public void setTotalTargetValue(Integer totalTargetValue) {
        this.totalTargetValue = totalTargetValue;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Integer getProgressRemaining() {
        return progressRemaining;
    }

    public void setProgressRemaining(Integer progressRemaining) {
        this.progressRemaining = progressRemaining;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Integer getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(Integer progressValue) {
        this.progressValue = progressValue;
    }
}
