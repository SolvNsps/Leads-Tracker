package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
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


    /**
     * @param clientDto
     * @return
     */
    @Override
    public ClientDto createClient(ClientDto clientDto) {
        String creatorUserId = clientDto.getCreatedByUserId();

        // Fetching the user
        UserEntity creatorEntity = userRepository.findByUserId(creatorUserId);

        String creatorRole = creatorEntity.getRole().getName();

        if (!creatorRole.equals("ROLE_TEAM_LEAD") && !creatorRole.equals("ROLE_TEAM_MEMBER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to create client.");
        }

        ClientEntity clientEntity = modelMapper.map(clientDto, ClientEntity.class);

        // Setting the creator
        clientEntity.setCreatedBy(creatorEntity);
        clientEntity.setClientId(utils.generateUserId(30));
        clientEntity.setClientStatus(Statuses.PENDING);

        // Setting the team lead
        if (creatorRole.equals("ROLE_TEAM_LEAD")) {
            clientEntity.setTeamLead(creatorEntity); // Team Lead created the client
        } else {
            if (creatorEntity.getTeamLead() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team Member must be assigned to a Team Lead.");
            }

            clientEntity.setTeamLead(creatorEntity.getTeamLead()); // Team member created the client
        }

        ClientEntity saved = clientRepository.save(clientEntity);
        return modelMapper.map(saved, ClientDto.class);
    }

    /**
     * @param duration
     * @return
     */
        public TeamPerformanceDto getTeamPerformance(String userId, String duration) {
            //Getting team lead and members
            UserEntity teamLead = userRepository.findByUserId(userId);
            List<UserEntity> teamMembers = userRepository.findByTeamLead(userId);

            //Calculating date range
            Date[] dateRange = calculateDateRange(duration);

            //Fetching all the clients of a team
            List<ClientEntity> clients = clientRepository.findByCreatedByInAndCreatedDateBetween(
                    teamMembers, dateRange[0], dateRange[1]
            );

            //response
            TeamPerformanceDto response = new TeamPerformanceDto();
            response.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());
            response.setTotalClientsAdded(clients.size());
            response.setTeamTarget(response.getNumberOfClients());
            response.setProgressPercentage(((double) clients.size() / (response.getNumberOfClients())) * 100);
            response.setNumberOfTeamMembers(teamMembers.size());

            //Building the status distribution using the enum
            response.setClientStatus(
                    clients.stream()
                            .collect(Collectors.groupingBy(
                                            ClientEntity::getClientStatus,
                                            Collectors.summingInt(c -> 1)
                                    )
                            ));

            // Building team member stats
            response.setTeamMembers(
                    teamMembers.stream()
                            .map(member -> teamMemberStats(member, dateRange[0], dateRange[1]))
                            .collect(Collectors.toList())
            );

            return response;
        }

    private Date[] calculateDateRange(String duration) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = duration.equals("month") ?
                endDate.minusMonths(1) : endDate.minusWeeks(1);

        return new Date[] {
                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        };
    }

    @Override
    public TeamMemberPerformanceDto getMemberPerformance(String memberId, String duration) {
        UserEntity member = userRepository.findByUserId(memberId);
        Date[] dateRange = calculateDateRange(duration);
        return teamMemberStats(member, dateRange[0], dateRange[1]);

    }

    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
            List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(
                    member, start, end
            );

            TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
            dto.setMemberId(member.getUserId());
            dto.setMemberName(member.getFirstName() + " " + member.getLastName());
            dto.setTotalClientsSubmitted(memberClients.size());

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


    /**
     * @param email
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        System.out.println("user entity :"+ userEntity);

        if (userEntity == null) throw new UsernameNotFoundException(email);

        return new UserPrincipal(userEntity);
    }


    /**
     * @param userId
     */
    @Override
    public void deleteClient(String userId) {
        ClientEntity clientEntity = clientRepository.findByClientId(userId);
        clientRepository.delete(clientEntity);
    }

}
