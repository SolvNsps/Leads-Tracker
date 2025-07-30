package com.leadstracker.leadstracker.response;

import java.time.LocalDate;

public class TeamTargetResponseDto {

    private Long id;
    private String teamName;
    private String teamLeadFullName;
    private int targetValue;
    private LocalDate dueDate;

    public TeamTargetResponseDto(Long id, String teamName, int targetValue, LocalDate dueDate){
        this.id = id;
        this.teamName = teamName;
        this.targetValue = targetValue;
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLeadFullName() {
        return teamLeadFullName;
    }

    public void setTeamLeadFullName(String teamLeadFullName) {
        this.teamLeadFullName = teamLeadFullName;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

}
