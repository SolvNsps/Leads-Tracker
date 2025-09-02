package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.entities.UserEntity;

import java.time.Instant;
import java.time.LocalDateTime;

public class ClientStatusHistoryDto {
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
    private UserDto changedBy;

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }


    public UserDto getChangedBy() {
        return changedBy;
    }

    @Override
    public String toString() {
        return "ClientStatusHistoryDto{" +
                "oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", changedAt=" + changedAt +
                ", changedBy=" + changedBy +
                '}';
    }

    public void setChangedBy(UserDto changedBy) {
        this.changedBy = changedBy;
    }
}
