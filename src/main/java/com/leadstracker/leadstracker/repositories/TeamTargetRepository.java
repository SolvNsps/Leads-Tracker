package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeamTargetRepository extends JpaRepository<TeamTargetEntity, Long> {

    // Get all targets for a team where due date is in the future or today
    List<TeamTargetEntity> findByTeamIdAndDueDateGreaterThanEqual(Long teamId, LocalDate date);

    // Get all targets for a team
    Optional<TeamTargetEntity> findByTeamId(Long teamId);
}