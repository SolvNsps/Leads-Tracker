package com.leadstracker.leadstracker.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ClientDetails {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gpsLocation;

    @NotBlank(message = "Client status is required.")
    @JsonProperty("clientStatus")
    private String clientStatus;

    public ClientDetails() {
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

    public String getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(String status) {
        this.clientStatus = status;
    }
}
