package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.*;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    Utils utils;

    @Autowired
    private AmazonSES amazonSES;

    @Autowired
    TeamTargetRepository teamTargetRepository;

    @Autowired
    UserTargetRepository userTargetRepository;

    @Autowired
    TeamsRepository teamsRepository;


    /**
     * @param clientDto
     * @return
     */
    @Override
    public ClientDto createClient(ClientDto clientDto) {
        String creatorUserId = clientDto.getCreatedBy().getUserId();

        // Fetching the user
        UserEntity creatorEntity = userRepository.findByUserId(creatorUserId);

        String creatorRole = creatorEntity.getRole().getName();

        if (!creatorRole.equals("ROLE_TEAM_LEAD") && !creatorRole.equals("ROLE_TEAM_MEMBER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to create client.");
        }

        if (!isInternetAvailable()) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to add client.");
        }

        ClientEntity clientEntity = modelMapper.map(clientDto, ClientEntity.class);
        clientEntity.setCreatedBy(creatorEntity);
        clientEntity.setClientId(utils.generateUserId(30));
        clientEntity.setFirstName(clientDto.getFirstName());
        clientEntity.setLastName(clientDto.getLastName());
        clientEntity.setGpsLocation(clientDto.getGpsLocation());
        clientEntity.setActive(true);
        Statuses statusEnum = null;
        try {
            statusEnum = Statuses.fromString(clientDto.getClientStatus());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client status: " + clientDto.getClientStatus());
        }
        clientEntity.setClientStatus(statusEnum);

        // Setting the team lead
        if (creatorRole.equals("ROLE_TEAM_LEAD")) {
            clientEntity.setTeamLead(creatorEntity); // Team Lead created the client
        } else {
            if (creatorEntity.getTeamLead() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team Member must be assigned to a Team Lead.");
            }

            clientEntity.setTeamLead(creatorEntity.getTeamLead()); // Team member created the client
        }
        System.out.println("gpsLocation being saved: '" + clientEntity.getGpsLocation() + "'");


        ClientEntity saved = clientRepository.save(clientEntity);

        // Mapping back to DTO
        ClientDto result = modelMapper.map(saved, ClientDto.class);

        UserDto assignedToDto = modelMapper.map(saved.getTeamLead(), UserDto.class);
        result.setAssignedTo(assignedToDto);

        return result;


//        return modelMapper.map(saved, ClientDto.class);
    }

    /**
     * @param duration
     * @return
     */
    public TeamPerformanceDto getTeamPerformance(String userId, String duration) {
        //Getting team lead and members
        UserEntity teamLead = userRepository.findByUserId(userId);
        TeamsEntity team = teamLead.getTeam();

        if (team == null) {
            TeamPerformanceDto emptyResponse = new TeamPerformanceDto();
            emptyResponse.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
            emptyResponse.setTeamId(null);
            emptyResponse.setEmail(teamLead.getEmail());
            emptyResponse.setTeamLeadUserId(teamLead.getUserId());
            emptyResponse.setTeamName("No Team Assigned");
            emptyResponse.setTotalClientsAdded(0);
            emptyResponse.setNumberOfTeamMembers(0);
            emptyResponse.setTeamTarget(0);
            emptyResponse.setProgressPercentage(0);
            emptyResponse.setProgressFraction("0/0");
//            emptyResponse.setClientStatus(Collections.emptyMap());
//            emptyResponse.setTeamMembers(Collections.emptyList());
            return emptyResponse;
        }

        //getting team members under a team lead
        List<UserEntity> teamMembers = userRepository.findByTeamLead(teamLead);
        // Adding team lead
        if (!teamMembers.contains(teamLead)) {
            teamMembers.add(teamLead);
        }

        //Calculating date range
        Date[] dateRange = calculateDateRange(duration);

        //Fetching all the clients of a team
        List<ClientEntity> clients = clientRepository.findByCreatedByInAndCreatedDateBetween(
                teamMembers, dateRange[0], dateRange[1]
        );

        //response
        TeamPerformanceDto response = new TeamPerformanceDto();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setTeamLeadUserId(teamLead.getUserId());
        response.setEmail(teamLead.getEmail());
        response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
        response.setTotalClientsAdded(clients.size());
//        response.setTeamTarget(response.getNumberOfClients());
//        response.setProgressPercentage(( (double) clients.size() / (response.getNumberOfClients())));
//        response.setNumberOfTeamMembers(teamMembers.size());
        // members only (excluding lead) for display
        response.setNumberOfTeamMembers((int) teamMembers.stream()
                .filter(ent -> !ent.getUserId().equals(teamLead.getUserId()))
                .count());

        //Fetching active target
        Optional<TeamTargetEntity> activeTargetOpt = teamTargetRepository
                .findTopByTeamIdAndDueDateGreaterThanEqualOrderByDueDateAsc(team.getId(), LocalDate.now());

        int teamTarget = activeTargetOpt.map(TeamTargetEntity::getTargetValue).orElse(0);
        response.setTeamTarget(teamTarget);

        // Setting Progress
        int numberOfClientsAdded = clients.size();
        double progress = 0;

        if (teamTarget > 0) {
            progress = Math.ceil(((double) numberOfClientsAdded / teamTarget) * 100);
        }
//      Setting both percentage and fraction
        response.setProgressPercentage(progress);
        response.setProgressFraction(numberOfClientsAdded + "/" + teamTarget);

        //Building the status distribution using the enum
        response.setClientStatus(
                clients.stream()
                        .collect(Collectors.groupingBy(
                                        ClientEntity::getClientStatus,
                                        Collectors.summingInt(c -> 1)
                                )
                        ));

        // Building team member stats
//        response.setTeamMembers(
//                teamMembers.stream()
//                        .map(member -> teamMemberStats(member, dateRange[0], dateRange[1]))
//                        .collect(Collectors.toList())
//        );

        // Member stats (excluding lead)
        response.setTeamMembers(
                teamMembers.stream()
                        .filter(m -> !m.getUserId().equals(teamLead.getUserId()))
                        .map(member -> teamMemberStats(member, dateRange[0], dateRange[1]))
                        .collect(Collectors.toList())
        );


        return response;
    }

    private Date[] calculateDateRange(String duration) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = duration.equals("month") ?
                endDate.minusMonths(1) : endDate.minusWeeks(1);

        return new Date[]{
                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
//                Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        };
    }


    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
        List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(
                member, start, end
        );

        // Fetching the most recently assigned target
        UserTargetEntity latestTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(member);
        int target = 0;
        if (latestTarget != null) {
            target = latestTarget.getTargetValue();
        }


        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setEmail(member.getEmail());
        dto.setTotalClientsSubmitted(memberClients.size());
        dto.setTarget(target);
        // Calculating progress
        double progressPercentage = 0;
        if (target > 0) {
            progressPercentage = Math.ceil((memberClients.size() * 100.0) / target);
        }

        dto.setProgressPercentage(progressPercentage);
        dto.setProgressFraction(memberClients.size() + "/" + target);
        //Adding Team Name
        if (member.getTeam() != null) {
            dto.setTeamName(member.getTeam().getName());
        } else {
            dto.setTeamName(null);
        }

        // Adding Team Lead Name
        if (member.getTeamLead() != null) {
            dto.setTeamLeadName(member.getTeamLead().getFirstName() + " " + member.getTeamLead().getLastName());
        } else {
            dto.setTeamLeadName(null);
        }

        // Grouping by status enum
        dto.setClientStatus(
                memberClients.stream()
                        .collect(Collectors.groupingBy(
                                        ClientEntity::getClientStatus,
                                        Collectors.summingInt(c -> 1)
                                )
                        ));

        return dto;
    }


    @Override
    public TeamMemberPerformanceDto getMemberPerformance(String memberId, String duration) {
        UserEntity member = userRepository.findByUserId(memberId);
        Date[] dateRange = calculateDateRange(duration);

        return teamMemberStats(member, dateRange[0], dateRange[1]);

    }

    /**
     * @param clientId
     */
    @Override
    public void deleteClient(String clientId) {
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);
        clientRepository.delete(clientEntity);
    }

    /**
     * @param clientId
     * @param clientDto
     * @return
     */

    @Override
    public ClientDto updateClient(String clientId, ClientDto clientDto) {
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);

        if (clientEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + clientId + " not found");
        }

        // Saving a copy of the old client for comparison
        ClientEntity oldClient = new ClientEntity();
        oldClient.setFirstName(clientEntity.getFirstName());
        oldClient.setLastName(clientEntity.getLastName());
        oldClient.setPhoneNumber(clientEntity.getPhoneNumber());
        oldClient.setClientStatus(clientEntity.getClientStatus());
        oldClient.setGpsLocation(clientEntity.getGpsLocation());

        if (!isInternetAvailable()) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update client.");
        }

        clientEntity.setFirstName(clientDto.getFirstName());
        clientEntity.setLastName(clientDto.getLastName());
//        clientEntity.setPhoneNumber(clientDto.getPhoneNumber());
        clientEntity.setGpsLocation(clientDto.getGpsLocation());
        clientEntity.setClientStatus(Statuses.fromString(clientDto.getClientStatus()));
        clientEntity.setGpsLocation(clientDto.getGpsLocation());
        clientEntity.setLastUpdated(Date.from(Instant.now()));

        ClientEntity updatedClient = clientRepository.save(clientEntity);

        // Fetching who updated the client
        String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity updatedBy = userRepository.findByEmail(loggedInUsername);

        // Fetching the team member (recipient of the email)
        UserEntity teamMember = clientEntity.getCreatedBy();

        amazonSES.sendClientUpdateNotificationEmail(teamMember, updatedClient, oldClient, updatedBy);

        return modelMapper.map(updatedClient, ClientDto.class);
    }


    public boolean isInternetAvailable() {
        try {
            // Trying to connect to a reliable server
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000); // setting 5 seconds timeout
            connection.connect();

            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * @return
     */
    @Override
    public Page<ClientDto> getAllClients(int page, int limit) {
        if (page > 0) {
            page -= 1;
        }

        Pageable pageableRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<ClientEntity> clientPage = clientRepository.findAll(pageableRequest);

        return clientPage.map(clientEntity -> {
            ClientDto dto = modelMapper.map(clientEntity, ClientDto.class);

            if (clientEntity.getCreatedBy() != null) {
                UserDto createdByDto = modelMapper.map(clientEntity.getCreatedBy(), UserDto.class);
                if (clientEntity.getCreatedBy().getTeam() != null) {
                    createdByDto.setTeamName(clientEntity.getCreatedBy().getTeam().getName());
                }
                dto.setCreatedBy(createdByDto);
            }

            if (clientEntity.getTeamLead() != null) {
                UserDto teamLeadDto = modelMapper.map(clientEntity.getTeamLead(), UserDto.class);
                dto.setAssignedTo(teamLeadDto);
            }

            dto.setClientStatus(clientEntity.getClientStatus().getDisplayName());

            return dto;
        });
    }


    /**
     * @param clientId
     * @return
     */
    @Override
    public ClientDto getClientByClientId(String clientId) {
        ClientDto returnClient = new ClientDto();
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);

        if (clientEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + clientId + "not found");
        }
        BeanUtils.copyProperties(clientEntity, returnClient);
        return returnClient;
    }


    /**
     * @param userId
     * @param page
     * @param limit
     * @return
     */
    @Override
    public List<ClientDto> getClientsUnderUser(String userId, int page, int limit) {
        if (page > 0) {
            page--;
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        UserEntity userEntity = userRepository.findByUserId(userId);

        List<ClientEntity> clients;

        if (userEntity.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Get self + team members
            List<UserEntity> teamMembers = userRepository.findByTeamLead(userEntity);
            teamMembers.add(userEntity); // Include the lead

            clients = clientRepository.findByCreatedByIn(teamMembers, pageable);
        } else {
            // Getting clients created by team member
            clients = clientRepository.findByCreatedBy(userEntity, pageable);
        }

        List<ClientDto> returnList = new ArrayList<>();
        for (ClientEntity entity : clients) {
            ClientDto dto = modelMapper.map(entity, ClientDto.class);

            if (entity.getCreatedBy() != null) {
                dto.setCreatedBy(modelMapper.map(entity.getCreatedBy(), UserDto.class));
            }

            if (entity.getTeamLead() != null) {
                dto.setAssignedTo(modelMapper.map(entity.getTeamLead(), UserDto.class));
            }

            dto.setClientStatus(entity.getClientStatus().getDisplayName());
            returnList.add(dto);
        }

        return returnList;
    }


    @Override
    public List<ClientDto> getAllClientsUnderUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        List<ClientEntity> clients;

        if (userEntity.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Fetching all clients for team lead (including team members)
            List<UserEntity> teamMembers = userRepository.findByTeamLead(userEntity);
            teamMembers.add(userEntity);

            clients = clientRepository.findByCreatedByIn(teamMembers);
        } else {
            // Fetching only clients created by this team member
            clients = clientRepository.findByCreatedBy(userEntity);
        }

        List<ClientDto> returnList = new ArrayList<>();
        for (ClientEntity entity : clients) {
            ClientDto dto = modelMapper.map(entity, ClientDto.class);

            if (entity.getCreatedBy() != null) {
                dto.setCreatedBy(modelMapper.map(entity.getCreatedBy(), UserDto.class));
            }

            if (entity.getTeamLead() != null) {
                dto.setAssignedTo(modelMapper.map(entity.getTeamLead(), UserDto.class));
            }

            dto.setClientStatus(entity.getClientStatus().getDisplayName());
            returnList.add(dto);
        }

        return returnList;
    }


    @Override
    public long countClientsUnderUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Count all clients under this lead
            List<UserEntity> teamMembers = userRepository.findByTeamLead(userEntity);
            teamMembers.add(userEntity);
            return clientRepository.countByCreatedByIn(teamMembers);
        } else {
            // Count only clients created by this member
            return clientRepository.countByCreatedBy(userEntity);
        }
    }

    /**
     * @param userId
     * @param page
     * @param limit
     * @return
     */
    @Override
    public List<ClientDto> getClientsByTeamMember(String userId, int page, int limit) {
        if (page > 0) page--;
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));

        UserEntity member = userRepository.findByUserId(userId);
        List<ClientEntity> clients = clientRepository.findByCreatedBy(member, pageable);

        return clients.stream().map(client -> {
            ClientDto dto = modelMapper.map(client, ClientDto.class);
            if (client.getCreatedBy() != null) {
                dto.setCreatedBy(modelMapper.map(client.getCreatedBy(), UserDto.class));
            }
            if (client.getTeamLead() != null) {
                dto.setAssignedTo(modelMapper.map(client.getTeamLead(), UserDto.class));
            }
            dto.setClientStatus(client.getClientStatus().getDisplayName());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public long countClientsByTeamMember(String userId) {
        UserEntity member = userRepository.findByUserId(userId);
        return clientRepository.countByCreatedBy(member);
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public List<ClientDto> getAllClientsByTeamMember(String userId) {
        UserEntity member = userRepository.findByUserId(userId);
        List<ClientEntity> clients = clientRepository.findByCreatedBy(member);

        return clients.stream().map(client -> {
            ClientDto dto = modelMapper.map(client, ClientDto.class);
            if (client.getCreatedBy() != null) {
                dto.setCreatedBy(modelMapper.map(client.getCreatedBy(), UserDto.class));
            }
            if (client.getTeamLead() != null) {
                dto.setAssignedTo(modelMapper.map(client.getTeamLead(), UserDto.class));
            }
            dto.setClientStatus(client.getClientStatus().getDisplayName());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * @return
     */
    @Override
    public OverallSystemDto getClientStats(String duration) {
        // Getting total statistics in the system
        long totalClients = clientRepository.count();
        List<ClientStatusCountDto> overallStats = clientRepository.countClientsByStatus();

        Map<String, Long> overallStatusCounts = new HashMap<>();
        for (ClientStatusCountDto stat : overallStats) {
            overallStatusCounts.put(stat.getStatus().toString(), stat.getCount());
        }

        // Calculating date range based on duration
        Date[] dateRange = calculateDateRange(duration);
        Date startDate = dateRange[0];
        Date endDate = dateRange[1];

        // Get all teams
        List<TeamsEntity> teams = teamsRepository.findAll();
        List<ClientStatsDto> teamStatsList = new ArrayList<>();

        for (TeamsEntity team : teams) {
            List<UserEntity> members = userRepository.findByTeam(team);

            // Fetching clients created by team members within date range
            List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(members, startDate, endDate);

            Map<String, Long> teamStatusCounts = teamClients.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getClientStatus().toString(),
                            Collectors.counting()));

            teamStatsList.add(new ClientStatsDto(team.getName(), teamClients.size(), teamStatusCounts));
        }

        return new OverallSystemDto(totalClients, overallStatusCounts, teamStatsList);
    }

    /**
     * @return
     */
    @Override
    public Page<ClientDto> getOverdueClients(int page, int limit) {

        Pageable pageableRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<ClientEntity> allClients = clientRepository.findAll(pageableRequest);

        EnumSet<Statuses> allowedStatuses = EnumSet.of(
                Statuses.PENDING,
                Statuses.INTERESTED,
                Statuses.AWAITING_DOCUMENTATION
        );

        List<ClientDto> overdueClients = new ArrayList<>();

        for (ClientEntity client : allClients) {
            if (client.getLastUpdated() == null) {
                continue;
            }

            long daysPending = ChronoUnit.DAYS.between(
                    client.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
            );

            if (daysPending > 5 &&
                    allowedStatuses.contains(client.getClientStatus())) {

                ClientDto dto = modelMapper.map(client, ClientDto.class);

                if (client.getCreatedBy() != null) {
                    UserDto createdByDto = modelMapper.map(client.getCreatedBy(), UserDto.class);
                    if (client.getCreatedBy().getTeam() != null) {
                        createdByDto.setTeamName(client.getCreatedBy().getTeam().getName());
                    }
                    dto.setCreatedBy(createdByDto);

                    // Assign the team lead of the creator
                    if (client.getCreatedBy().getTeamLead() != null) {
                        dto.setAssignedTo(modelMapper.map(client.getCreatedBy().getTeamLead(), UserDto.class));
                    } else {
                        // Creator is a team lead --- assign to themselves
                        dto.setAssignedTo(modelMapper.map(client.getCreatedBy(), UserDto.class));
                    }
                }


                dto.setClientStatus(client.getClientStatus().getDisplayName());
                overdueClients.add(dto);
            }
        }

        return new PageImpl<>(overdueClients, pageableRequest, overdueClients.size());
//        return allClients.map(clientEntity -> {
//            ClientDto dto = modelMapper.map(clientEntity, ClientDto.class);
//            return dto;
//        });
    }


    /**
     * @param name
     * @param status
     * @param date
     * @return
     */
    @Override
    public List<ClientSearchDto> searchClients(String name, Statuses status, LocalDate date) {
        Date startDateTime = (date != null) ? Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date endDateTime = (date != null) ? Date.from(date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()) : null;

        List<ClientEntity> clients = clientRepository.searchClients(
                (name != null && !name.trim().isEmpty()) ? name.trim() : null,
                status ,
                startDateTime,
                endDateTime
        );

        return clients.stream().map(client -> {
            ClientSearchDto dto = new ClientSearchDto();
            dto.setClientId(client.getClientId());
            dto.setFirstName(client.getFirstName());
            dto.setLastName(client.getLastName());
            dto.setPhoneNumber(client.getPhoneNumber());
            dto.setCreatedByName(client.getCreatedBy().getFirstName() + " " + client.getCreatedBy().getLastName());
            dto.setCreatedByEmail(client.getCreatedBy().getEmail());
            dto.setCreatedDate(client.getCreatedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            dto.setStatus(client.getClientStatus() != null ? client.getClientStatus().name() : null);
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * @param email
     * @param page
     * @param limit
     * @return
     */
    @Override
    public Page<ClientDto> getClients(String email, Integer page, Integer limit) {
        UserEntity user = userRepository.findByEmail(email);

        Pageable pageable = PageRequest.of(page, limit);
        Page<ClientEntity> clientsPage;

        if (user.getRole().getName().equals("ROLE_ADMIN")) {
            clientsPage = clientRepository.findAll(pageable);
        }
        else if (user.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            List<String> userIds = new ArrayList<>();
            userIds.add(user.getUserId());

            List<UserEntity> members = userRepository.findByTeamLead_UserId(user.getUserId());
            members.forEach(m -> userIds.add(m.getUserId()));

            clientsPage = clientRepository.findByCreatedByIdIn(userIds, pageable);
        }
        else {
            clientsPage = clientRepository.findByCreatedBy_UserId(user.getUserId(), pageable);
        }

        return clientsPage.map(client -> modelMapper.map(client, ClientDto.class));
    }



    @Override
    public PaginatedResponse<ClientRest> getOverdueClientsForUserRole(
            String loggedInUserId, String role, String targetUserId, Pageable pageable,
            String name, Statuses status, LocalDate fromDate, LocalDate toDate) {

        Page<ClientEntity> clientsPage;

        EnumSet<Statuses> allowedStatuses = EnumSet.of(
                Statuses.PENDING,
                Statuses.INTERESTED,
                Statuses.AWAITING_DOCUMENTATION
        );

        switch (role) {
            case "ROLE_ADMIN" -> {
                UserEntity targetUser = userRepository.findByUserId(targetUserId);

                if (targetUser.getRole().getName().equals("ROLE_TEAM_LEAD")) {
                    // Get team lead + all their team members' userIds
                    List<String> userIds = new ArrayList<>();
                    userIds.add(targetUserId); // team lead
                    userIds.addAll(userRepository.findByTeamLead_UserId(targetUserId)
                            .stream()
                            .map(UserEntity::getUserId)
                            .toList());

                    clientsPage = clientRepository.findByCreatedBy_UserIdIn(userIds, pageable);

                } else {
                    // Just that user's clients
                    clientsPage = clientRepository.findByCreatedBy_UserId(targetUserId, pageable);
                }
            }
            case "ROLE_TEAM_LEAD" -> {
                if (loggedInUserId.equals(targetUserId)) {
                    clientsPage = clientRepository.findByCreatedBy_UserId(loggedInUserId, pageable);
                } else {
                    boolean isMember = userRepository.existsByUserIdAndTeamLead_UserId(targetUserId, loggedInUserId);
                    if (!isMember) {
                        throw new AccessDeniedException("You can only view your own or your team members' clients");
                    }
                    clientsPage = clientRepository.findByCreatedBy_UserId(targetUserId, pageable);
                }
            }
            case "ROLE_TEAM_MEMBER" -> {
                if (!loggedInUserId.equals(targetUserId)) {
                    throw new AccessDeniedException("You can only view your own clients");
                }
                clientsPage = clientRepository.findByCreatedBy_UserId(loggedInUserId, pageable);
            }
            default -> throw new AccessDeniedException("Invalid role");
        }

        // Filtering overdue clients
        List<ClientDto> overdueClients = clientsPage.stream()
                .filter(client -> client.getLastUpdated() != null)
                .filter(client -> {
                    long daysPending = ChronoUnit.DAYS.between(
                            client.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            LocalDate.now()
                    );
                    return daysPending > 5 &&
                            allowedStatuses.contains(client.getClientStatus());
                })

                // name search
                .filter(client -> {
                    if (name == null || name.isBlank()) return true;
                    String fullName = (client.getFirstName() + " " + client.getLastName()).toLowerCase();
                    return fullName.contains(name.toLowerCase());
                })

                // status filter
                .filter(client -> {
                    if (status == null) return true;
                    return client.getClientStatus() == status;
                })

                // date range filter
                .filter(client -> {
                    if (fromDate == null && toDate == null) return true;
                    LocalDate createdDate = client.getCreatedDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    if (fromDate != null && toDate != null) {
                        return (createdDate.isEqual(fromDate) || createdDate.isAfter(fromDate)) &&
                                (createdDate.isEqual(toDate) || createdDate.isBefore(toDate));
                    } else if (fromDate != null) {
                        return createdDate.isEqual(fromDate) || createdDate.isAfter(fromDate);
                    } else {
                        return createdDate.isEqual(toDate) || createdDate.isBefore(toDate);
                    }
                })
                .map(client -> {
                    ClientDto dto = modelMapper.map(client, ClientDto.class);

                    if (client.getCreatedBy() != null) {
                        dto.setCreatedBy(modelMapper.map(client.getCreatedBy(), UserDto.class));

                        if (client.getCreatedBy().getTeamLead() != null) {
                            dto.setAssignedTo(modelMapper.map(client.getCreatedBy().getTeamLead(), UserDto.class));
                        }
                        else {
                            // Creator is a team lead → assign to themselves
                            dto.setAssignedTo(modelMapper.map(client.getCreatedBy(), UserDto.class));
                        }
                    }


                    dto.setClientStatus(client.getClientStatus().getDisplayName());
                    return dto;
                })
                .toList();

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

        // Building PaginatedResponse
        PaginatedResponse<ClientRest> response = new PaginatedResponse<>();
        response.setData(result);
        response.setCurrentPage(clientsPage.getNumber());
        response.setTotalPages(clientsPage.getTotalPages());
        response.setTotalItems(clientsPage.getTotalElements());
        response.setPageSize(clientsPage.getSize());
        response.setHasNext(clientsPage.hasNext());
        response.setHasPrevious(clientsPage.hasPrevious());

        return response;
    }

    @Override
    public void deactivateClient(String clientId) {
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);

        if (!clientEntity.isActive()) {
            throw new RuntimeException("Client is already inactive");
        }

        clientEntity.setActive(false);
        clientRepository.save(clientEntity);
    }



    @Override
    public PaginatedResponse<ClientRest> getMyClientsForUserRole(String loggedInUserId, String role, String userId,
            Pageable pageable, String name, Statuses status, LocalDate date) {

        List<String> allowedUserIds = new ArrayList<>();

        switch (role) {
            case "ROLE_ADMIN" -> {
                UserEntity targetUser = userRepository.findByUserId(userId);

                if (targetUser.getRole().getName().equals("ROLE_TEAM_LEAD")) {
                    allowedUserIds.add(userId);
                    allowedUserIds.addAll(userRepository.findByTeamLead_UserId(userId)
                            .stream()
                            .map(UserEntity::getUserId)
                            .toList());
                } else {
                    allowedUserIds.add(userId);
                }
            }
            case "ROLE_TEAM_LEAD" -> {
                if (loggedInUserId.equals(userId)) {
                    allowedUserIds.add(loggedInUserId);
                    allowedUserIds.addAll(userRepository.findByTeamLead_UserId(loggedInUserId)
                            .stream()
                            .map(UserEntity::getUserId)
                            .toList());
                } else {
                    boolean isMember = userRepository.existsByUserIdAndTeamLead_UserId(userId, loggedInUserId);
                    if (!isMember) {
                        throw new AccessDeniedException("You can only view your own or your team members' clients");
                    }
                    allowedUserIds.add(userId);
                }
            }
            case "ROLE_TEAM_MEMBER" -> {
                if (!loggedInUserId.equals(userId)) {
                    throw new AccessDeniedException("You can only view your own clients");
                }
                allowedUserIds.add(loggedInUserId);
            }
            default -> throw new AccessDeniedException("Invalid role");
        }

        // Converting date filters
        Date startDateTime = (date != null)
                ? Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;
        Date endDateTime = (date != null)
                ? Date.from(date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
                : null;

        // Calling repository search method
        Page<ClientEntity> clientsPage = clientRepository.searchClientsWithUserIds(allowedUserIds, (name != null && !name.trim().isEmpty()) ? name.trim() : null,
                status, startDateTime, endDateTime, pageable
        );

        List<ClientRest> dtoList = clientsPage.stream()
                .map(client -> {
                    ClientRest dto = modelMapper.map(client, ClientRest.class);

                    if (client.getCreatedBy() != null) {
                        dto.setCreatedBy(client.getCreatedBy().getFirstName() + " " + client.getCreatedBy().getLastName());
                    } else {
                        dto.setCreatedBy("Unknown");
                    }

                    if (dto.getCreatedAt() == null) {
                        if (client.getCreatedDate() != null) {
                            LocalDateTime ldt = client.getCreatedDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            dto.setCreatedAt(ldt);
                        } else {
                            dto.setCreatedAt(LocalDateTime.of(1970, 1, 1, 0, 0));
                        }
                    }

                    if (client.getLastUpdated() != null) {
                        dto.setLastUpdated(client.getLastUpdated().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDateTime());

                        Instant lastUpdatedInstant = client.getLastUpdated().toInstant();
                        Duration duration = Duration.between(lastUpdatedInstant, Instant.now());
                        dto.setLastAction(utils.getExactDuration(duration));
                    }

                    if (dto.getAssignedTo() == null) {
                        if (client.getTeamLead() != null) {
                            dto.setAssignedTo(client.getTeamLead().getFirstName() + " " + client.getTeamLead().getLastName());
                        } else {
                            dto.setAssignedTo("Unassigned");
                        }
                    }

                    return dto;
                })
                .toList();

        // Paginated response
        PaginatedResponse<ClientRest> response = new PaginatedResponse<>();
        response.setData(dtoList);
        response.setCurrentPage(clientsPage.getNumber());
        response.setTotalPages(clientsPage.getTotalPages());
        response.setTotalItems(clientsPage.getTotalElements());
        response.setPageSize(clientsPage.getSize());
        response.setHasNext(clientsPage.hasNext());
        response.setHasPrevious(clientsPage.hasPrevious());

        return response;
    }



    @Override
    public Object getClientStatsForLoggedInUser(String duration) {
        // Getting the logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String email = principal.getUsername();
        UserEntity loggedInUser = userRepository.findByEmail(email);

        //  Date range
        Date[] dateRange = calculateDateRange(duration);
        Date startDate = dateRange[0];
        Date endDate = dateRange[1];

        // Role-based stats
        if (loggedInUser.getRole().getName().equals("ROLE_ADMIN")) {
            // Admin → full system stats
            return getClientStats(duration);
        }
        else if (loggedInUser.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Team Lead → stats for their team
            List<UserEntity> members = userRepository.findByTeam(loggedInUser.getTeam());
            List<ClientEntity> teamClients = clientRepository
                    .findByCreatedByInAndCreatedDateBetween(members, startDate, endDate);

            Map<String, Long> statusCounts = teamClients.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getClientStatus().toString(),
                            Collectors.counting()));

            return new ClientStatsDto(
                    loggedInUser.getTeam().getName(),
                    teamClients.size(),
                    statusCounts
            );
        }
        else {
            // Team Member → only their own stats
            List<ClientEntity> myClients = clientRepository
                    .findByCreatedByAndCreatedDateBetween(loggedInUser, startDate, endDate);

            Map<String, Long> statusCounts = myClients.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getClientStatus().toString(),
                            Collectors.counting()));

            return new UserStatsDto(
                    loggedInUser.getUserId(),
                    loggedInUser.getFirstName() + " " + loggedInUser.getLastName(),
                    myClients.size(),
                    statusCounts
            );
        }
    }


}
