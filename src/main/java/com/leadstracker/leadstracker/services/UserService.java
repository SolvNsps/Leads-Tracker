package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.TeamDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface UserService extends UserDetailsService {

    UserDto getUserByUserId(String userId);

    UserDto getUser(String userName);

    UserDto updateUser(String userId, UserDto user);

    List<UserDto> getAllUsers(int page, int limit);

    boolean verifyEmailToken(String token);

    String initiatePasswordReset(String email);

    void resetPassword(String token, String newPassword, String confirmNewPassword);

    boolean validateOtp(String email, String otp);

    void saveOtp(String email, String otp, Date expiryTime);

    void deleteUser(String id);

    Map<String, Object> resendOtp(String email);

    UserDto createUser(UserDto userDto);

    List<UserDto> getMembersUnderLead(String id, String name);

    UserDto getMemberUnderLead(String userId, String memberId);

    UserDto getUserByEmail(String loggedInEmail);

    Page<UserDto> getAllTeamMembers(String name, String team, int page, int limit);

    Page<UserDto> getAllTeamLeads(String name, String team, int page, int limit);

    TeamDto createTeam(TeamDto teamDto);

    Page<UserDto> getTeamMembersData(String userId, int page, int limit);

    List<UserDto> searchUsers(String keyword);

    TeamDto getTeamById(String teamId);

    TeamDto updateTeam(String teamId, TeamDto teamDto);

    void deactivateTeam(String teamId);

    List<TeamDto> searchTeamByName(String name);

    void reactivateTeam(String teamId, String newTeamLeadId);

    TeamDto getTeamWithMembers(String teamId, LocalDate startDate, LocalDate endDate, int page, int limit);
}
