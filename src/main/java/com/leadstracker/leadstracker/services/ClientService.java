package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamMemberPerformanceDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public interface ClientService extends UserDetailsService {
    ClientDto createClient(ClientDto clientDto);

    TeamPerformanceDto getTeamPerformance(String userId, String duration);

    TeamMemberPerformanceDto getMemberPerformance(String memberId, String duration);

    void deleteClient(String userId);
}
