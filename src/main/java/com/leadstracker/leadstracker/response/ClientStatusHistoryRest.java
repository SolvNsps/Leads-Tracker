package com.leadstracker.leadstracker.response;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.UserEntity;

import java.time.LocalDateTime;

public class ClientStatusHistoryRest {
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
    private UserDto changedBy;



    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

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

    public UserDto getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UserDto changedBy) {
        this.changedBy = changedBy;
    }
}
