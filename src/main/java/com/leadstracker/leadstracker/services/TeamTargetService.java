package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.TeamTargetUpdateRequestDto;
import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.MyTargetResponse;
import com.leadstracker.leadstracker.response.TeamTargetOverviewDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;
import com.leadstracker.leadstracker.response.UserTargetResponseDto;

import java.util.List;
import java.util.Map;

public interface TeamTargetService {

    TeamTargetResponseDto createTarget(TeamTargetRequestDto dto);

    List<TeamTargetResponseDto> getAllTargets();

    TeamTargetOverviewDto getTeamTargetOverview(String teamLeadEmail);

    void assignTargetToTeamMembers(Long teamTargetId, Map<String, Integer> memberTargets, String teamLeadEmail);

    List<UserTargetResponseDto> getTeamMemberTargets(Long teamTargetId, String teamLeadEmail);

    MyTargetResponse getMyTarget(String teamMemberEmail);

    TeamTargetResponseDto getMyTeamTarget(String email);

    void editMemberTarget(Long teamTargetId, String memberId, Integer newTargetValue, String teamLeadEmail);

    TeamTargetResponseDto activateTarget(Long targetId);

    TeamTargetUpdateRequestDto updateTarget(Long targetId,TeamTargetUpdateRequestDto requestDto);

}

