package com.leadstracker.leadstracker.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_targets")
public class UserTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link for user (team member) who receives this target
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Link for team target this is part of
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_target_id", nullable = false)
    private TeamTargetEntity teamTarget;

    @Column(nullable = false)
    private int targetValue;

    @Column(nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private int progress;

    public UserTargetEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public TeamTargetEntity getTeamTarget() {
        return teamTarget;
    }

    public void setTeamTarget(TeamTargetEntity teamTarget) {
        this.teamTarget = teamTarget;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = LocalDate.now();
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @PrePersist
    public void prePersist() {
        if (assignedDate == null) {
            assignedDate = LocalDate.now();
        }
    }
}
