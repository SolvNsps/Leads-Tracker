package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@RestController
@RequestMapping("api/v1/clients")
public class ClientController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    UserService userService;

    @Autowired
    ClientService clientService;

    @PreAuthorize("hasAnyAuthority('ROLE_TEAM_LEAD', 'ROLE_TEAM_MEMBER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientRest> createClient(
            @RequestBody ClientDetails clientDetails, @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Getting current logged in user
        String loggedInEmail = userPrincipal.getUsername();
        UserDto creatorUser = userService.getUserByEmail(loggedInEmail);

        // Setting creator in the DTO
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        clientDto.setCreatedByUserId(creatorUser.getUserId());

        String teamLeadId = creatorUser.getTeamLeadUserId() != null
                ? creatorUser.getTeamLeadUserId()  // Team Member's lead
                : creatorUser.getUserId();         // Team Lead (self)
        clientDto.setTeamLeadId(teamLeadId);

        // Saving the client
        ClientDto createdClient = clientService.createClient(clientDto);
        ClientRest clientRest = modelMapper.map(createdClient, ClientRest.class);

        return ResponseEntity.ok(clientRest);
    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @GetMapping("/team-performance")
        public ResponseEntity<TeamPerformanceDto> getTeamOverview(String userId,
                @RequestParam(defaultValue = "week") String duration
        ) {
            return ResponseEntity.ok(clientService.getTeamPerformance(userId, duration));
        }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable String id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok("Client deleted successfully.");
    }

    }


