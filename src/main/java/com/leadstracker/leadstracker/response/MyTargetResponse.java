package com.leadstracker.leadstracker.response;

import java.time.LocalDate;

public class MyTargetResponse {
    private Integer targetValue;
    private LocalDate dueDate;
    private LocalDate assignedDate;
    private Integer progressRemaining;


    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
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
}
