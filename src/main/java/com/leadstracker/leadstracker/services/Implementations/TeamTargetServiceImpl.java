package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.TeamTargetUpdateRequestDto;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.entities.UserTargetEntity;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.MyTargetResponse;
import com.leadstracker.leadstracker.response.TeamTargetOverviewDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;
import com.leadstracker.leadstracker.response.UserTargetResponseDto;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserService;
import jakarta.transaction.Transactional;
import org.apache.coyote.BadRequestException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;
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
    public TeamTargetResponseDto createTarget(TeamTargetRequestDto dto) {
        // Validate team exists
        TeamsEntity team = teamsRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        // Validate target value
        if (dto.getTargetValue() == null || dto.getTargetValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target value must be greater than zero.");
        }

        //  Validate due date
        if (dto.getDueDate() == null || !dto.getDueDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be in the future.");
        }

        // Validate start date
        if (dto.getStartDate() == null || dto.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be today or in the future.");
        }


        // Create new target (inactive by default)
        TeamTargetEntity newTarget = new TeamTargetEntity();
        newTarget.setTeam(team);
        newTarget.setTargetValue(dto.getTargetValue());
        newTarget.setDueDate(dto.getDueDate());
        newTarget.setAssignedDate(dto.getStartDate());
        newTarget.setActive(false); // mark as inactive

        TeamTargetEntity savedTarget = teamTargetRepository.save(newTarget);

        // response DTO
        String teamLeadFullName = (team.getTeamLead() != null)
                ? team.getTeamLead().getFirstName() + " " + team.getTeamLead().getLastName()
                : null;

        TeamTargetResponseDto response = new TeamTargetResponseDto();
        response.setId(savedTarget.getId());
        response.setTeamName(team.getName());
        response.setTeamLeadFullName(teamLeadFullName);
        response.setTargetValue(savedTarget.getTargetValue());
        response.setStartDate(savedTarget.getAssignedDate());
        response.setDueDate(savedTarget.getDueDate());

        return response;
    }


//    @Override
//    public List<TeamTargetResponseDto> getAllTargets() {
//        List<TeamTargetEntity> targetList = teamTargetRepository.findAll();
//
//        List<TeamTargetResponseDto> dtoList = new ArrayList<>();
//        for (TeamTargetEntity target : targetList) {
//            dtoList.add(mapToResponseDto(target));
//        }
//
//        return dtoList;
//    }

    @Override
    public List<TeamTargetResponseDto> getAllTargets() {
        List<TeamTargetEntity> targetList = teamTargetRepository.findAll();

        List<TeamTargetResponseDto> dtoList = targetList.stream()
                .map(this::mapToResponseDto)
                .sorted(Comparator.comparing(TeamTargetResponseDto::getTeamName, Comparator.nullsLast(String::compareTo)))
                .toList();

        return dtoList;
    }

    private TeamTargetResponseDto mapToResponseDto(TeamTargetEntity entity) {
        String teamName = entity.getTeam() != null ? entity.getTeam().getName() : "Unknown Team";
        String teamLeadFullName = "N/A";

        if (entity.getTeam() != null && entity.getTeam().getTeamLead() != null) {
            UserEntity lead = entity.getTeam().getTeamLead();
            teamLeadFullName = lead.getFirstName() + " " + lead.getLastName();
        }

        LocalDate startDate = entity.getAssignedDate();
        LocalDate dueDate = entity.getDueDate();

        // Calculate total clients added by this team
        int totalClients = 0;
        if (entity.getTeam() != null) {
            List<UserEntity> members = userRepository.findByTeam(entity.getTeam());
            totalClients = Math.toIntExact(clientRepository.countByCreatedByIn(members));
        }

        return new TeamTargetResponseDto(
                entity.getId(),
                teamName,
                teamLeadFullName,
                entity.getTargetValue(),
                startDate,
                dueDate,
                totalClients
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

        //Getting all user targets under this team target
        List<UserTargetEntity> userTargets = userTargetRepository.findByTeamTargetId(teamTarget.getId());

        // Map to response DTO
        return mapToTeamTargetOverviewDto(teamTarget, userTargets);
    }


//    @Override
//    public void assignTargetToTeamMembers(Long teamTargetId, Map<String, Integer> memberTargets, String teamLeadEmail) {
//        // Validation
//        UserEntity teamLead = userRepository.findByEmail(teamLeadEmail);
//        if (teamLead == null) {
//            throw new RuntimeException("Team Lead not found");
//        }
//
//        TeamsEntity team = teamLead.getTeam();
//        if (team == null) {
//            throw new RuntimeException("You have not been assigned to any team.");
//        }
//        TeamTargetEntity teamTarget = teamTargetRepository.findById(teamTargetId)
//                .orElseThrow(() -> new RuntimeException("Team Target not found."));
//
//        // Validation: Checking ownership
//        if (!teamTarget.getTeam().getId().equals(team.getId())) {
//            throw new RuntimeException("You are not authorized to distribute this target.");
//        }
//
//        // Validating total target
//        int totalDistributed = memberTargets.values().stream().mapToInt(Integer::intValue).sum();
//        if (totalDistributed > teamTarget.getTargetValue()) {
//            throw new RuntimeException("Total distributed target exceeds the team target value.");
//        }
//
//        for (Map.Entry<String, Integer> entry : memberTargets.entrySet()) {
//            String memberId = entry.getKey();
//            Integer value = entry.getValue();
//
//            UserEntity member = userRepository.findByUserId(memberId);
//
//            if (!Objects.equals(member.getTeamLead().getId(), teamLead.getId())) {
//                throw new RuntimeException("User is not your team member.");
//            }
//
//            UserTargetEntity userTarget = userTargetRepository
//                    .findByTeamTarget_IdAndUser_UserId(teamTargetId, memberId)
//                    .orElse(new UserTargetEntity());
//
//            userTarget.setTeamTarget(teamTarget);
//            userTarget.setUser(member);
//            userTarget.setTargetValue(value);
//            userTarget.setAssignedDate(LocalDate.now());
//            userTarget.setDueDate(teamTarget.getDueDate());
//
//            userTargetRepository.save(userTarget);
//        }
//    }

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

        if (!teamTarget.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("You are not authorized to distribute this target.");
        }

        // Validating total target
        int totalDistributed = memberTargets.values().stream().mapToInt(Integer::intValue).sum();
        if (totalDistributed > teamTarget.getTargetValue()) {
            throw new RuntimeException("Total distributed target exceeds the team target value.");
        }

        for (Map.Entry<String, Integer> entry : memberTargets.entrySet()) {
            String memberId = entry.getKey();
            Integer value = entry.getValue();

            UserEntity member = userRepository.findByUserId(memberId);
            if (member == null) {
                throw new RuntimeException("User not found: " + memberId);
            }

            // Special case: Team Lead assigning to himself
            if (Objects.equals(member.getId(), teamLead.getId())) {
                // allow directly (lead assigning to self)
            } else {
                if (member.getTeam() == null || !Objects.equals(member.getTeam().getId(), teamLead.getTeam().getId())) {
                    throw new RuntimeException("User " + member.getUserId() + " is not in your team.");
                }
            }


            // Save or update the target
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

        // Calculating total achieved across all members
        final int[] totalAchieved = {0};

        List<UserTargetResponseDto> memberDistributions = userTargets.stream().map(userTarget -> {
            UserTargetResponseDto memberDto = new UserTargetResponseDto();

            UserEntity member = userTarget.getUser();

            if (member != null) {
                memberDto.setUserId(member.getUserId());
                memberDto.setFullName(member.getFirstName() + " " + member.getLastName());
            }

            // Assigned target value for member
            memberDto.setAssignedTargetValue(userTarget.getTargetValue());
            memberDto.setDueDate(userTarget.getDueDate());
            memberDto.setDateAssigned(userTarget.getAssignedDate());

            // Calculating member progress: total clients submitted / target
            int memberClients = Math.toIntExact(clientRepository.countByCreatedBy(member));
            memberDto.setProgressAchieved(memberClients + "/" + userTarget.getTargetValue());
            memberDto.setProgressPercentage(Math.round(memberClients * 100 / userTarget.getTargetValue()));

            // Adding to total
            totalAchieved[0] = totalAchieved[0] + memberClients;

            return memberDto;
        }).collect(Collectors.toList());

        // Setting team progress
        dto.setProgressAchieve(totalAchieved[0] + "/" + teamTarget.getTargetValue());
        if (teamTarget.getTargetValue() > 0) {
            double percentage = ((double) totalAchieved[0] / teamTarget.getTargetValue()) * 100;
            dto.setProgressPercentage((int) Math.round(percentage));
        } else {
            dto.setProgressPercentage(0);
        }

        dto.setMemberDistributions(memberDistributions);

        return dto;
    }



    @Override
    public MyTargetResponse getMyTarget(String teamMemberEmail) {
        //  Getting the logged-in Team Member
        UserEntity teamMember = userRepository.findByEmail(teamMemberEmail);
        if (teamMember == null) {
            throw new RuntimeException("Team Member not found.");
        }

        // Getting the latest assigned target
        UserTargetEntity userTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(teamMember);
        if (userTarget == null) {
            throw new RuntimeException("No target has been assigned to you yet.");
        }

        // Calculating the progress made
        int assignedValue = userTarget.getTargetValue();
        int currentProgress = userTarget.getProgress();
        int progressRemaining = Math.max(assignedValue - currentProgress, 0);
        int progressPercentage = (int) (double) ((currentProgress / assignedValue) * 100);

        final int[] totalAchieved = {0};

        int memberClients = Math.toIntExact(clientRepository.countByCreatedBy(teamMember));

        //Building response
        MyTargetResponse response = new MyTargetResponse();
        response.setTotalTargetValue(userTarget.getTargetValue());
        response.setDueDate(userTarget.getDueDate());
        response.setAssignedDate(userTarget.getAssignedDate());
        response.setProgressRemaining(progressRemaining);
        response.setProgressValue(totalAchieved[0] + memberClients);
        response.setProgressPercentage(((totalAchieved[0] + memberClients)  * 100) / assignedValue);

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

    @Override
    @Transactional
    public TeamTargetResponseDto activateTarget(Long targetId) {
        // Find target to activate
        TeamTargetEntity target = teamTargetRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("No target found with ID: " + targetId));

        TeamsEntity team = target.getTeam();

        // Deactivate all other targets for this team
        List<TeamTargetEntity> teamTargets = teamTargetRepository.findByTeamIdAndDueDateGreaterThanEqual(
                team.getId(), LocalDate.now()
        );

        for (TeamTargetEntity t : teamTargets) {
            if (!t.getId().equals(targetId) && t.isActive()) {
                t.setActive(false);
                teamTargetRepository.save(t);
            }
        }

        //  Activate the selected target
        target.setActive(true);
        teamTargetRepository.save(target);

        // Build and return response
        TeamTargetResponseDto response = new TeamTargetResponseDto();
        response.setId(target.getId());
        response.setTeamName(team.getName());
        response.setTargetValue(target.getTargetValue());
        response.setDueDate(target.getDueDate());
        response.setStartDate(target.getAssignedDate());

        return response;
    }

    @Override
    public TeamTargetUpdateRequestDto updateTarget(Long targetId, TeamTargetUpdateRequestDto requestDto) {
        // Fetch the existing target
        TeamTargetEntity target = teamTargetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found with ID: " + targetId));

        // Update allowed fields
        if (requestDto.getTargetValue() != null && requestDto.getTargetValue() > 0) {
            target.setTargetValue(requestDto.getTargetValue());
        }
        if (requestDto.getDueDate() != null && requestDto.getDueDate().isAfter(LocalDate.now())) {
            target.setDueDate(requestDto.getDueDate());
        }

        // Save updated entity
        TeamTargetEntity updatedTarget = teamTargetRepository.save(target);

        // Map and return response
        TeamTargetUpdateRequestDto response = new TeamTargetUpdateRequestDto();
        response.setTargetValue(updatedTarget.getTargetValue());
        response.setDueDate(updatedTarget.getDueDate());
        response.setStartDate(updatedTarget.getAssignedDate());

        return response;
    }


    @Override
    public List<TeamTargetResponseDto> getTargetsByTeam(Long teamId) {
        TeamsEntity team = teamsRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamTargetEntity> targetList = teamTargetRepository.findByTeam_Id(team.getId());

        List<TeamTargetResponseDto> dtoList = targetList.stream()
                .map(entity -> mapToResponseDto(entity))
                .sorted(Comparator.comparing(TeamTargetResponseDto::getTeamName, Comparator.nullsLast(String::compareTo)))
                .toList();

        return dtoList;
    }

    /**
     * @return
     */
    @Override
    public List<TeamTargetResponseDto> getAllActiveTargets() {
        List<TeamTargetEntity> targetList = teamTargetRepository.findByActiveTrue();

        List<TeamTargetResponseDto> activeList = targetList.stream()
                .map(this::mapToResponseDto)
                .sorted(Comparator.comparing(TeamTargetResponseDto::getTeamName, Comparator.nullsLast(String::compareTo)))
                .toList();

        return activeList;
    }

}
