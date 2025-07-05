package com.leadstracker.leadstracker.DTO;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;

    private String userId;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private String emailVerificationToken;

    private boolean emailVerificationStatus;

    private String role;

    @JsonIgnore
    private transient RoleEntity roleEntity;

//    default to true for new users
    private boolean defaultPassword;

    private String otp;

    private Date otpExpiryDate;

    private Integer otpFailedAttempts;

    private Date tempBlockTime;

    private Boolean accountLocked;

    private Integer resendOtpAttempts;

    private LocalDateTime lastOtpResendTime;

    private String phoneNumber;

    private String staffId;

    private String teamLeadUserId;

    private List<String> teamMemberIds = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public boolean isEmailVerificationStatus() {
        return emailVerificationStatus;
    }

    public void setEmailVerificationStatus(boolean emailVerificationStatus) {
        this.emailVerificationStatus = emailVerificationStatus;
    }

    public boolean isDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(boolean defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public Date getOtpExpiryDate() {
        return otpExpiryDate;
    }

    public void setOtpExpiryDate(Date otpExpiryDate) {
        this.otpExpiryDate = otpExpiryDate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setOtpFailedAttempts(Integer otpFailedAttempts) {
        this.otpFailedAttempts = otpFailedAttempts;
    }

    public Integer getOtpFailedAttempts() {
        return otpFailedAttempts;
    }

    public Date getTempBlockTime() {
        return tempBlockTime;
    }

    public void setTempBlockTime(Date tempBlockTime) {
        this.tempBlockTime = tempBlockTime;
    }

    public Boolean getAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(Boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public Integer getResendOtpAttempts() {
        return resendOtpAttempts;
    }

    public void setResendOtpAttempts(Integer resendOtpAttempts) {
        this.resendOtpAttempts = resendOtpAttempts;
    }

    public LocalDateTime getLastOtpResendTime() {
        return lastOtpResendTime;
    }

    public void setLastOtpResendTime(LocalDateTime lastOtpResendTime) {
        this.lastOtpResendTime = lastOtpResendTime;
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

    public List<String> getTeamMemberIds() {
        return teamMemberIds;
    }

    public void setTeamMemberIds(List<String> teamMemberIds) {
        this.teamMemberIds = teamMemberIds;
    }

    // Internal use only
    public RoleEntity getRoleEntity() {
        return roleEntity;
    }

    public void setRoleEntity(RoleEntity roleEntity) {
        this.roleEntity = roleEntity;
        if (roleEntity != null) {
            this.role = roleEntity.getName();
        }
    }


    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", emailVerificationToken='" + emailVerificationToken + '\'' +
                ", emailVerificationStatus=" + emailVerificationStatus +
                ", role='" + role + '\'' +
                ", defaultPassword=" + defaultPassword +
                ", otp='" + otp + '\'' +
                ", otpExpiryDate=" + otpExpiryDate +
                ", otpFailedAttempts=" + otpFailedAttempts +
                ", phoneNumber=" + phoneNumber +
                '}';
    }

}
