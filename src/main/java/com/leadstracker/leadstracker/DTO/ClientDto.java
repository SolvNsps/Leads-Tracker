package com.leadstracker.leadstracker.DTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class ClientDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String GPSLocation;
    private String createdByUserId;
    private String teamLeadId;
    private Date createdDate;
    private Date lastUpdated;
    private String clientStatus;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getGPSLocation() {
        return GPSLocation;
    }

    public void setGPSLocation(String GPSLocation) {
        this.GPSLocation = GPSLocation;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
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

    @Override
    public String toString() {
        return "ClientDto{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", GPSLocation='" + GPSLocation + '\'' +
                ", createdByUserId=" + createdByUserId +
                ", createdDate=" + createdDate +
                ", lastUpdated=" + lastUpdated +
                ", clientStatus=" + clientStatus +
                '}';
    }
}
