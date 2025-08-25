package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.entities.UserTargetEntity;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.request.TargetDistributionRequest;
import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.MyTargetResponse;
import com.leadstracker.leadstracker.response.TeamTargetOverviewDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;
import com.leadstracker.leadstracker.response.UserTargetResponseDto;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

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

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    ModelMapper mapper;


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
        //  Getting the Team Lead
        UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
        if (teamLead == null) {
            throw new RuntimeException("Team Lead not found");
        }
        // Checking if they have a team
        TeamsEntity team = teamLead.getTeam();
        if (team == null) {
            throw new RuntimeException("You have not been assigned to any team.");
        }

        // Getting the team target
        TeamTargetEntity teamTarget = teamTargetRepository.findByTeamId(team.getId())
                .orElseThrow(() -> new RuntimeException("No target has been assigned to your team yet."));

        //Get all user targets under this team target
        List<UserTargetEntity> userTargets = userTargetRepository.findByTeamTargetId(teamTarget.getId());

        // Map to response DTO
        return mapToTeamTargetOverviewDto(teamTarget, userTargets);
    }


    @Override
    public void assignTargetToTeamMembers(Long teamTargetId, Map<String, Integer> memberTargets, String teamLeadEmail) {
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

        // Save or update assignments
        for (Map.Entry<String, Integer> entry : memberTargets.entrySet()) {
            String memberId = entry.getKey();
            Integer value = entry.getValue();

            UserEntity member = userRepository.findByUserId(memberId);

            if (!Objects.equals(member.getTeamLead().getId(), teamLead.getId())) {
                throw new RuntimeException("User is not your team member.");
            }

            UserTargetEntity userTarget = userTargetRepository
                    .findByTeamTarget_IdAndUser_UserId(teamTargetId, memberId)
                    .orElse(new UserTargetEntity());

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
                    dto.setUserId(target.getUser().getUserId());
                    dto.setFullName(target.getUser().getFirstName() + " " + target.getUser().getLastName());
                    dto.setAssignedTargetValue(target.getTargetValue());
                    dto.setDueDate(target.getDueDate());
                    dto.setDateAssigned(target.getAssignedDate());
                    return dto;
                }).collect(Collectors.toList());
            }


    private TeamTargetOverviewDto mapToTeamTargetOverviewDto(TeamTargetEntity teamTarget, List<UserTargetEntity> userTargets) {
        TeamTargetOverviewDto dto = new TeamTargetOverviewDto();
        dto.setTotalTargetValue(teamTarget.getTargetValue());
        dto.setDueDate(teamTarget.getDueDate());
        dto.setDateAssigned(teamTarget.getAssignedDate());

        List<UserTargetResponseDto> memberDistributions = userTargets.stream().map(userTarget -> {
            UserTargetResponseDto memberDto = new UserTargetResponseDto();

            UserEntity member = userTarget.getUser();

            if (member != null) {
                memberDto.setUserId(member.getUserId());
                memberDto.setFullName(member.getFirstName() + " " + member.getLastName());
            }

            // Assigned target value for this member
            memberDto.setAssignedTargetValue(userTarget.getTargetValue());
            memberDto.setDueDate(userTarget.getDueDate());
            memberDto.setDateAssigned(userTarget.getAssignedDate());

            // Calculate progress: total clients submitted / target
            int totalClients = Math.toIntExact(clientRepository.countByCreatedBy(member));
            memberDto.setProgressAchieved(totalClients + "/" + userTarget.getTargetValue());

            return memberDto;
        }).collect(Collectors.toList());

        dto.setMemberDistributions(memberDistributions);

        // Optional: Calculate overall progressPercentage for the team
//        int totalClientsAll = memberDistributions.stream()
//                .mapToInt(md -> {
//                    int fraction = md.getProgressAchieved();
//                    return Integer.parseInt(fraction[1]);
//                }).sum();
//
//        dto.setProgressPercentage(teamTarget.getTargetValue() > 0
//                ? (int) Math.ceil((totalClientsAll * 100.0) / teamTarget.getTargetValue())
//                : 0);

        return dto;
    }


    @Override
    public MyTargetResponse getMyTarget(String teamMemberEmail) {
        //  Get the logged-in Team Member
        UserEntity teamMember = userRepository.findByEmail(teamMemberEmail);
        if (teamMember == null) {
            throw new RuntimeException("Team Member not found.");
        }

        // Getting the latest assigned target
        UserTargetEntity userTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(teamMember);
        if (userTarget == null) {
            throw new RuntimeException("No target has been assigned to you yet.");
        }

        //Calculating the progress made
        int assignedValue = userTarget.getTargetValue();
        int currentProgress = userTarget.getProgress(); // assumes this is updated periodically
        int progressRemaining = Math.max(assignedValue - currentProgress, 0);
        int progressPercentage = (int) (double) ((currentProgress / assignedValue) * 100);

        //Building response
        MyTargetResponse response = new MyTargetResponse();
        response.setTotalTargetValue(userTarget.getTargetValue());
        response.setDueDate(userTarget.getDueDate());
        response.setAssignedDate(userTarget.getAssignedDate());
        response.setProgressRemaining(progressRemaining);
        response.setProgressPercentage(progressPercentage);
        response.setProgressValue(currentProgress);


        return response;
    }

    /**
     * @param email
     * @return
     */
    @Override
    public TeamTargetResponseDto getMyTeamTarget(String email) {
        // Fetch the team lead
        UserEntity teamLead = userRepository.findByEmail(email);

        // Ensure the user is indeed a team lead
        if (!teamLead.getRole().getName().equals("ROLE_TEAM_LEAD")) {
            throw new RuntimeException("Access denied: Not a team lead");
        }

        // Fetch the team assigned to this lead
        TeamsEntity team = teamLead.getTeam();
        if (team == null) {
            throw new RuntimeException("No team assigned to this lead");
        }

        // Fetch the target for the team
        Optional<TeamTargetEntity> targetOpt = teamTargetRepository.findByTeam(team);
        if (targetOpt.isEmpty()) {
            throw new RuntimeException("No target set for this team");
        }

        TeamTargetEntity target = targetOpt.get();

        // Populate DTO
        TeamTargetResponseDto dto = new TeamTargetResponseDto();
        dto.setId(target.getId());
        dto.setTeamName(team.getName());
        dto.setTeamLeadFullName(teamLead.getFirstName() + " " + teamLead.getLastName());
        dto.setTargetValue(target.getTargetValue());
        dto.setDueDate(target.getDueDate());

        return dto;

    }

    /**
     * @param teamTargetId
     * @param memberId
     * @param newTargetValue
     * @param teamLeadEmail
     */
    @Override
    public void editMemberTarget(Long teamTargetId, String memberId, Integer newTargetValue, String teamLeadEmail) {
        // Validating team lead
        UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
        if (teamLead == null) {
            throw new RuntimeException("Team Lead not found");
        }

        TeamsEntity team = teamLead.getTeam();
        if (team == null) {
            throw new RuntimeException("You have not been assigned to any team.");
        }

        // Validating team target
        TeamTargetEntity teamTarget = teamTargetRepository.findById(teamTargetId)
                .orElseThrow(() -> new RuntimeException("Team Target not found."));

        if (!teamTarget.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("You are not authorized to edit this target.");
        }

        // Validating that the member belongs to the team lead
        UserEntity member = userRepository.findByUserId(memberId);
        if (member == null) {
            throw new RuntimeException("Team member not found.");
        }
        if (!Objects.equals(member.getTeamLead().getId(), teamLead.getId())) {
            throw new RuntimeException("User is not your team member.");
        }

        // Finding the existing user target
        UserTargetEntity userTarget = userTargetRepository
                .findByTeamTarget_IdAndUser_UserId(teamTargetId, memberId)
                .orElseThrow(() -> new RuntimeException("Target for this member not found."));

        //Validating total distribution after change
        int currentTotal = userTargetRepository.findByTeamTarget_Id(teamTargetId).stream()
                .filter(t -> !t.getUser().getUserId().equals(memberId))
                .mapToInt(UserTargetEntity::getTargetValue)
                .sum();
        if (currentTotal + newTargetValue > teamTarget.getTargetValue()) {
            throw new RuntimeException("Total distributed target exceeds the team target value.");
        }

        // Updating and save
        userTarget.setTargetValue(newTargetValue);
        userTargetRepository.save(userTarget);
    }

}
