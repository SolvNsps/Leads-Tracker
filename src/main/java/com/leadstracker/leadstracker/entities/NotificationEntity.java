package com.leadstracker.leadstracker.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class NotificationEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String message;

    private boolean resolved = false;

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;

    private String type; // e.g. FORWARDED_CLIENT, OVERDUE_FOLLOWUP

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id")
    private UserEntity admin;

    @ManyToOne
    @JoinColumn(name = "team_lead_id")
    private UserEntity teamLead;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    private String actionRequired;
    private String forwardedBy; // Name of admin/team member who forwarded
    private long daysOverdue;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserEntity getAdmin() {
        return admin;
    }

    public void setAdmin(UserEntity admin) {
        this.admin = admin;
    }

    public UserEntity getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(UserEntity teamLead) {
        this.teamLead = teamLead;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getForwardedBy() {
        return forwardedBy;
    }

    public void setForwardedBy(String forwardedBy) {
        this.forwardedBy = forwardedBy;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }
}
