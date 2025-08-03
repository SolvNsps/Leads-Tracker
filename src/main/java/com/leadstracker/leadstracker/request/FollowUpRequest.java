package com.leadstracker.leadstracker.request;

import com.leadstracker.leadstracker.response.Statuses;

public class FollowUpRequest {
    private Long notificationId;
    private String actionTaken;
    private String newStatus;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
