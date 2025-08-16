package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.response.UserRest;
import com.leadstracker.leadstracker.security.AppConfig;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.TeamService;
import com.leadstracker.leadstracker.services.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
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
    public ResponseEntity<ClientRest> createClient(@Valid
            @RequestBody ClientDetails clientDetails, @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Getting current logged in user
        String loggedInEmail = userPrincipal.getUsername();
        UserDto creatorUser = userService.getUserByEmail(loggedInEmail);

        System.out.println("Received clientStatus: " + clientDetails.getClientStatus());

        // Setting creator in the DTO
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        clientDto.setCreatedBy(creatorUser);

        String teamLeadId = creatorUser.getTeamLeadUserId() != null
                ? creatorUser.getTeamLeadUserId()  // Team Member's lead
                : creatorUser.getUserId();         // Team Lead (self)
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



    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD')")
    @GetMapping("/admin/overdueClients")
    public ResponseEntity<PaginatedResponse<ClientRest>> getOverdueClients(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        Page<ClientDto> overdueClients = clientService.getOverdueClients(page, limit);

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

            if (dto.getCreatedBy() != null && dto.getCreatedBy().getTeamName() != null) {
                rest.setTeamName(dto.getCreatedBy().getTeamName());
            }

            return rest;
        }).toList();

        PaginatedResponse<ClientRest> res = new PaginatedResponse<>();
        res.setData(result);
        res.setCurrentPage(overdueClients.getNumber());
        res.setTotalPages(overdueClients.getTotalPages());
        res.setTotalItems(overdueClients.getTotalElements());
        res.setPageSize(overdueClients.getSize());
        res.setHasNext(overdueClients.hasNext());
        res.setHasPrevious(overdueClients.hasPrevious());

        return ResponseEntity.ok(res);
    }



    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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


    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/notifications/{id}/alert")
    public ResponseEntity<?> alertTeamLead(@PathVariable Long id) {
        notificationService.alertTeamLead(id);
        return ResponseEntity.ok(Map.of("message", "Team Lead alerted successfully."));
    }


    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<?> updateClient(@PathVariable String id, @RequestBody ClientDetails clientDetails) throws Exception {
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        ClientDto updatedClient = clientService.updateClient(id, clientDto);

        ClientRest clientRest = modelMapper.map(updatedClient, ClientRest.class);
        if (updatedClient.getCreatedBy() != null) {
            String fullName = updatedClient.getCreatedBy().getFirstName() + " " + updatedClient.getCreatedBy().getLastName();
            clientRest.setCreatedBy(fullName);
        }

        if (updatedClient.getAssignedTo() != null) {
            UserDto teamLead = updatedClient.getAssignedTo();
            clientRest.setAssignedTo(teamLead.getFirstName() + " " + teamLead.getLastName());
        }

        if (updatedClient.getCreatedDate() != null) {
            clientRest.setCreatedAt(updatedClient.getCreatedDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        if (updatedClient.getLastUpdated() != null) {
            clientRest.setLastUpdated(updatedClient.getLastUpdated().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        return ResponseEntity.ok(Map.of(
                "user", clientRest,
                "status", "SUCCESS",
                "message", "Client status updated successfully"));
    }

    //Viewing all clients
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all-clients")
    public ResponseEntity<PaginatedResponse<ClientRest>> getAllClients(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        Page<ClientDto> pagedClients = clientService.getAllClients(page, limit);

        List<ClientRest> clientRestList = pagedClients.getContent().stream().map(dto -> {
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

                Duration duration = Duration.between(dto.getLastUpdated().toInstant(), Instant.now());
                rest.setLastAction(utils.getExactDuration(duration));
            }

            if (dto.getCreatedBy() != null) {
                UserDto creator = dto.getCreatedBy();
                rest.setCreatedBy(creator.getFirstName() + " " + creator.getLastName());
            }

            if (dto.getAssignedTo() != null) {
                UserDto teamLead = dto.getAssignedTo();
                rest.setAssignedTo(teamLead.getFirstName() + " " + teamLead.getLastName());
            }

            if (dto.getCreatedBy() != null && dto.getCreatedBy().getTeamName() != null) {
                rest.setTeamName(dto.getCreatedBy().getTeamName());
            }

            return rest;
        }).toList();

        PaginatedResponse<ClientRest> response = new PaginatedResponse<>();
        response.setData(clientRestList);
        response.setCurrentPage(pagedClients.getNumber());
        response.setTotalPages(pagedClients.getTotalPages());
        response.setTotalItems(pagedClients.getTotalElements());
        response.setPageSize(pagedClients.getSize());
        response.setHasNext(pagedClients.hasNext());
        response.setHasPrevious(pagedClients.hasPrevious());

        return ResponseEntity.ok(response);
    }


    //getting all clients under a user
    @GetMapping("/all-clients/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD', 'ROLE_TEAM_MEMBER')")
    public ResponseEntity<PaginatedResponse<ClientRest>> getClients(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String name, @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal authentication) {

        String loggedInUserId = authentication.getId();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        Statuses statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            statusEnum = Statuses.fromString(status);
        }

        PaginatedResponse<ClientRest> clients = clientService.getMyClientsForUserRole(
                        loggedInUserId, role, userId, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate")), name, statusEnum,
                fromDate, endDate);

        return ResponseEntity.ok(clients);
    }


//getting all clients under a team member
    @GetMapping("/team-member/{memberId}/clients")
    public ResponseEntity<PaginatedResponse<ClientRest>> getClientsOfTeamMember(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String memberId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {

        boolean isPaginated = (page != null && page >= 0) && (limit != null && limit > 0);
        int pageNumber = isPaginated ? page : 0;
        int pageSize = isPaginated ? limit : Integer.MAX_VALUE;

        List<ClientDto> clientDtos;
        long totalItems;
        int totalPages;

        if (isPaginated) {
            clientDtos = clientService.getClientsByTeamMember(memberId, pageNumber, pageSize);
            totalItems = clientService.countClientsByTeamMember(memberId);
            totalPages = (int) Math.ceil((double) totalItems / pageSize);
        } else {
            clientDtos = clientService.getAllClientsByTeamMember(memberId);
            totalItems = clientDtos.size();
            totalPages = 1;
            pageSize = clientDtos.size();
            pageNumber = 0;
        }

        List<ClientRest> results = clientDtos.stream().map(dto -> {
            ClientRest rest = modelMapper.map(dto, ClientRest.class);
            rest.setClientId(dto.getClientId());
            rest.setFirstName(dto.getFirstName());
            rest.setLastName(dto.getLastName());
            rest.setPhoneNumber(dto.getPhoneNumber());
            rest.setClientStatus(dto.getClientStatus());
            rest.setGpsLocation(dto.getGpsLocation());

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

        PaginatedResponse<ClientRest> response = new PaginatedResponse<>();
        response.setData(results);
        response.setCurrentPage(pageNumber);
        response.setPageSize(pageSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        response.setHasNext(isPaginated && (pageNumber + 1 < totalPages));
        response.setHasPrevious(isPaginated && (pageNumber > 0));

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/statistics")
    public ResponseEntity<OverallSystemDto> getClientStats(@RequestParam(defaultValue = "week") String duration) {
        OverallSystemDto stats = clientService.getClientStats(duration);
        return ResponseEntity.ok(stats);
    }

    //search for clients
    @GetMapping("/search-client")
    public ResponseEntity<List<ClientSearchDto>> searchClients(@RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        {
            Statuses statusEnum = null;
            if (status != null && !status.trim().isEmpty()) {
                statusEnum = Statuses.fromString(status);
            }
            return ResponseEntity.ok(clientService.searchClients(name, statusEnum, date));
        }
    }

    //getting overdue clients under a user
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEAM_LEAD', 'ROLE_TEAM_MEMBER')")
    @GetMapping("/user/{userId}/overdueClients")
    public ResponseEntity<PaginatedResponse<ClientRest>> getOverdueClients(@PathVariable String userId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) String name, @RequestParam(required = false) String status,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @AuthenticationPrincipal UserPrincipal authentication) {

    Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
    String loggedInUserId =  authentication.getId();
    String role = authentication.getAuthorities().iterator().next().getAuthority();

        Statuses statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            statusEnum = Statuses.fromString(status);
        }

    PaginatedResponse<ClientRest> overdueClients = clientService.getOverdueClientsForUserRole(
            loggedInUserId, role, userId, pageable, name, statusEnum, fromDate, toDate);

    return ResponseEntity.ok(overdueClients);
   }


    //my statistics
    @GetMapping("/my-statistics")
    public ResponseEntity<?> getMyStats(@RequestParam(defaultValue = "week") String duration) {
        return ResponseEntity.ok(clientService.getClientStatsForLoggedInUser(duration));
    }


    //deactivating client
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN, ROLE_TEAM_LEAD')")
    @PatchMapping("/{clientId}/deactivate")
    public ResponseEntity<?> deactivateClient(@PathVariable String clientId) {
        clientService.deactivateClient(clientId);
        return ResponseEntity.ok(Map.of(
                "message", "Client successfully deactivated"
        ));
    }

}

