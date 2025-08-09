package com.leadstracker.leadstracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity(name = "users")
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))

public class UserEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 20)
    private String firstName;

    @Column(nullable = false, length = 20)
    private String lastName;

    @Column(nullable = false, length = 50, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @JsonIgnore
    private String emailVerificationToken;

    @JsonIgnore
    private String passwordResetToken;

    @JsonIgnore
    @Column(name = "password_reset_expiration")
    private Date passwordResetExpiration;

    @JsonIgnore
    @Column(nullable = false)
    private boolean emailVerificationStatus;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 10)
    private String staffId;

    @ManyToOne
    @JoinColumn(name = "team_lead_id")
    private UserEntity teamLead; // Only set if the user is a TEAM_MEMBER

    @OneToMany(mappedBy = "teamLead")
    private List<UserEntity> teamMembers = new ArrayList<>(); // Only used if the user is a TEAM_LEAD

    //    default to true for new users
    @JsonIgnore
    @Column(name = "default_password", nullable = false)
    private boolean defaultPassword = true;

    @JsonIgnore
    private String otp;

    @JsonIgnore
    private Date otpExpiryDate;

    @JsonIgnore
    @Column(name = "otp_failed_attempts")
    private Integer otpFailedAttempts = 0;

    @JsonIgnore
    @Column(name = "temp_block_time")
    private Date tempBlockTime;

    @JsonIgnore
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @JsonIgnore
    @Column(name = "resend_otp_attempts")
    private Integer resendOtpAttempts = 0;

    @JsonIgnore
    @Column(name = "last_otp_resend")
    private LocalDateTime lastOtpResendTime;

    @OneToMany(mappedBy = "createdBy")
    private List<ClientEntity> clients;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "users_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id", referencedColumnName = "id"))
    private RoleEntity role;

    @ManyToOne
    @JoinColumn(name = "teams_id")
    private TeamsEntity team;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @JsonIgnore
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    @JsonIgnore
    public Date getPasswordResetExpiration() {
        return passwordResetExpiration;
    }

    public void setPasswordResetExpiration(Date passwordResetExpiration) {
        this.passwordResetExpiration = passwordResetExpiration;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public boolean isEmailVerificationStatus() {
        return emailVerificationStatus;
    }

    public void setEmailVerificationStatus(boolean emailVerificationStatus) {
        this.emailVerificationStatus = emailVerificationStatus;
    }

    @JsonIgnore
    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @JsonIgnore
    public Date getOtpExpiryDate() {
        return otpExpiryDate;
    }

    public void setOtpExpiryDate(Date otpExpiryDate) {
        this.otpExpiryDate = otpExpiryDate;
    }

    public Integer getOtpFailedAttempts() {
        return otpFailedAttempts;
    }

    public void setOtpFailedAttempts(Integer otpFailedAttempts) {
        this.otpFailedAttempts = otpFailedAttempts;
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

    public UserEntity getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(UserEntity teamLead) {
        this.teamLead = teamLead;
    }

    public List<UserEntity> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<UserEntity> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public List<ClientEntity> getClients() {
        return clients;
    }

    public void setClients(List<ClientEntity> clients) {
        this.clients = clients;
    }

    public TeamsEntity getTeam() {
        return team;
    }

    public void setTeam(TeamsEntity team) {
        this.team = team;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}