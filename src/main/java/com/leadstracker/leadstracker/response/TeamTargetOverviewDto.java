package com.leadstracker.leadstracker.response;

import java.time.LocalDate;
import java.util.List;

public class TeamTargetOverviewDto {

    private int totalTargetValue;
    private LocalDate dueDate;
    private LocalDate dateAssigned;
    private String progressAchieve;
    private int progressPercentage;
//    private int progressRemaining;
//    private int progressValue;

    // Team Target Distribution
    private List<UserTargetResponseDto> memberDistributions;

    public TeamTargetOverviewDto() {}


    public int getTotalTargetValue() {
        return totalTargetValue;
    }

    public void setTotalTargetValue(int totalTargetValue) {
        this.totalTargetValue = totalTargetValue;
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

    public String getProgressAchieve() {
        return progressAchieve;
    }

    public void setProgressAchieve(String progressAchieve) {
        this.progressAchieve = progressAchieve;
    }

    public List<UserTargetResponseDto> getMemberDistributions() {
        return memberDistributions;
    }

    public void setMemberDistributions(List<UserTargetResponseDto> memberDistributions) {
        this.memberDistributions = memberDistributions;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
