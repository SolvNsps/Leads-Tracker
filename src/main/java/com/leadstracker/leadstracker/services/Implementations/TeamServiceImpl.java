package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.TeamsRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    TeamsRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientRepository clientRepository;
    /**
     * @param search
     * @param duration
     * @return
     */
    @Override
    public List<TeamPerformanceDto> getTeamsOverview(String search, String duration) {
        List<TeamsEntity> teams = (search == null || search.isBlank())
                ? teamRepository.findAllByActiveTrue()
                : teamRepository.findByNameContainingIgnoreCaseAndActiveTrue(search);

        //Calculating date range
        Date[] dateRange = calculateDateRange(duration);

        return teams.stream()
                .map(team -> {
                    UserEntity teamLead = team.getTeamLead();

                    List<UserEntity> teamMembers = userRepository.findByTeamLead(teamLead);

                    List<ClientEntity> teamClients = clientRepository.findByCreatedByInAndCreatedDateBetween(
                            teamMembers, dateRange[0], dateRange[1]);

                    TeamPerformanceDto dto = new TeamPerformanceDto();
                    dto.setTeamName(team.getName());
                    dto.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
                    dto.setNumberOfTeamMembers(teamMembers.size());
                    dto.setTotalClientsAdded(teamClients.size());

                    // Team target logic placeholder
                    int target = 100; // Example target
                    dto.setTeamTarget(target);
                    dto.setProgressPercentage((double) teamClients.size() / target * 100);

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

        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setTotalClientsSubmitted(memberClients.size());

        // Member target logic placeholder (set your own logic)
        int target = 25;
        dto.setTarget(target);
        dto.setProgressPercentage((double) memberClients.size() / target * 100);

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

