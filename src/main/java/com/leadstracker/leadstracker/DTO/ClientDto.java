package com.leadstracker.leadstracker.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class ClientDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private String clientId;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    @JsonProperty("gpsLocation")
    private String gpsLocation;

    private UserDto createdBy;
    private String teamLeadId;
    private Date createdDate;
    private Date lastUpdated;
    private String clientStatus;
    private UserDto assignedTo;
    private TeamDto teamName;
    private List<ClientStatusHistoryDto> statusHistory;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getGpsLocation() {
        return gpsLocation;
    }

    public void setGpsLocation(String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public UserDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserDto createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(String clientStatus) {
        this.clientStatus = clientStatus;
    }

    public String getTeamLeadId() {
        return teamLeadId;
    }

    public void setTeamLeadId(String teamLeadId) {
        this.teamLeadId = teamLeadId;
    }

    public UserDto getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UserDto assignedTo) {
        this.assignedTo = assignedTo;
    }

    public TeamDto getTeamName() {
        return teamName;
    }

    public void setTeamName(TeamDto teamName) {
        this.teamName = teamName;
    }

    public List<ClientStatusHistoryDto> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<ClientStatusHistoryDto> statusHistory) {
        this.statusHistory = statusHistory;
    }


    @Override
    public String toString() {
        return "ClientDto{" +
                "id=" + id +
                ", clientId='" + clientId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", gpsLocation='" + gpsLocation + '\'' +
                ", createdBy=" + createdBy +
                ", teamLeadId='" + teamLeadId + '\'' +
                ", createdDate=" + createdDate +
                ", lastUpdated=" + lastUpdated +
                ", clientStatus='" + clientStatus + '\'' +
                ", assignedTo=" + assignedTo +
                ", teamName=" + teamName +
                ", statusHistory=" + statusHistory +
                '}';
    }
}
