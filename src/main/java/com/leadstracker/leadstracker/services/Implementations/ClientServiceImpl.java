package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.*;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;

import io.micrometer.common.KeyValues;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leadstracker.leadstracker.repositories.ClientStatusHistoryRepository;


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

    @Autowired
    ClientStatusHistoryRepository clientStatusHistoryRepository;

    private static final Logger log = LoggerFactory.getLogger(ClientServiceImpl.class);


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
            clientEntity.setTeam(creatorEntity.getTeam()); // setting the team of the team lead who created the client
        } else {
//            if (creatorEntity.getTeamLead() == null) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team Member must be assigned to a Team Lead.");
//            }
            if (creatorEntity.getTeam() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must belong to a team to add a client.");
            }

//            clientEntity.setTeamLead(creatorEntity.getTeamLead()); // Team member created the client
            clientEntity.setTeamLead(creatorEntity.getTeam().getTeamLead());
            clientEntity.setTeam(creatorEntity.getTeam()); // setting the team of the team member who created the client
        }
        System.out.println("gpsLocation being saved: '" + clientEntity.getGpsLocation() + "'");

        ClientEntity saved = clientRepository.save(clientEntity);

        // Mapping back to DTO
        ClientDto result = modelMapper.map(saved, ClientDto.class);

        UserDto assignedToDto = modelMapper.map(saved.getTeamLead(), UserDto.class);
        result.setAssignedTo(assignedToDto);

        return result;
    }

    /**
     * @param startDate
     * @param endDate
     * @return
     */

    @Override
    public TeamPerformanceDto getTeamPerformance(String userId, LocalDate startDate, LocalDate endDate, String name, String teams) {
        UserEntity teamLead = userRepository.findByUserId(userId);
//                .orElseThrow(() -> new RuntimeException("Team Lead not found"));

        TeamsEntity team = teamLead.getTeam();

// --- Handle null team safely ---
        if (team == null) {
            TeamPerformanceDto response = new TeamPerformanceDto();
            response.setTeamId(null);
            response.setTeamName("No Team Assigned");
            response.setTotalClientsAdded(0);
            response.setTeamTarget(0);
            response.setProgressPercentage(0);
            response.setNumberOfTeamMembers(0);

            TeamMemberPerformanceDto leadPerformance = new TeamMemberPerformanceDto();
            leadPerformance.setMemberId(teamLead.getUserId());
            leadPerformance.setMemberName(teamLead.getFirstName() + " " + teamLead.getLastName());
            response.setLeadPerformance(leadPerformance);

            response.setTeamMembers(Collections.emptyList());
            response.setTeamLeadUserId(teamLead.getUserId());
            response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());

            return response;
        }

//        if (team == null) {
//            throw new RuntimeException("Team not found for the given lead");
//        }

        // Handle nulls safely
        LocalDate effectiveStart = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEnd   = (endDate   != null) ? endDate   : LocalDate.now();

        // --- Get active team target ---
        TeamTargetEntity activeTeamTarget = teamTargetRepository
                .findByTeamAndActiveTrue(team)
                .orElse(null);

        int teamTarget = (activeTeamTarget != null) ? activeTeamTarget.getTargetValue() : 0;

        // --- User target allocations (lead + members) ---
        Map<String, Integer> usersTargets = userTargetRepository.findByUser_Team(team).stream()
                .collect(Collectors.toMap(
                        ut -> ut.getUser().getUserId(),
                        UserTargetEntity::getTargetValue,
                        (a, b) -> a // keep one if duplicates
                ));

        // --- Clients in date range ---
        List<ClientEntity> clients = clientRepository.findByTeamAndCreatedDateBetween(
                team, effectiveStart.atStartOfDay(),
                effectiveEnd.atTime(23, 59, 59));

        List<ClientEntity> leadClients = clients.stream()
                .filter(c -> c.getCreatedBy().getUserId().equals(userId))
                .toList();

        List<UserEntity> teamMembers = userRepository.findByTeam(team);

        TeamPerformanceDto response = new TeamPerformanceDto();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());

        // --- Member performances (excluding lead) ---
        List<TeamMemberPerformanceDto> memberPerformances = teamMembers.stream()
                .filter(m -> !m.getUserId().equals(userId))
                .map(member -> {
                    List<ClientEntity> memberClients = clients.stream()
                            .filter(c -> c.getCreatedBy().getUserId().equals(member.getUserId()))
                            .toList();

                    int memberTarget = usersTargets.getOrDefault(member.getUserId(), 0);
                     double memberProgress = memberTarget > 0
                            ? (memberClients.size() * 100.0) / memberTarget
                            : 0.0;

                    Map<String, Integer> memberStatusSummary = memberClients.stream()
                            .collect(Collectors.toMap(
                                    c -> c.getClientStatus().getDisplayName(),
                                    c -> 1,
                                    Integer::sum
                            ));

                    TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
                    dto.setMemberId(member.getUserId());
                    dto.setMemberName(member.getFirstName() + " " + member.getLastName());
                    dto.setTotalClientsSubmitted(memberClients.size());
                    dto.setTarget(memberTarget);
                    dto.setProgressPercentage(Math.round(memberProgress));
                    dto.setClientStatus(memberStatusSummary);
                    dto.setEmail(member.getEmail());

                    return dto;
                })
                .toList();

        response.setTeamMembers(memberPerformances);

        // --- Lead performance (separate) ---
        int leadTarget = usersTargets.getOrDefault(userId, 0);
        double leadProgress = leadTarget > 0
                ? (leadClients.size() * 100.0) / leadTarget
                : 0.0;

        Map<String, Integer> leadStatusSummary = leadClients.stream()
                .collect(Collectors.toMap(
                        c -> c.getClientStatus().getDisplayName(),
                        c -> 1,
                        Integer::sum
                ));

        TeamMemberPerformanceDto leadPerformance = new TeamMemberPerformanceDto();
        leadPerformance.setMemberId(teamLead.getUserId());
        leadPerformance.setMemberName(teamLead.getFirstName() + " " + teamLead.getLastName());
        leadPerformance.setTotalClientsSubmitted(leadClients.size());
        leadPerformance.setTarget(leadTarget);
        leadPerformance.setProgressPercentage(Math.round(leadProgress));
        leadPerformance.setClientStatus(leadStatusSummary);
        leadPerformance.setEmail(teamLead.getEmail());

        response.setLeadPerformance(leadPerformance);

        // --- Team totals ---
        int totalClients = clients.size();
        double teamProgress = teamTarget > 0
                ? (totalClients * 100.0) / teamTarget
                : 0.0;

        response.setTotalClientsAdded(totalClients);
        response.setTeamTarget(teamTarget);
        response.setProgressPercentage(Math.round(teamProgress));
        response.setNumberOfTeamMembers(memberPerformances.size());
        response.setTeamLeadUserId(teamLead.getUserId());
        response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());

        return response;
    }


//    public TeamPerformanceDto getTeamPerformance(String userId, LocalDate startDate, LocalDate endDate, String name, String team) {
//        //Getting team lead and members
//        UserEntity teamLead = userRepository.findByUserId(userId);
//        TeamsEntity teams = teamLead.getTeam();
//
//        // Suppose you already have the team lead (UserEntity teamLead)
//        List<UserTargetEntity> userTargets = userTargetRepository.findByUser(teamLead);
//
////        List<UserTargetEntity> userTargetEntities = userTargetRepository.findByTeam(teams);
//        List<UserTargetEntity> userTargetEntities = userTargetRepository.findByUser_Team(teams);
//
//        List<TeamTargetEntity> teamTargets = teamTargetRepository.findByTeam_Id(teams.getId());
//
//        List<UserEntity> userEntities = userRepository.searchAllUsersByFirstNameAndLastNameAndTeamName(
//                        (name != null && !name.trim().isEmpty()) ? name.trim() : null, team);
//
//        if (teams == null) {
//            TeamPerformanceDto emptyResponse = new TeamPerformanceDto();
//            emptyResponse.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
//            emptyResponse.setTeamId(null);
//            emptyResponse.setEmail(teamLead.getEmail());
//            emptyResponse.setTeamLeadUserId(teamLead.getUserId());
//            emptyResponse.setTeamName("No Team Assigned");
//            emptyResponse.setTotalClientsAdded(0);
//            emptyResponse.setNumberOfTeamMembers(0);
//            emptyResponse.setTeamTarget(0);
//            emptyResponse.setProgressPercentage(0);
//            emptyResponse.setProgressFraction("0/0");
//
//            return emptyResponse;
//        }
//
//        //getting team members under a team lead
//        List<UserEntity> teamMembers = userRepository.findByTeamLead(teamLead);
//        // Adding team lead
//        if (!teamMembers.contains(teamLead)) {
//            teamMembers.add(teamLead);
//        }
//
//        //Calculating date range
//        Date[] dateRange = calculateDateRange(startDate, endDate);
//
//        // Fetching all the clients of the team within the range
//        List<ClientEntity> clients = clientRepository.findByCreatedByInAndCreatedDateBetween(
//                teamMembers, dateRange[0], dateRange[1]);
//
//
//        // Only lead's own clients
//        List<ClientEntity> leadClients = clientRepository.findByCreatedByAndCreatedDateBetween(
//                teamLead, dateRange[0], dateRange[1]);
//
//        //response
////        TeamPerformanceDto response = new TeamPerformanceDto();
////        response.setTeamId(teams.getId());
////        response.setTeamName(teams.getName());
////        response.setTeamLeadUserId(teamLead.getUserId());
////        response.setEmail(teamLead.getEmail());
////        response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
////        response.setTotalClientsAdded(clients.size());
////        // members only (excluding lead) for display
////        response.setNumberOfTeamMembers((int) teamMembers.stream()
////                .filter(entity -> !entity.getUserId().equals(teamLead.getUserId()))
////                .count());
////
////        //Fetching active team target
////        Optional<TeamTargetEntity> activeTargetOpt = teamTargetRepository
////                .findTopByTeamIdAndDueDateGreaterThanEqualOrderByDueDateAsc(teams.getId(), LocalDate.now());
////
////        int teamTarget = activeTargetOpt.map(TeamTargetEntity::getTargetValue).orElse(0);
////        response.setTeamTarget(teamTarget);
////
////        // Setting Progress
////        int numberOfClientsAdded = clients.size();
////        double progress = 0;
////
////        if (teamTarget > 0) {
////            progress = Math.ceil(((double) numberOfClientsAdded / teamTarget) * 100);
////        }
//////      Setting both percentage and fraction
////        response.setProgressPercentage(progress);
////        response.setProgressFraction(numberOfClientsAdded + "/" + teamTarget);
////
////        //Building the status distribution using the enum
////        response.setClientStatus(
////                clients.stream()
////                        .collect(Collectors.groupingBy(
////                                c -> (c.getClientStatus().getDisplayName()),
////                                Collectors.summingInt(c -> 1)
////                                )
////                        ));
////
////
////        // Member stats (excluding lead)
////        response.setTeamMembers(
////                teamMembers.stream()
////                        .filter(m -> !m.getUserId().equals(teamLead.getUserId()))
////                        .map(member -> teamMemberStats(member, dateRange[0], dateRange[1]))
////                        .collect(Collectors.toList())
////        );
////
////        // Build a map of userId -> target value
////        Map<String, Integer> targetMap = userTargets.stream()
////                .collect(Collectors.toMap(
////                        ut -> ut.getUser().getUserId(),
////                        UserTargetEntity::getTargetValue
////                ));
////
////
////// Lead’s data
////        int leadTarget = targetMap.getOrDefault(teamLead.getUserId(), 0);
////        int numberOfLeadClients = leadClients.size();
////
////        double leadProgress = 0;
////        if (leadTarget > 0) {
////            leadProgress = Math.ceil(((double) numberOfLeadClients / leadTarget) * 100);
////        }
////
////        Map<String, Integer> leadStatusSummary = leadClients.stream()
////                .collect(Collectors.groupingBy(
////                        c -> c.getClientStatus().getDisplayName(),
////                        Collectors.summingInt(c -> 1)
////                ));
////
////        response.setTeamLeadUserId(teamLead.getUserId());
////        response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
////        response.setTeamTarget(leadTarget);
////        response.setTotalClientsAdded(numberOfLeadClients);
////        response.setProgressPercentage(leadProgress);
////        response.setClientStatus(leadStatusSummary);
////        response.setEmail(teamLead.getEmail());
//
//        TeamPerformanceDto response = new TeamPerformanceDto();
//        response.setTeamId(teams.getId());
//        response.setTeamName(teams.getName());
//        response.setTeamLeadUserId(teamLead.getUserId());
//        response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
//        response.setEmail(teamLead.getEmail());
//
//// --- Team totals (all clients including lead) ---
//        int totalClients = clients.size();
//        int teamTarget = teamTargets.stream()
//                .mapToInt(TeamTargetEntity::getTargetValue)
//                .sum();
//
//        double teamProgress = teamTarget > 0 ? (totalClients * 100.0) / teamTarget : 0.0;
//
//        response.setTotalClientsAdded(totalClients);
//        response.setTeamTarget(teamTarget);
//        response.setProgressPercentage(teamProgress);
//        response.setProgressFraction(totalClients + "/" + teamTarget);
//
//// Build team client status summary inline
//        Map<String, Integer> teamStatusSummary = clients.stream()
//                .collect(Collectors.toMap(
//                        c -> c.getClientStatus().getDisplayName(),
//                        c -> 1,
//                        Integer::sum
//                ));
//        response.setClientStatus(teamStatusSummary);
//
//// --- Members (excluding lead) ---
//        List<TeamMemberPerformanceDto> memberPerformances = teamMembers.stream()
//                .map(member -> {
//                    List<ClientEntity> memberClients = clients.stream()
//                            .filter(c -> c.getCreatedBy().getUserId().equals(member.getUserId()))
//                            .toList();
//
//
//// Convert List<UserTargetEntity> → Map<userId, targetValue>
//                    // Build the map using String keys
//                    Map<String, Integer> usersTargets = userTargetEntities.stream()
//                            .collect(Collectors.toMap(
//                                    ut -> ut.getUser().getUserId(),   // String userId
//                                    UserTargetEntity::getTargetValue,
//                                    Integer::sum
//                            ));
//
//// Then check by String
//                    int memberTarget = usersTargets.getOrDefault(member.getUserId(), 0);
//
//
//                    double memberProgress = memberTarget > 0
//                            ? (memberClients.size() * 100.0) / memberTarget
//                            : 0.0;
//
//                    Map<String, Integer> memberStatusSummary = memberClients.stream()
//                            .collect(Collectors.toMap(
//                                    c -> c.getClientStatus().getDisplayName(),
//                                    c -> 1,
//                                    Integer::sum
//                            ));
//
//                    TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
//                    dto.setMemberId(member.getUserId());
//                    dto.setMemberName(member.getFirstName() + " " + member.getLastName());
//                    dto.setTotalClientsSubmitted(memberClients.size());
//                    dto.setTarget(memberTarget);
//                    dto.setProgressPercentage(memberProgress);
//                    dto.setClientStatus(memberStatusSummary);
//                    dto.setEmail(member.getEmail());
//
//                    return dto;
//                })
//                .toList();
//
//        response.setTeamMembers(memberPerformances);
//
//// --- Lead personal performance (separate field) ---
//        List<ClientEntity> leadsClients = clients.stream()
//                .filter(c -> c.getCreatedBy().getUserId().equals(teamLead.getUserId()))
//                .toList();
//
//        Map<String, Integer> userTarget = userTargetEntities.stream()
//                .collect(Collectors.toMap(
//                        ut -> ut.getUser().getUserId(),   // String userId
//                        UserTargetEntity::getTargetValue,
//                        Integer::sum
//                ));
//
//
//        int leadTarget = userTarget.getOrDefault(teamLead.getUserId(), 0);
//
//        double leadProgress = leadTarget > 0
//                ? (leadsClients.size() * 100.0) / leadTarget
//                : 0.0;
//
//        Map<String, Integer> leadStatusSummary = leadsClients.stream()
//                .collect(Collectors.toMap(
//                        c -> c.getClientStatus().getDisplayName(),
//                        c -> 1,
//                        Integer::sum
//                ));
//
//        TeamMemberPerformanceDto leadPerformance = new TeamMemberPerformanceDto();
//        leadPerformance.setMemberId(teamLead.getUserId());
//        leadPerformance.setMemberName(teamLead.getFirstName() + " " + teamLead.getLastName());
//        leadPerformance.setTotalClientsSubmitted(leadClients.size());
//        leadPerformance.setTarget(leadTarget);
//        leadPerformance.setProgressPercentage(leadProgress);
//        leadPerformance.setClientStatus(leadStatusSummary);
//        leadPerformance.setEmail(teamLead.getEmail());
//
//// ⚠️ You need to add this field in TeamPerformanceDto first
//// private TeamMemberPerformanceDto leadPerformance;
//        response.setLeadPerformance(leadPerformance);
//
//
//        return response;
//    }


    private Date[] calculateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // If no dates provided, use last 5 days as default
        if (startDate == null && endDate == null) {
            endDate = today;
            startDate = today.minusDays(4); // 5 days total
        }
        // If only startDate is provided
        else if (startDate != null && endDate == null) {
            endDate = today;
        }
        // If only endDate is provided
        else if (startDate == null && endDate != null) {
            startDate = endDate.minusDays(4);
        }

        // Ensuring at least 5 days
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween < 4) {
            startDate = endDate.minusDays(4);
        }

        return new Date[]{
                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
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
                                c -> (c.getClientStatus().getDisplayName()),
                                Collectors.summingInt(c -> 1)
                        ))
        );


        return dto;
    }


    @Override
    public TeamMemberPerformanceDto getMemberPerformance(String memberId, LocalDate startDate, LocalDate endDate) {
        UserEntity member = userRepository.findByUserId(memberId);
        Date[] dateRange = calculateDateRange(startDate, endDate);

//        return teamMemberStats(member, dateRange[0], dateRange[1]);
        // Build stats
        TeamMemberPerformanceDto dto = teamMemberStats(member, dateRange[0], dateRange[1]);

        // Add member basic info
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setEmail(member.getEmail());
        dto.setPhoneNumber(member.getPhoneNumber());
        dto.setStaffId(member.getStaffId());
        dto.setCreatedDate(member.getCreatedDate());

        // Handle team name safely
        if (member.getTeam() != null) {
            dto.setTeamName(member.getTeam().getName());
        } else {
            dto.setTeamName("Unassigned");
        }

        // Handle team lead safely
        if (member.getTeamLead() != null) {
            dto.setTeamLeadName(member.getTeamLead().getFirstName() + " " + member.getTeamLead().getLastName());
        } else if (member.getTeam() != null && member.getTeam().getTeamLead() != null) {
            UserEntity lead = member.getTeam().getTeamLead();
            dto.setTeamLeadName(lead.getFirstName() + " " + lead.getLastName());
        } else {
            dto.setTeamLeadName("No Lead");
        }

        return dto;

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
        try {
            // Fetch the client entity
            ClientEntity clientEntity = clientRepository.findByClientId(clientId);
            if (clientEntity == null) {
                throw new UsernameNotFoundException("Client with ID: " + clientId + " not found");
            }

            // Keep a copy of the old client data for email notification
            ClientEntity oldClient = new ClientEntity();
            oldClient.setFirstName(clientEntity.getFirstName());
            oldClient.setLastName(clientEntity.getLastName());
            oldClient.setPhoneNumber(clientEntity.getPhoneNumber());
            oldClient.setClientStatus(clientEntity.getClientStatus());
            oldClient.setGpsLocation(clientEntity.getGpsLocation());

            // Check for internet availability before update
            if (!isInternetAvailable()) {
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update client.");
            }

            // Get logged-in user
            String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            UserEntity updatedBy = userRepository.findByEmail(loggedInUsername);

            // Capture old status to compare
            String oldStatus = clientEntity.getClientStatus().getDisplayName();

            // Update fields
            clientEntity.setFirstName(clientDto.getFirstName());
            clientEntity.setLastName(clientDto.getLastName());
            clientEntity.setGpsLocation(clientDto.getGpsLocation());
            clientEntity.setClientStatus(Statuses.fromString(clientDto.getClientStatus()));
            clientEntity.setLastUpdated(Date.from(Instant.now()));

            // Save updated client
            ClientEntity updatedClient = clientRepository.save(clientEntity);

            // Capture new status after update
            String newStatus = updatedClient.getClientStatus().getDisplayName();

            // Log status change if there was a change
            if (!newStatus.equals(oldStatus)) {
                ClientStatusHistoryEntity history = new ClientStatusHistoryEntity();
                history.setClient(updatedClient);
                history.setOldStatus(Statuses.fromString(oldStatus));
                history.setNewStatus(Statuses.fromString(newStatus));
                history.setChangedAt(LocalDateTime.now());
                history.setChangedBy(updatedBy);
                history.setStatus(Statuses.fromString(newStatus));

                clientStatusHistoryRepository.save(history);
            }

            // Build the response DTO
            ClientDto responseDto = modelMapper.map(updatedClient, ClientDto.class);

            // Fetch and map status history
            List<ClientStatusHistoryEntity> historyList = clientStatusHistoryRepository
                    .findByClient_ClientIdAndChangedByOrderByChangedAtDesc(updatedClient.getClientId());


            List<ClientStatusHistoryDto> historyDtos = historyList.stream()
                    .map(history -> {
                        ClientStatusHistoryDto dto = new ClientStatusHistoryDto();
                        dto.setOldStatus(history.getOldStatus() != null ? history.getOldStatus().getDisplayName() : "N/A");
                        dto.setNewStatus(history.getNewStatus() != null ? history.getNewStatus().getDisplayName() : "N/A");
                        dto.setChangedAt(history.getChangedAt());
//                        dto.setChangedBy(history.getChangedBy() != null ? history.getChangedBy().getFirstName() + " " + history.getChangedBy().getLastName() : "System");
                        dto.setChangedBy(modelMapper.map(history.getChangedBy(), UserDto.class));
                        return dto;
                    }).toList();

            responseDto.setStatusHistory(historyDtos);

            return responseDto;

        } catch (Exception e) {
            log.error("Error while updating client with ID {}: {}", clientId, e.getMessage(), e);
            throw e; // rethrow to let GlobalExceptionHandler handle it
        }
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
    public Page<ClientDto> getAllClients(int page, int limit, String name, Statuses status, String team) {
        if (page > 0) {
            page -= 1;
        }

        Pageable pageableRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<ClientEntity> clientPage = clientRepository.searchAllClientsByFirstNameAndLastNameAndClientStatusAndTeamNameAndActiveTrue
                (pageableRequest,
                (name != null && !name.trim().isEmpty()) ? name.trim() : null, status, team);

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
        // Find the client
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);
        if (clientEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + clientId + " not found");
        }

        // Map base client fields
        ClientDto returnClient = new ClientDto();
        BeanUtils.copyProperties(clientEntity, returnClient);
        returnClient.setCreatedBy(modelMapper.map(clientEntity.getCreatedBy(), UserDto.class));
        returnClient.setAssignedTo(modelMapper.map(clientEntity.getTeamLead(), UserDto.class));
        returnClient.setClientStatus(clientEntity.getClientStatus().getDisplayName());
        returnClient.setGpsLocation(clientEntity.getGpsLocation());
        returnClient.setPhoneNumber(clientEntity.getPhoneNumber());
        returnClient.setCreatedDate(clientEntity.getCreatedDate());
        returnClient.setLastUpdated(clientEntity.getLastUpdated());

        // Fetch and map status history
        List<ClientStatusHistoryEntity> historyEntities =
                clientStatusHistoryRepository.findByClient_ClientIdAndChangedByOrderByChangedAtDesc(clientId);

        System.out.println(historyEntities);

        List<ClientStatusHistoryDto> historyDtos = historyEntities.stream()
                .map(history -> {
                    ClientStatusHistoryDto dto = new ClientStatusHistoryDto();

                    // Map OLD status
                    dto.setOldStatus(history.getOldStatus() != null
                            ? history.getOldStatus().getDisplayName()
                            : "N/A");

                    // Map NEW status
                    dto.setNewStatus(history.getNewStatus() != null
                            ? history.getNewStatus().getDisplayName()
                            : "System");

                    // Map change timestamp
                    dto.setChangedAt(history.getChangedAt());

                    if (history.getChangedBy() != null) {
                        dto.setChangedBy(modelMapper.map(history.getChangedBy(), UserDto.class));
                    }
                    return dto;
                })
                .toList();

        // Attach status history to client DTO
        returnClient.setStatusHistory(historyDtos);

        return returnClient;
    }



    @Override
    public List<ClientStatusHistoryDto> getClientStatusHistory(String clientId) {
        ClientEntity client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new UsernameNotFoundException("Client not found: " + clientId);
        }

        List<ClientStatusHistoryEntity> historyEntities =
                clientStatusHistoryRepository.findByClientOrderByChangedAtDesc(client);

        return historyEntities.stream().map(entity -> {
            ClientStatusHistoryDto dto = new ClientStatusHistoryDto();

            // Convert enums to display names
            dto.setOldStatus(entity.getOldStatus() != null
                    ? entity.getOldStatus().getDisplayName()
                    : "N/A");

            dto.setNewStatus(entity.getNewStatus() != null
                    ? entity.getNewStatus().getDisplayName()
                    : "N/A");

            if(entity.getChangedBy() != null) {
                dto.setChangedBy(modelMapper.map(entity.getChangedBy(), UserDto.class));
            }

            return dto;
        }).toList();
    }

    /**
     * @param response
     */
    @Override
    public void exportExcel(HttpServletResponse response, String table) throws IOException {
        switch (table) {
            case "all-clients" -> {
                List<ClientEntity> clientEntities = clientRepository.findAll();
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("Clients");
                XSSFRow row = sheet.createRow(0);

                row.createCell(0).setCellValue("Client Name");
                row.createCell(1).setCellValue("Status");
                row.createCell(2).setCellValue("Date Added");
                row.createCell(3).setCellValue("Time Overdue");
                row.createCell(4).setCellValue("Assigned To");
                row.createCell(5).setCellValue("Forwarded By");
                row.createCell(6).setCellValue("Team");

                int dataRowIndex = 1;
                for(ClientEntity clientEntity : clientEntities) {
                    XSSFRow dataRow = sheet.createRow(dataRowIndex);
                    dataRow.createCell(0).setCellValue(clientEntity.getFirstName()
                            + clientEntity.getLastName());
                    dataRow.createCell(1).setCellValue(clientEntity.getClientStatus().getDisplayName());
                    dataRow.createCell(2).setCellValue(clientEntity.getCreatedDate().toString());
                    dataRow.createCell(3).setCellValue(clientEntity.getLastUpdated().toString());
                    dataRow.createCell(4).setCellValue(clientEntity.getTeamLead().getFirstName()
                            + clientEntity.getTeamLead().getLastName());
                    dataRow.createCell(5).setCellValue(clientEntity.getCreatedBy().getFirstName()
                            + clientEntity.getCreatedBy().getLastName());
//            dataRow.createCell(6).setCellValue(clientEntity.getCreatedBy()
//                    .getTeam().getName());
                }

                ServletOutputStream out = response.getOutputStream();
                workbook.write(out);
                workbook.close();
                out.close();
            }

            case "team-members-under-team-lead" -> {
                List<UserEntity> userEntities = userRepository.findAll();
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("team members");
                XSSFRow row = sheet.createRow(0);

                row.createCell(0).setCellValue("Name");
                row.createCell(1).setCellValue("ID");
                row.createCell(2).setCellValue("Date Added");
                row.createCell(3).setCellValue("Email");
                row.createCell(4).setCellValue("Phone");

                int dataRowIndex = 1;
                for(UserEntity userEntity: userEntities) {
                    XSSFRow dataRow = sheet.createRow(dataRowIndex);
                    dataRow.createCell(0).setCellValue(userEntity.getFirstName()
                    + userEntity.getLastName());
                    dataRow.createCell(1).setCellValue(userEntity.getUserId());
                    dataRow.createCell(2).setCellValue(userEntity.getCreatedDate());
                    dataRow.createCell(3).setCellValue(userEntity.getEmail());
                    dataRow.createCell(4).setCellValue(userEntity.getPhoneNumber());
                    dataRowIndex++;
                }
                ServletOutputStream out = response.getOutputStream();
                workbook.write(out);
                workbook.close();
                out.close();
            }
        }

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
    public OverallSystemDto getClientStats(LocalDate fromDate, LocalDate toDate) {
        // Calculating date range based on duration
        Date[] dateRange = calculateDateRange(fromDate, toDate);
        Date startDate = dateRange[0];
        Date endDate = dateRange[1];

        // Overall stats (filtered by date range)
        long totalClients = clientRepository.countByCreatedDateBetween(startDate, endDate);
        List<ClientEntity> overallClients = clientRepository.findByCreatedDateBetween(startDate, endDate);

        Map<String, Long> overallStatusCounts = overallClients.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getClientStatus().toString(),
                        Collectors.counting()
                ));

        // Per-team stats
        List<TeamsEntity> teams = teamsRepository.findByActiveTrue();
        List<ClientStatsDto> teamStatsList = new ArrayList<>();

        for (TeamsEntity team : teams) {
            List<UserEntity> members = userRepository.findByTeam(team);

            // Fetching clients created by team members within date range
            List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(members, startDate, endDate);

            Map<String, Long> teamStatusCounts = teamClients.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getClientStatus().toString(),
                            Collectors.counting()
                    ));

            teamStatsList.add(new ClientStatsDto(
                    team.getName(),
                    teamClients.size(),
                    teamStatusCounts
            ));
        }

        return new OverallSystemDto(totalClients, overallStatusCounts, teamStatsList);
    }


    /**
     * @return
     */
    @Override
    public Page<ClientDto> getOverdueClients(int page, int limit, LocalDate startDate, LocalDate endDate, String name) {
        Pageable pageableRequest = PageRequest.of(page, limit);

        List<Statuses> allowedStatuses = List.of(
                Statuses.PENDING,
                Statuses.INTERESTED,
                Statuses.AWAITING_DOCUMENTATION
        );

        Page<ClientEntity> overdueClients = clientRepository.findOverdueClients(allowedStatuses, pageableRequest);

        // Filtering in-memory
        List<ClientDto> filtered = overdueClients
                .stream()
                .filter(client -> {
                    if (startDate == null && endDate == null) return true;
                    LocalDate createdDate = client.getCreatedDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    if (startDate != null && endDate != null) {
                        return (createdDate.isEqual(startDate) || createdDate.isAfter(startDate)) &&
                                (createdDate.isEqual(endDate) || createdDate.isBefore(endDate));
                    } else if (startDate != null) {
                        return createdDate.isEqual(startDate) || createdDate.isAfter(startDate);
                    } else {
                        return createdDate.isEqual(endDate) || createdDate.isBefore(endDate);
                    }
                })
                .filter(client -> {
                    if (name == null || name.isBlank()) return true;
                    String fullName = (client.getFirstName() + " " + client.getLastName()).toLowerCase();
                    return fullName.contains(name.toLowerCase());
                })
                .map(client -> {
                    ClientDto dto = modelMapper.map(client, ClientDto.class);

                    if (client.getCreatedBy() != null) {
                        UserDto createdByDto = modelMapper.map(client.getCreatedBy(), UserDto.class);
                        if (client.getCreatedBy().getTeam() != null) {
                            createdByDto.setTeamName(client.getCreatedBy().getTeam().getName());
                        }
                        dto.setCreatedBy(createdByDto);

                        if (client.getCreatedBy().getTeamLead() != null) {
                            dto.setAssignedTo(modelMapper.map(client.getCreatedBy().getTeamLead(), UserDto.class));
                        } else {
                            dto.setAssignedTo(modelMapper.map(client.getCreatedBy(), UserDto.class));
                        }
                    }

                    dto.setClientStatus(client.getClientStatus().getDisplayName());
                    return dto;
                })
                .toList();

        // Wrapping the filtered list back into a Page
        return new PageImpl<>(filtered, pageableRequest, filtered.size());
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
            Pageable pageable, String name, Statuses status, LocalDate fromDate, LocalDate toDate) {

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
        Date startDateTime = (fromDate != null)
                ? Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;

        Date endDateTime = (toDate != null)
                ? Date.from(toDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
                : null;

        // Calling repository search method
        Page<ClientEntity> clientsPage = clientRepository.searchClientsWithUserIdsAndActiveTrue(allowedUserIds, (name != null && !name.trim().isEmpty()) ? name.trim() : null,
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
    public Object getClientStatsForLoggedInUser(LocalDate fromDate, LocalDate toDate) {
        // Getting the logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String email = principal.getUsername();
        UserEntity loggedInUser = userRepository.findByEmail(email);

        //  Date range
        Date[] dateRange = calculateDateRange(fromDate, toDate);
        Date startDate = dateRange[0];
        Date endDate = dateRange[1];

        // Role-based stats
        if (loggedInUser.getRole().getName().equals("ROLE_ADMIN")) {
            // Admin full system stats
            return getClientStats(fromDate, toDate);
        }
        else if (loggedInUser.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Team Lead stats for themselves
            List<ClientEntity> leadClients = clientRepository
                    .findByCreatedByAndCreatedDateBetween(loggedInUser, startDate, endDate);

            Map<String, Long> statusCounts = leadClients.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getClientStatus().toString(),
                            Collectors.counting()));

            return new UserStatsDto(
                    loggedInUser.getUserId(),
                    loggedInUser.getFirstName() + " " + loggedInUser.getLastName(),
                    leadClients.size(),
                    statusCounts
            );
//            List<UserEntity> members = userRepository.findByTeam(loggedInUser.getTeam());
//            List<ClientEntity> teamClients = clientRepository
//                    .findByCreatedByInAndCreatedDateBetween(members, startDate, endDate);
//
//            Map<String, Long> statusCounts = teamClients.stream()
//                    .collect(Collectors.groupingBy(
//                            c -> c.getClientStatus().toString(),
//                            Collectors.counting()));
//
//            return new ClientStatsDto(
//                    loggedInUser.getTeam().getName(),
//                    teamClients.size(),
//                    statusCounts
//            );
        }
        else {
            // Team Member getting their own stats
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
