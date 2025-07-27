package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.*;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    @Autowired
    Utils utils;


    @PreAuthorize("hasAnyAuthority('ROLE_TEAM_LEAD', 'ROLE_TEAM_MEMBER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientRest> createClient(
            @RequestBody ClientDetails clientDetails, @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Getting current logged in user
        String loggedInEmail = userPrincipal.getUsername();
        UserDto creatorUser = userService.getUserByEmail(loggedInEmail);

        // Setting creator in the DTO
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        clientDto.setCreatedBy(creatorUser);

        String teamLeadId = creatorUser.getTeamLeadUserId() != null
                ? creatorUser.getTeamLeadUserId()  // Team Member's lead
                : creatorUser.getUserId();         // Team Lead (self)wh
        clientDto.setTeamLeadId(teamLeadId);

        // Saving the client
        ClientDto createdClient = clientService.createClient(clientDto);
        ClientRest clientRest = modelMapper.map(createdClient, ClientRest.class);


        if (createdClient.getCreatedBy() != null) {
            UserDto creator = createdClient.getCreatedBy();
            clientRest.setCreatedBy(creator.getFirstName() + " " + creator.getLastName());
        }

        if (createdClient.getCreatedDate() != null) {
            clientRest.setCreatedAt(createdClient.getCreatedDate()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        if (createdClient.getLastUpdated() != null) {
            clientRest.setLastUpdated(createdClient.getLastUpdated()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        if (createdClient.getAssignedTo() != null) {
            UserDto teamLead = createdClient.getAssignedTo();
            clientRest.setAssignedTo(teamLead.getFirstName() + " " + teamLead.getLastName());
        }

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
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
//    @GetMapping("/admin/notifications")
//    public List<NotificationEntity> getAllUnresolvedNotifications() {
//        return notificationService.getUnresolvedNotifications();
//
//        //from the figma table, the above notifications should be seen by the admin where we get all clients in the system
//
//    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @GetMapping("/admin/notifications")
    public ResponseEntity<List<ClientRest>> getOverdueClients() {
        List<ClientDto> overdueClients = clientService.getOverdueClients();

        List<ClientRest> result = overdueClients.stream().map(dto -> {
            ClientRest rest = modelMapper.map(dto, ClientRest.class);

            rest.setClientId(dto.getClientId());
            rest.setFirstName(dto.getFirstName());
            rest.setLastName(dto.getLastName());
            rest.setPhoneNumber(dto.getPhoneNumber());
            rest.setClientStatus(dto.getClientStatus());

            if (dto.getCreatedDate() != null) {
                rest.setCreatedAt(dto.getCreatedDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            if (dto.getLastUpdated() != null) {
                rest.setLastUpdated(dto.getLastUpdated().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());

                Instant lastUpdatedInstant = dto.getLastUpdated().toInstant();
                Duration duration = Duration.between(lastUpdatedInstant, Instant.now());

                rest.setLastAction(utils.getExactDuration(duration));
            }

            if (dto.getCreatedBy() != null) {
                rest.setCreatedBy(dto.getCreatedBy().getFirstName() + " " + dto.getCreatedBy().getLastName());
            }

            if (dto.getAssignedTo() != null) {
                rest.setAssignedTo(dto.getAssignedTo().getFirstName() + " " + dto.getAssignedTo().getLastName());
            }

            return rest;
        }).toList();

        return ResponseEntity.ok(result);
    }



    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/notifications/{id}/resolve")
    public ResponseEntity<String> resolveNotification(@PathVariable Long id) {
        notificationService.resolveNotification(id);
        return ResponseEntity.ok("Notification resolved");
    }


    //Get a client
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @GetMapping(path = "/{id}")

    public ClientRest getClient(@PathVariable String id) {
        ClientRest returnClient = new ClientRest();

        ClientDto clientDto = clientService.getClientByClientId(id);
        BeanUtils.copyProperties(clientDto, returnClient);

        return returnClient;
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

    //Viewing all clients
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all-clients")
    public ResponseEntity<List<ClientRest>> getAllClients(@RequestParam(value = "page", defaultValue = "0")
                                 int page, @RequestParam(value = "limit", defaultValue = "10") int limit) {

        List<ClientRest> clientRest = new ArrayList<>();
        List<ClientDto> allClients = clientService.getAllClients(page, limit);

        List<ClientRest> result = null;
        for (ClientDto clientDto : allClients) {

            result = allClients.stream().map(dto -> {
                ClientRest rest = modelMapper.map(dto, ClientRest.class);

                rest.setClientId(dto.getClientId());
                rest.setFirstName(dto.getFirstName());
                rest.setLastName(dto.getLastName());
                rest.setPhoneNumber(dto.getPhoneNumber());
                rest.setClientStatus(dto.getClientStatus());

                // Converting Date to LocalDateTime
                if (dto.getCreatedDate() != null) {
                    rest.setCreatedAt(dto.getCreatedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
                }

                if (dto.getLastUpdated() != null) {
                    rest.setLastUpdated(dto.getLastUpdated().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());

                    // Calculating days since last update
                    // Converting Date to Instant
                    Instant lastUpdatedInstant = dto.getLastUpdated().toInstant();
                    Duration duration = Duration.between(lastUpdatedInstant, Instant.now());

                    String exactDuration = utils.getExactDuration(duration);
                    rest.setLastAction(exactDuration);

                }

                // Getting creator
                if (dto.getCreatedBy() != null) {
                    UserDto creator = dto.getCreatedBy();
                    rest.setCreatedBy(creator.getFirstName() + " " + creator.getLastName());
                }

                if(dto.getAssignedTo() !=null) {
                    UserDto teamLead = dto.getAssignedTo();
                    rest.setAssignedTo(teamLead.getFirstName() + " " + teamLead.getLastName());
                }

                return rest;
            }).toList();
        }
        return ResponseEntity.ok(result);
    }


    //getting all clients under a user
    @GetMapping("/all-clients/{userId}")
    public ResponseEntity<List<ClientRest>> getMyClients(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        String email = userPrincipal.getUsername();
        UserDto currentUser = userService.getUserByEmail(email);

        List<ClientDto> clientDtos = clientService.getClientsUnderUser(currentUser.getUserId(), page, limit);

        List<ClientRest> results = clientDtos.stream().map(dto -> {
            ClientRest rest = modelMapper.map(dto, ClientRest.class);

            rest.setClientId(dto.getClientId());
            rest.setFirstName(dto.getFirstName());
            rest.setLastName(dto.getLastName());
            rest.setPhoneNumber(dto.getPhoneNumber());
            rest.setClientStatus(dto.getClientStatus());

            if (dto.getCreatedDate() != null) {
                rest.setCreatedAt(dto.getCreatedDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            if (dto.getLastUpdated() != null) {
                rest.setLastUpdated(dto.getLastUpdated().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());

                Instant lastUpdatedInstant = dto.getLastUpdated().toInstant();
                Duration duration = Duration.between(lastUpdatedInstant, Instant.now());

                String humanReadable = utils.getExactDuration(duration);
                rest.setLastAction(humanReadable);
            }

            if (dto.getCreatedBy() != null) {
                rest.setCreatedBy(dto.getCreatedBy().getFirstName() + " " + dto.getCreatedBy().getLastName());
            }

            if (dto.getAssignedTo() != null) {
                rest.setAssignedTo(dto.getAssignedTo().getFirstName() + " " + dto.getAssignedTo().getLastName());
            }

            return rest;
        }).toList();

        return ResponseEntity.ok(results);
    }


}

