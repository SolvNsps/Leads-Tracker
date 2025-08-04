package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.TeamTargetRepository;
import com.leadstracker.leadstracker.repositories.TeamsRepository;
import com.leadstracker.leadstracker.request.TeamTargetRequestDto;
import com.leadstracker.leadstracker.response.TeamTargetResponseDto;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;

import java.util.List;
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

        // Checkin for active target
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

//        UserEntity teamLead = team.getTeamLead();
//        notificationService.createTeamTargetAssignedNotification(teamLead, savedTarget);

//        UserEntity teamLead = team.getTeamLead();
//        amazonSES.sendTargetAssignmentEmail(
//                teamLead.getEmail(),
//                teamLead.getFirstName(),
//                team.getName(),
//                savedTarget.getDueDate(),
//                savedTarget.getTargetValue()
//        );

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

}
