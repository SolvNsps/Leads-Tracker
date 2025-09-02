package com.leadstracker.leadstracker.entities;

import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.response.Statuses;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_status_history")
public class ClientStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    // Make the old single 'status' column nullable
    @Column(nullable = false)
    private String status;

    private Statuses oldStatus;
    private Statuses newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private UserEntity changedBy; // optional: who made the change


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public Statuses getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Statuses oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Statuses getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Statuses newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public UserEntity getChangedBy() {
        return changedBy;
    }

    @Override
    public String toString() {
        return "ClientStatusHistoryEntity{" +
                "id=" + id +
                ", client=" + client +
                ", status='" + status + '\'' +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", changedAt=" + changedAt +
                ", changedBy=" + changedBy +
                '}';
    }

    public void setChangedBy(UserEntity changedBy) {
        this.changedBy = changedBy;
    }
}
