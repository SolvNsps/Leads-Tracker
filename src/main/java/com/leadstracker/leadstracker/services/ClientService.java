package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ClientService {
    ClientDto createClient(ClientDto clientDto);

    TeamPerformanceDto getTeamPerformance(String userId, String duration);

    TeamMemberPerformanceDto getMemberPerformance(String memberId, String duration);

    void deleteClient(String userId);

    ClientDto updateClient(String id, ClientDto clientDto);

     Page<ClientDto> getAllClients(int limit, int page);

    ClientDto getClientByClientId(String clientId);

    List<ClientDto> getClientsUnderUser(String userId, int page, int limit);

    Page<ClientDto> getOverdueClients(int page, int limit);

    List<ClientDto> getAllClientsUnderUser(String userId);

    long countClientsUnderUser(String userId);

    List<ClientDto> getClientsByTeamMember(String userId, int page, int limit);

    long countClientsByTeamMember(String userId);

    List<ClientDto> getAllClientsByTeamMember(String userId);
}
