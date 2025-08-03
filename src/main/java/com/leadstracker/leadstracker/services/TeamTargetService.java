package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;

import java.util.List;

public interface TeamTargetService {

    TeamTargetResponseDto assignTargetToTeam(TeamTargetRequestDto dto);
    List<TeamTargetResponseDto> getAllTargets();
}
