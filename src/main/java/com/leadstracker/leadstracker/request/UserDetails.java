package com.leadstracker.leadstracker.request;

public class UserDetails {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNumber;
    private String staffId;
    private String teamLeadUserId;
    public String role;

    public UserDetails() {}

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getTeamLeadUserId() {
        return teamLeadUserId;
    }

    public void setTeamLeadUserId(String teamLeadUserId) {
        this.teamLeadUserId = teamLeadUserId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
