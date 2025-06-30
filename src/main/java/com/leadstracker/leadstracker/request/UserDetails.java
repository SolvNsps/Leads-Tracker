package com.leadstracker.leadstracker.request;

public class UserDetails {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNumber;
    private String staffIdNumber;
    private String teamId;
    public String role;    // “TEAM_LEAD” or “TEAM_MEMBER”

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

    public String getStaffIdNumber() {
        return staffIdNumber;
    }

    public void setStaffIdNumber(String staffIdNumber) {
        this.staffIdNumber = staffIdNumber;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
