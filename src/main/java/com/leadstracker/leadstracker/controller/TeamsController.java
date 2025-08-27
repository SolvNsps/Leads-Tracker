package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamsController {

    @Autowired
    private TeamService teamService;
    @Autowired
    private ClientService clientService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all-teams")
    public ResponseEntity<List<TeamPerformanceDto>> getTeamsOverview(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ) {
        return ResponseEntity.ok(teamService.getTeamsOverview(name, startDate, endDate));
    }


    // Getting the list of unassigned members (team members without a team)
    @GetMapping("/unassigned-members")
    public ResponseEntity<List<UserDto>> getUnassignedMembers() {
        return ResponseEntity.ok(teamService.getUnassignedMembers());
    }

}
