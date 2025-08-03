package com.leadstracker.leadstracker.DTO;

import java.io.Serial;
import java.io.Serializable;

public class TeamDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String teamLeadUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamLeadUserId() {
        return teamLeadUserId;
    }

    public void setTeamLeadUserId(String teamLeadUserId) {
        this.teamLeadUserId = teamLeadUserId;
    }
}
