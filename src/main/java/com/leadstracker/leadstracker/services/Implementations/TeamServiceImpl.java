package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.*;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    TeamsRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    TeamTargetRepository teamTargetRepository;

    @Autowired
    UserTargetRepository userTargetRepository;
    /**
     * @param name
     * @param startDate
     * @param endDate
     * @return
     */
//    @Override
//    public List<TeamPerformanceDto> getTeamsOverview(String name, LocalDate startDate, LocalDate endDate) {
//        List<TeamsEntity> teams = teamRepository.findAllByActiveTrue();
//
//        if (name != null && !name.isBlank()) {
//            teams = teamRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
//        }
//
//        //date range (default last 5 days, min 5 days)
//        Date[] dateRange = calculateDateRange(startDate, endDate);
//
//        return teams.stream()
//                .map(team -> {
//                    UserEntity teamLead = team.getTeamLead();
//
//                    //Getting members assigned to this lead
//                    List<UserEntity> membersOnly = (teamLead != null)
//                            ? userRepository.findByTeamLead(teamLead)
//                            : new ArrayList<>();
//
//                    // Building participants = members + lead
//                    List<UserEntity> participants = new ArrayList<>(membersOnly);
//                    if (teamLead != null) {
//                        participants.add(teamLead);
//                    }
//                    participants = participants.stream()
//                            .collect(Collectors.collectingAndThen(
//                                    Collectors.toCollection(java.util.LinkedHashSet::new),
//                                    ArrayList::new
//                            ));
//
//                    //All clients created by lead + members
//                    List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(
//                            participants, dateRange[0], dateRange[1]);
//
//                    TeamPerformanceDto dto = new TeamPerformanceDto();
//                    dto.setTeamId(team.getId());
//                    dto.setTeamName(team.getName());
//
//                    // Lead fields
//                    dto.setTeamLeadUserId(teamLead != null ? teamLead.getUserId() : null);
//                    dto.setTeamLeadName(teamLead != null ? (teamLead.getFirstName() + " " + teamLead.getLastName()) : null);
//                    dto.setEmail(teamLead != null ? teamLead.getEmail() : null);
//
//                    // Number of members = team lead + team members
//                    dto.setNumberOfTeamMembers(participants.size());
//
//                    //Total clients = team lead’s clients + team members’ clients
//                    dto.setTotalClientsAdded(teamClients.size());
//
//                    // Active team target
//                    int teamTarget = teamTargetRepository
//                            .findTopByTeamIdAndDueDateGreaterThanEqualOrderByDueDateAsc(team.getId(), LocalDate.now())
//                            .map(TeamTargetEntity::getTargetValue)
//                            .orElse(0);
//                    dto.setTeamTarget(teamTarget);
//
//                    //Setting Progress
//                    int added = teamClients.size();
//                    double progress = (teamTarget > 0) ? Math.ceil((added * 100.0) / teamTarget) : 0.0;
//                    dto.setProgressPercentage(progress);
//                    dto.setProgressFraction(added + "/" + teamTarget);
//
//                    // Team-level client status breakdown
//                    dto.setClientStatus(
//                            teamClients.stream()
//                                    .collect(Collectors.groupingBy(
//                                            c -> c.getClientStatus().getDisplayName(),
//                                            Collectors.summingInt(c -> 1)
//                                    ))
//                    );
//
//                    // Per-participant stats (lead + members)
//                    dto.setTeamMembers(
//                            participants.stream()
//                                    .map(p -> teamMemberStats(p, dateRange[0], dateRange[1]))
//                                    .collect(Collectors.toList())
//                    );
//
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
//
//    //Team member stats
//    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
//        List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(member, start, end);
//
//        // Latest target for this user
//        UserTargetEntity latestTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(member);
//        int target = (latestTarget != null) ? latestTarget.getTargetValue() : 0;
//
//        int submitted = memberClients.size();
//        double progressPercentage = (target > 0) ? Math.ceil((submitted * 100.0) / target) : 0.0;
//
//        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
//        dto.setMemberId(member.getUserId());
//        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
//        dto.setEmail(member.getEmail());
//        dto.setTeamName(member.getTeam() != null ? member.getTeam().getName() : null);
//        dto.setTeamLeadName(member.getTeamLead() != null
//                ? (member.getTeamLead().getFirstName() + " " + member.getTeamLead().getLastName())
//                : (member.getTeam() != null && member.getTeam().getTeamLead() != null
//                ? member.getTeam().getTeamLead().getFirstName() + " " + member.getTeam().getTeamLead().getLastName()
//                : null));
//
//        dto.setTotalClientsSubmitted(submitted);
//        dto.setTarget(target);
//        dto.setProgressPercentage(progressPercentage);
//        dto.setProgressFraction(submitted + "/" + target);
//
//        dto.setClientStatus(
//                memberClients.stream()
//                        .collect(Collectors.groupingBy(
//                                c -> c.getClientStatus().getDisplayName(),
//                                Collectors.summingInt(c -> 1)
//                        ))
//        );
//
//        return dto;
//    }
//
//    //date range method
//    private Date[] calculateDateRange(LocalDate startDate, LocalDate endDate) {
//        LocalDate today = LocalDate.now();
//
//        // If no dates provided, use last 5 days as default
//        if (startDate == null && endDate == null) {
//            endDate = today;
//            startDate = today.minusDays(4);
//        }
//        //  If only startDate is provided
//        else if (startDate != null && endDate == null) {
//            endDate = today;
//        }
//        //  If only endDate is provided
//        else if (startDate == null && endDate != null) {
//            startDate = endDate.minusDays(4);
//        }
//
//        // Ensuring at least 5 days
//        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
//        if (daysBetween < 4) {
//            startDate = endDate.minusDays(4);
//        }
//
//        return new Date[]{
//                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
//                Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
//        };
//    }

    @Override
    public List<TeamPerformanceDto> getTeamsOverview(String name, LocalDate startDate, LocalDate endDate) {
        List<TeamsEntity> teams = teamRepository.findAllByActiveTrue();

        if (name != null && !name.isBlank()) {
            teams = teamRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
        }

        //date range (default last 5 days, min 5 days)
        Date[] dateRange = calculateDateRange(startDate, endDate);

        return teams.stream()
                .map(team -> {
                    UserEntity teamLead = team.getTeamLead();

                    // Participants = all users in the team (lead + members)
                    List<UserEntity> participants = new ArrayList<>(team.getUsers());

                    // De-duplicate in case of any duplicates
                    participants = participants.stream()
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toCollection(LinkedHashSet::new),
                                    ArrayList::new
                            ));

                    // All clients created by any team participant
                    List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(
                            participants, dateRange[0], dateRange[1]);

                    TeamPerformanceDto dto = new TeamPerformanceDto();
                    dto.setTeamId(team.getId());
                    dto.setTeamName(team.getName());

                    // Lead fields
                    dto.setTeamLeadUserId(teamLead != null ? teamLead.getUserId() : null);
                    dto.setTeamLeadName(teamLead != null ? (teamLead.getFirstName() + " " + teamLead.getLastName()) : null);
                    dto.setEmail(teamLead != null ? teamLead.getEmail() : null);

                    // Number of members (lead + members in the team)
                    dto.setNumberOfTeamMembers(participants.size());

                    // Total clients = all participants’ clients
                    dto.setTotalClientsAdded(teamClients.size());

                    // Active team target
                    int teamTarget = teamTargetRepository
                            .findTopByTeamIdAndDueDateGreaterThanEqualOrderByDueDateAsc(team.getId(), LocalDate.now())
                            .map(TeamTargetEntity::getTargetValue)
                            .orElse(0);
                    dto.setTeamTarget(teamTarget);

                    // Setting Progress
                    int added = teamClients.size();
                    double progress = (teamTarget > 0) ? Math.ceil((added * 100.0) / teamTarget) : 0.0;
                    dto.setProgressPercentage(progress);
                    dto.setProgressFraction(added + "/" + teamTarget);

                    // Team-level client status breakdown
                    dto.setClientStatus(
                            teamClients.stream()
                                    .collect(Collectors.groupingBy(
                                            c -> c.getClientStatus().getDisplayName(),
                                            Collectors.summingInt(c -> 1)
                                    ))
                    );

                    // Per-participant stats (lead + members)
                    dto.setTeamMembers(
                            participants.stream()
                                    .map(p -> teamMemberStats(p, dateRange[0], dateRange[1]))
                                    .collect(Collectors.toList())
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Team member stats
    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
        List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(member, start, end);

        // Latest target for this user
        UserTargetEntity latestTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(member);
        int target = (latestTarget != null) ? latestTarget.getTargetValue() : 0;

        int submitted = memberClients.size();
        double progressPercentage = (target > 0) ? Math.ceil((submitted * 100.0) / target) : 0.0;

        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setEmail(member.getEmail());
        dto.setTeamName(member.getTeam() != null ? member.getTeam().getName() : null);
        dto.setTeamLeadName(member.getTeam() != null && member.getTeam().getTeamLead() != null
                ? member.getTeam().getTeamLead().getFirstName() + " " + member.getTeam().getTeamLead().getLastName()
                : null);

        dto.setTotalClientsSubmitted(submitted);
        dto.setTarget(target);
        dto.setProgressPercentage(progressPercentage);
        dto.setProgressFraction(submitted + "/" + target);

        dto.setClientStatus(
                memberClients.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getClientStatus().getDisplayName(),
                                Collectors.summingInt(c -> 1)
                        ))
        );

        return dto;
    }

    // Date range method
    private Date[] calculateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // using last 5 days as default if no dates are provided
        if (startDate == null && endDate == null) {
            endDate = today;
            startDate = today.minusDays(4);
        }
        //  If only startDate is provided
        else if (startDate != null && endDate == null) {
            endDate = today;
        }
        //  If only endDate is provided
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


    /**
     * @return
     */
    @Override
    public List<UserDto> getUnassignedMembers() {
        return List.of();
    }

}

