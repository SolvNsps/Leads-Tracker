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
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
     * @param search
     * @param duration
     * @return
     */
    @Override
    public List<TeamPerformanceDto> getTeamsOverview(String search, String duration) {
        List<TeamsEntity> teams = teamRepository.findAllByActiveTrue();

        if (search != null && !search.isBlank()) {
            teams = teamRepository.findByNameContainingIgnoreCaseAndActiveTrue(search);
        }

        //Calculating date range
        Date[] dateRange = calculateDateRange(duration);

        return teams.stream()
                .map(team -> {
                    UserEntity teamLead = team.getTeamLead();

                    List<UserEntity> teamMembers = userRepository.findByTeam(team);
                    //including team lead
                    if (teamLead != null && !teamMembers.contains(teamLead)) {
                        teamMembers.add(teamLead);
                    }

                    List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(
                            teamMembers, dateRange[0], dateRange[1]);

                    TeamPerformanceDto dto = new TeamPerformanceDto();
                    dto.setTeamId(team.getId());
                    dto.setTeamName(team.getName());
                    dto.setTeamLeadName(
                            teamLead != null
                                    ? teamLead.getFirstName() + " " + teamLead.getLastName()
                                    : null
                    );
                    dto.setNumberOfTeamMembers(teamMembers.size());
                    dto.setTotalClientsAdded(teamClients.size());

                    // Fetch active team target from DB
                    Optional<TeamTargetEntity> activeTargetOpt = teamTargetRepository
                            .findTopByTeamIdAndDueDateGreaterThanEqualOrderByDueDateAsc(team.getId(), LocalDate.now());

                    int teamTarget = 0;
                    if (activeTargetOpt.isPresent()) {
                        teamTarget = activeTargetOpt.get().getTargetValue();
                    }

                    dto.setTeamTarget(teamTarget);

                    // Calculate progress
                    int numberOfClientsAdded = teamClients.size();
                    double progress = Math.ceil((teamTarget > 0) ? ((double) numberOfClientsAdded / teamTarget) * 100 : 0);
                    dto.setProgressPercentage(progress);
                    dto.setProgressFraction(numberOfClientsAdded + "/" + teamTarget);

                    // Client status breakdown
                    dto.setClientStatus(
                            teamClients.stream()
                                    .collect(Collectors.groupingBy(
                                            ClientEntity::getClientStatus,
                                            Collectors.summingInt(c -> 1)
                                    ))
                    );

                    // Team member stats
                    dto.setTeamMembers(
                            teamMembers.stream()
                                    .map(member -> teamMemberStats(member, dateRange[0], dateRange[1]))
                                    .collect(Collectors.toList())
                    );

                    return dto;
                }).collect(Collectors.toList());
    }

    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
        List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(
                member, start, end);

        // Fetching the latest target assigned by team lead
        UserTargetEntity latestTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(member);
        int target = 0;
        if (latestTarget != null) {
            target = latestTarget.getTargetValue();
        }


        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setTotalClientsSubmitted(memberClients.size());
        dto.setTarget(target);
        //  Calculating progress
        double progressPercentage = 0;
        if (target > 0) {
            progressPercentage = Math.ceil((memberClients.size() * 100.0) / target);
        }

        dto.setProgressPercentage(progressPercentage);
        dto.setProgressFraction(memberClients.size() + "/" + target);

        dto.setClientStatus(
                memberClients.stream()
                        .collect(Collectors.groupingBy(
                                ClientEntity::getClientStatus,
                                Collectors.summingInt(c -> 1)
                        ))
        );

        return dto;
    }

    private Date[] calculateDateRange(String duration) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = duration.equalsIgnoreCase("month") ?
                endDate.minusMonths(1) : endDate.minusWeeks(1);

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

