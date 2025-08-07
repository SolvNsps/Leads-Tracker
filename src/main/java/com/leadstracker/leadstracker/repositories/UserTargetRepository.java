package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.entities.UserTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTargetRepository extends JpaRepository<UserTargetEntity, Long> {

    // Find all user targets under a specific team target
    List<UserTargetEntity> findByTeamTargetId(Long teamTargetId);

    // Find all user targets for a specific user
    List<UserTargetEntity> findByUser(UserEntity user);

    boolean existsByUserAndTeamTarget(UserEntity user, TeamTargetEntity teamTarget);

    UserTargetEntity findTopByUserOrderByAssignedDateDesc(UserEntity user);
}
