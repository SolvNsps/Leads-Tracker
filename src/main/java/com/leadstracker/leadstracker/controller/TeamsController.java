package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamsController {

    @Autowired
    private TeamService teamService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all-teams")
    public ResponseEntity<List<TeamPerformanceDto>> getTeamsOverview(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "week") String duration
    ) {
        return ResponseEntity.ok(teamService.getTeamsOverview(search, duration));
    }


    // Get list of unassigned members (team members without a team)
    @GetMapping("/unassigned-members")
    public ResponseEntity<List<UserDto>> getUnassignedMembers() {
        return ResponseEntity.ok(teamService.getUnassignedMembers());
    }

}
