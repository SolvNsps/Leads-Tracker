package com.leadstracker.leadstracker.DTO;

import java.util.List;
import java.util.Map;

public class OverallSystemDto {
    private long totalClients;
    private Map<String, Long> overallStatusCounts;
    private List<ClientStatsDto> teamStats;

    public OverallSystemDto(long totalClients, Map<String, Long> overallStatusCounts, List<ClientStatsDto> teamStats) {
        this.totalClients = totalClients;
        this.overallStatusCounts = overallStatusCounts;
        this.teamStats = teamStats;
    }

    public OverallSystemDto() {
    }

    public long getTotalClients() {
        return totalClients;
    }

    public void setTotalClients(long totalClients) {
        this.totalClients = totalClients;
    }

    public Map<String, Long> getOverallStatusCounts() {
        return overallStatusCounts;
    }

    public void setOverallStatusCounts(Map<String, Long> overallStatusCounts) {
        this.overallStatusCounts = overallStatusCounts;
    }

    public List<ClientStatsDto> getTeamStats() {
        return teamStats;
    }

    public void setTeamStats(List<ClientStatsDto> teamStats) {
        this.teamStats = teamStats;
    }
}
