package com.leadstracker.leadstracker.response;


import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;

public class UserRest {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phoneNumber;
    private String staffId;
    private String team;
//    private String teamName; // Optional -  if user is a Team Member
//    private TeamPerformanceDto teamPerformance;
//    private TeamMemberPerformanceDto memberPerformance;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
