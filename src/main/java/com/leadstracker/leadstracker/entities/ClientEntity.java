package com.leadstracker.leadstracker.entities;

import com.leadstracker.leadstracker.response.Statuses;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "clients")
@Table(name = "clients")
public class ClientEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable=false)
    private String firstName;

    @Column(nullable=false)
    private String lastName;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable=false)
    private String GPSLocation;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne
    @JoinColumn(name = "team_lead_id", nullable = false)
    private UserEntity teamLead; // Always points to the lead responsible for the client


    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    @Enumerated(EnumType.STRING)
    private Statuses clientStatus = Statuses.PENDING;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getGPSLocation() {
        return GPSLocation;
    }

    public void setGPSLocation(String GPSLocation) {
        this.GPSLocation = GPSLocation;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Statuses getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(Statuses clientStatus) {
        this.clientStatus = clientStatus;
    }

    public UserEntity getTeamLead() {
        return teamLead;
    }

    public void setTeamLead(UserEntity teamLead) {
        this.teamLead = teamLead;
    }

    @PrePersist
    protected void onCreate() {
        createdDate = new Date();
        lastUpdated = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = new Date();
    }

    @Override
    public String toString() {
        return "ClientEntity{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", GPSLocation='" + GPSLocation + '\'' +
                ", createdBy=" + createdBy +
                ", createdDate=" + createdDate +
                ", lastUpdated=" + lastUpdated +
                ", clientStatus=" + clientStatus +
                '}';
    }
}
