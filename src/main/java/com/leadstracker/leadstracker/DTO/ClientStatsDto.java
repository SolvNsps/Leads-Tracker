package com.leadstracker.leadstracker.DTO;

import java.util.Map;

public class ClientStatsDto {
    private String teamName;
    private long totalClients;
    private Map<String, Long> statusCounts;

    public ClientStatsDto() {}

    public ClientStatsDto(String teamName, long totalClients, Map<String, Long> statusCounts) {
        this.teamName = teamName;
        this.totalClients = totalClients;
        this.statusCounts = statusCounts;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
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
}
