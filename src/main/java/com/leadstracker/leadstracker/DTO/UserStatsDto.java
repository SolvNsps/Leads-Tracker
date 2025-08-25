package com.leadstracker.leadstracker.DTO;

import java.util.Map;

public class UserStatsDto {
    private String userId;
    private String fullName;
//    private String progress;
    private long totalClients;
    private Map<String, Long> statusCounts;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getTotalClients() {
        return totalClients;
    }

    public void setTotalClients(long totalClients) {
        this.totalClients = totalClients;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public UserStatsDto(String userId, String fullName, long totalClients, Map<String, Long> statusCounts) {
        this.userId = userId;
        this.fullName = fullName;
        this.totalClients = totalClients;
        this.statusCounts = statusCounts;
    }
}
