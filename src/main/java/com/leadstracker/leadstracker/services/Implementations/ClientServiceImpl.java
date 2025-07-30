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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        clientEntity.setClientStatus(Statuses.fromString(clientDto.getClientStatus()));

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

        // Map back to DTO
        ClientDto result = modelMapper.map(saved, ClientDto.class);

//  Add team lead manually
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
        List<UserEntity> teamMembers = userRepository.findByTeamLead(teamLead);

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

        return new Date[]{
                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
//                Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
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
        oldClient.setGPSLocation(clientEntity.getGPSLocation());

        if (!isInternetAvailable()) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update client.");
        }

        clientEntity.setFirstName(clientDto.getFirstName());
        clientEntity.setLastName(clientDto.getLastName());
//        clientEntity.setPhoneNumber(clientDto.getPhoneNumber());
        clientEntity.setGPSLocation(clientDto.getGPSLocation());
        clientEntity.setClientStatus(Statuses.fromString(clientDto.getClientStatus()));
        clientEntity.setGPSLocation(clientDto.getGPSLocation());
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

        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<ClientEntity> clientPage = clientRepository.findAll(pageableRequest);

        return clientPage.map(clientEntity -> {
            ClientDto dto = modelMapper.map(clientEntity, ClientDto.class);

            if (clientEntity.getTeamLead() != null) {
                UserDto teamLeadDto = modelMapper.map(clientEntity.getTeamLead(), UserDto.class);
                dto.setAssignedTo(teamLeadDto);
            }

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

        Pageable pageable = PageRequest.of(page, limit);
        UserEntity userEntity = userRepository.findByUserId(userId);

        List<ClientEntity> clients;

        if (userEntity.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            // Getting clients created by this lead or their team members
            clients = clientRepository.findByTeamLead(userEntity, pageable);
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

            dto.setClientStatus(entity.getClientStatus().toString());
            returnList.add(dto);
        }

        return returnList;
    }

    /**
     * @return
     */
    @Override
    public Page<ClientDto> getOverdueClients(int page, int limit) {

        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<ClientEntity> allClients = clientRepository.findAll(pageableRequest);

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
                    EnumSet.of(Statuses.PENDING, Statuses.INTERESTED, Statuses.AWAITING_DOCUMENTATION)
                            .contains(client.getClientStatus())) {

                ClientDto dto = modelMapper.map(client, ClientDto.class);

                if (client.getCreatedBy() != null) {
                    dto.setCreatedBy(modelMapper.map(client.getCreatedBy(), UserDto.class));
                }

                if (client.getTeamLead() != null) {
                    dto.setAssignedTo(modelMapper.map(client.getTeamLead(), UserDto.class));
                }

                dto.setClientStatus(client.getClientStatus().toString());
                overdueClients.add(dto);
            }
        }

        return allClients.map(clientEntity -> {
            ClientDto dto = modelMapper.map(clientEntity, ClientDto.class);
            return dto;
        });
    }
}
