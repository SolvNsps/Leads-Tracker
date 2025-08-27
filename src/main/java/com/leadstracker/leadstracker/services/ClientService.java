package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.response.Statuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface ClientService {
    ClientDto createClient(ClientDto clientDto);

    TeamPerformanceDto getTeamPerformance(String userId, LocalDate startDate, LocalDate endDate, String name, String team);

    TeamMemberPerformanceDto getMemberPerformance(String memberId, LocalDate startDate, LocalDate endDate);

    void deleteClient(String userId);

    ClientDto updateClient(String id, ClientDto clientDto);

     Page<ClientDto> getAllClients(int limit, int page, String name, Statuses status, String team);

    ClientDto getClientByClientId(String clientId);

    List<ClientDto> getClientsUnderUser(String userId, int page, int limit);

    Page<ClientDto> getOverdueClients(int page, int limit, LocalDate startDate, LocalDate endDate, String name);

    List<ClientDto> getAllClientsUnderUser(String userId);

    long countClientsUnderUser(String userId);

    List<ClientDto> getClientsByTeamMember(String userId, int page, int limit);

    long countClientsByTeamMember(String userId);

    List<ClientDto> getAllClientsByTeamMember(String userId);

    OverallSystemDto getClientStats(LocalDate fromDate, LocalDate toDate);

    List<ClientSearchDto> searchClients(String name, Statuses status, LocalDate date);

    Page<ClientDto> getClients(String email, Integer page, Integer size);

    PaginatedResponse<ClientRest> getOverdueClientsForUserRole(String loggedInUserId, String role, String userId, Pageable pageable, String name, Statuses status, LocalDate fromDate, LocalDate toDate);

    PaginatedResponse<ClientRest> getMyClientsForUserRole(String loggedInUserId, String role, String userId, Pageable pageable, String name, Statuses status, LocalDate fromDate, LocalDate toDate);

   Object getClientStatsForLoggedInUser(LocalDate fromDate, LocalDate toDate);

    void deactivateClient(String clientId);
}
