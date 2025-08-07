package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.entities.UserTargetEntity;
import com.leadstracker.leadstracker.repositories.TeamTargetRepository;
import com.leadstracker.leadstracker.repositories.TeamsRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.repositories.UserTargetRepository;
import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.MyTargetResponse;
import com.leadstracker.leadstracker.response.TeamTargetOverviewDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;
import com.leadstracker.leadstracker.response.UserTargetResponseDto;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeamTargetServiceImpl implements TeamTargetService {

    @Autowired
    TeamTargetRepository teamTargetRepository;

    @Autowired
    TeamsRepository teamsRepository;

    @Autowired
    UserService userService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    AmazonSES amazonSES;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserTargetRepository userTargetRepository;


    @Override
    public TeamTargetResponseDto assignTargetToTeam(TeamTargetRequestDto dto) {
        TeamsEntity team = teamsRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (dto.getTargetValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must be greater than zero.");
        }

        // Validate due date
        if (dto.getDueDate() == null || !dto.getDueDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be a future date.");
        }

        // Checking for active target
        boolean hasUnexpiredTarget = !teamTargetRepository
                .findByTeamIdAndDueDateGreaterThanEqual(team.getId(), LocalDate.now())
                .isEmpty();

        if (hasUnexpiredTarget) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active target already exists for this team.");
        }

        UserEntity teamLead = team.getTeamLead();
        String teamLeadFullName = teamLead != null
                ? teamLead.getFirstName() + " " + teamLead.getLastName()
                : null;

        // Create new target entity
        TeamTargetEntity newTarget = new TeamTargetEntity();
        newTarget.setTeam(team);
        newTarget.setTargetValue(dto.getTargetValue());
        newTarget.setDueDate(dto.getDueDate());

        TeamTargetEntity savedTarget = teamTargetRepository.save(newTarget);

        return new TeamTargetResponseDto(
                savedTarget.getId(),
                savedTarget.getTeam().getName(),
                savedTarget.getTargetValue(),
                savedTarget.getDueDate()
        );
    }

    @Override
    public List<TeamTargetResponseDto> getAllTargets() {
        List<TeamTargetEntity> targetList = teamTargetRepository.findAll();

        List<TeamTargetResponseDto> dtoList = new ArrayList<>();
        for (TeamTargetEntity target : targetList) {
            dtoList.add(mapToResponseDto(target));
        }

        return dtoList;
    }

    private TeamTargetResponseDto mapToResponseDto(TeamTargetEntity entity) {
        return new TeamTargetResponseDto(
                entity.getId(),
                entity.getTeam().getName(),
                entity.getTargetValue(),
                entity.getDueDate()
        );
    }

    @Override
    public TeamTargetOverviewDto getTeamTargetOverview(String teamLeadEmail) {
        // 1. Get the Team Lead
        UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
        if (teamLead == null) {
            throw new RuntimeException("Team Lead not found");
        }
        // 2. Check if they have a team
        TeamsEntity team = teamLead.getTeam();
        if (team == null) {
            throw new RuntimeException("You have not been assigned to any team.");
        }

        // 3. Get the team target
        TeamTargetEntity teamTarget = teamTargetRepository.findByTeamId(team.getId())
                .orElseThrow(() -> new RuntimeException("No target has been assigned to your team yet."));

        // 4. Get all user targets under this team target
        List<UserTargetEntity> userTargets = userTargetRepository.findByTeamTargetId(teamTarget.getId());

        // 5. Map to response DTO
        return mapToTeamTargetOverviewDto(teamTarget, userTargets);
    }

    @Override
    public void assignTargetToTeamMembers(Long teamTargetId, Map<Long, Integer> memberTargets, String teamLeadEmail) {
        // Validation
        UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
        if (teamLead == null) {
            throw new RuntimeException("Team Lead not found");
        }

        TeamsEntity team = teamLead.getTeam();
        if (team == null) {
            throw new RuntimeException("You have not been assigned to any team.");
        }
        TeamTargetEntity teamTarget = teamTargetRepository.findById(teamTargetId)
                .orElseThrow(() -> new RuntimeException("Team Target not found."));

        // Validation: Check ownership
        if (!teamTarget.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("You are not authorized to distribute this target.");
        }

        // Validate total target
        int totalDistributed = memberTargets.values().stream().mapToInt(Integer::intValue).sum();
        if (totalDistributed > teamTarget.getTargetValue()) {
            throw new RuntimeException("Total distributed target exceeds the team target value.");
        }

        // Save assignments
        for (Map.Entry<Long, Integer> entry : memberTargets.entrySet()) {
            Long memberId = entry.getKey();
            Integer value = entry.getValue();

            UserEntity member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Team Member not found"));

            if (!Objects.equals(member.getTeamLead().getId(), teamLead.getId())) {
                throw new RuntimeException("User is not your team member.");
            }

            UserTargetEntity userTarget = new UserTargetEntity();
            userTarget.setTeamTarget(teamTarget);
            userTarget.setUser(member);
            userTarget.setTargetValue(value);
            userTarget.setAssignedDate(LocalDate.now());
            userTarget.setDueDate(teamTarget.getDueDate());
            userTargetRepository.save(userTarget);
        }
    }


            @Override
            public List<UserTargetResponseDto> getTeamMemberTargets(Long teamTargetId, String teamLeadEmail) {
                UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
                if (teamLead == null) {
                    throw new RuntimeException("Team Lead not found");
                }

                TeamTargetEntity teamTarget = teamTargetRepository.findById(teamTargetId)
                        .orElseThrow(() -> new RuntimeException("Team Target not found."));

                // Ensure the teamLead is authorized
                if (!teamLead.getTeam().getId().equals(teamTarget.getTeam().getId())) {
                    throw new RuntimeException("Unauthorized access to team target.");
                }

                List<UserTargetEntity> userTargets = userTargetRepository.findByTeamTargetId(teamTargetId);

                return userTargets.stream().map(target -> {
                    UserTargetResponseDto dto = new UserTargetResponseDto();
                    dto.setUserId(target.getUser().getId());
                    dto.setFullName(target.getUser().getFirstName() + " " + target.getUser().getLastName());
                    dto.setAssignedTargetValue(target.getTargetValue());
                    dto.setDueDate(target.getDueDate());
                    dto.setDateAssigned(target.getAssignedDate());
                    return dto;
                }).collect(Collectors.toList());
            }



            private TeamTargetOverviewDto mapToTeamTargetOverviewDto (TeamTargetEntity
                teamTarget, List < UserTargetEntity > userTargets){
                    TeamTargetOverviewDto dto = new TeamTargetOverviewDto();

                    // Set team-level target section
                    dto.setTotalTargetValue(teamTarget.getTargetValue());
                    dto.setTotalTargetValue(teamTarget.getTargetValue());
                    dto.setDueDate(teamTarget.getDueDate());
                    dto.setDateAssigned(teamTarget.getAssignedDate());

                    // Map member distributions
                    List<UserTargetResponseDto> memberDtos = userTargets.stream().map(ut -> {
                        UserTargetResponseDto memberDto = new UserTargetResponseDto();
                        memberDto.setUserId(memberDto.getUserId());
                        memberDto.setFullName(memberDto.getFullName());
                        memberDto.setAssignedTargetValue(memberDto.getAssignedTargetValue());
                        memberDto.setDateAssigned(memberDto.getDateAssigned());
                        memberDto.setDueDate(memberDto.getDueDate());
                        memberDto.setProgressAchieved(memberDto.getProgressAchieved());
                        return memberDto;

                    }).toList();

                    dto.setMemberDistributions(memberDtos);

                    return dto;
                }

    @Override
    public MyTargetResponse getMyTarget(String teamMemberEmail) {
        //  Get the logged-in Team Member
        UserEntity teamMember = userRepository.findByEmail(teamMemberEmail);
        if (teamMember == null) {
            throw new RuntimeException("Team Member not found.");
        }

        // Get the latest assigned target (you can adjust this if multiple are allowed)
        UserTargetEntity userTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(teamMember);
        if (userTarget == null) {
            throw new RuntimeException("No target has been assigned to you yet.");
        }

        //Calculate the progress made (sum of client values)
        int assignedValue = userTarget.getTargetValue();
        int currentProgress = userTarget.getProgress(); // assumes this is updated periodically
        int progressRemaining = Math.max(assignedValue - currentProgress, 0);


        //Build response
        MyTargetResponse response = new MyTargetResponse();
        response.setTargetValue(userTarget.getTargetValue());
        response.setDueDate(userTarget.getDueDate());
        response.setAssignedDate(userTarget.getAssignedDate());
        response.setProgressRemaining(progressRemaining);

        return response;
    }

}
