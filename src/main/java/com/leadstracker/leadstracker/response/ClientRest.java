package com.leadstracker.leadstracker.response;

import java.time.LocalDateTime;
import java.util.List;

public class ClientRest {
    private String clientId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String clientStatus;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    private String lastAction;
    private String createdBy;
    private String assignedTo;
    private String gpsLocation;
    private String teamName;

    private List<ClientStatusHistoryRest> statusHistory;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(String clientStatus) {
        this.clientStatus = clientStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setGpsLocation(String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public String getGpsLocation() {
        return gpsLocation;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<ClientStatusHistoryRest> getStatusHistory() {
        return statusHistory;
    }

    @Override
    public String toString() {
        return "ClientRest{" +
                "clientId='" + clientId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", clientStatus='" + clientStatus + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", createdAt=" + createdAt +
                ", lastAction='" + lastAction + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", gpsLocation='" + gpsLocation + '\'' +
                ", teamName='" + teamName + '\'' +
                ", statusHistory=" + statusHistory +
                '}';
    }

    public void setStatusHistory(List<ClientStatusHistoryRest> statusHistory) {
        this.statusHistory = statusHistory;
    }
}
