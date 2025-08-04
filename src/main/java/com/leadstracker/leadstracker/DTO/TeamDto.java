package com.leadstracker.leadstracker.DTO;

import java.io.Serial;
import java.io.Serializable;

public class TeamDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String teamLeadName;

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

    public String getTeamLeadName() {
        return teamLeadName;
    }

    public void setTeamLeadUserId(String teamLeadName) {
        this.teamLeadName = teamLeadName;
    }
}
