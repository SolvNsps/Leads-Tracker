package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TeamService {
    List<TeamPerformanceDto> getTeamsOverview(String search, String duration);

    List<UserDto> getUnassignedMembers();

}
