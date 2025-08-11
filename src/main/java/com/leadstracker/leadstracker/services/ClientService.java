package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    OverallSystemDto getClientStats(String duration);

    List<ClientSearchDto> searchClients(String name, String status, LocalDate date);

    List<ClientDto> getMyClients(String email);

    Page<ClientDto> getClients(String email, Integer page, Integer size);

//    Page<ClientDto> getOverdueClientsByUser(String userId, int page, int limit);

    PaginatedResponse<ClientRest> getOverdueClientsForUserRole(String loggedInUserId, String role, String userId, Pageable pageable);
//
//    Page<ClientDto> getOverdueClientsUnderUser(String userId, String loggedInUserId, int page, int size);

//    Page<ClientDto> getOverdueClientsUnderUser(UserPrincipal authentication, int page, int size);
}
