package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.UserRest;
import com.leadstracker.leadstracker.security.AppConfig;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("api/v1/clients")
public class ClientController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    UserService userService;

    @Autowired
    ClientService clientService;

    @Autowired
    NotificationService notificationService;



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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @GetMapping("/admin/notifications")
    public List<NotificationEntity> getAllUnresolvedNotifications() {
        return notificationService.getUnresolvedNotifications();

        //from the figma table, the above notifications should be seen by the admin where we get all clients in the system

    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/notifications/{id}/resolve")
    public ResponseEntity<String> resolveNotification(@PathVariable Long id) {
        notificationService.resolveNotification(id);
        return ResponseEntity.ok("Notification resolved");
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/notifications/{id}/alert")
    public ResponseEntity<String> alertTeamLead(@PathVariable Long id) {
        notificationService.alertTeamLead(id);
        return ResponseEntity.ok("Team Lead alerted successfully.");
    }

    @PreAuthorize("hasRole('TEAM_LEAD')")
    @GetMapping("/notifications/team-lead")
    public List<NotificationEntity> getTeamLeadNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String teamLeadId = userService.getUserByEmail(userPrincipal.getUsername()).getUserId();
        return notificationService.getNotificationsForTeamLead(teamLeadId);
    }



    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<?> updateClient(@PathVariable String id, @RequestBody ClientDetails clientDetails) throws Exception {
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        ClientDto updatedClient = clientService.updateClient(id, clientDto);

        ClientRest clientRest = modelMapper.map(updatedClient, ClientRest.class);
        return ResponseEntity.ok(Map.of(
                "user", clientRest,
                "status", "SUCCESS",
                "message", "Client status updated successfully"));
    }

    //Viewing all team leads
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all-clients")
    public ResponseEntity<List<ClientRest>> getAllClients() {
        List<ClientDto> allClients = clientService.getAllClients();

        List<ClientRest> result = allClients.stream()
                .map(user -> modelMapper.map(user, ClientRest.class)).toList();

        return ResponseEntity.ok(result);
    }
}


