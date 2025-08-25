package com.leadstracker.leadstracker.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String message;

    private boolean resolved = false;

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;

    private String type; // e.g. FORWARDED_CLIENT, OVERDUE_FOLLOWUP

    private SimpleUserDto admin;

    private SimpleUserDto teamLead;

    private SimpleClientDto client;

    private String actionRequired;

    private String forwardedBy;

    private long daysOverdue; // For overdue notifications

        // Generating a consistent message
        public String generateMessage() {
            String safeType = this.type != null ? this.type : "GENERAL";

            switch (safeType) {
                case "FORWARDED_CLIENT":
                    return String.format("Client %s %s was forwarded to you by %s",
                            client != null ? client.getFirstName() : "",
                            client != null ? client.getLastName() : "",
                            forwardedBy != null ? forwardedBy : "Unknown");

                case "OVERDUE_FOLLOWUP":
                    return String.format("Client %s %s is still in '%s' status for %d days",
                            client != null ? client.getFirstName() : "",
                            client != null ? client.getLastName() : "",
                            client != null ? client.getClientStatus() : "Unknown",
                            daysOverdue);

                default:
                    return message != null ? message : "No details available";
            }
        }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public SimpleUserDto getAdmin() {
        return admin;
    }

    public void setAdmin(SimpleUserDto admin) {
        this.admin = admin;
    }

    public SimpleUserDto getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(SimpleUserDto teamLead) {
        this.teamLead = teamLead;
    }

    public SimpleClientDto getClient() {
        return client;
    }

    public void setClient(SimpleClientDto client) {
        this.client = client;
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
